package edu.clinic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Spring Security OAuth2 resource-server config, validating Keycloak JWTs.
 *
 * Adapted from fanvote-communication's SecurityConfig, including the custom
 * {@link #jwtAuthenticationConverter()} — see the note on that method for why a
 * plain {@code JwtGrantedAuthoritiesConverter} with a dotted claim name silently
 * produces no authorities at all for a nested Keycloak claim.
 */
@Configuration
// Turns on @PreAuthorize. Without it those annotations are silently inert — they
// compile, they read like protection, and they enforce nothing.
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${clinic.cors.allowed-origin}")
    private String allowedOrigin;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Clinic API")
                        .version("1.0")
                        .description("Dermatology clinic booking API"));
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        // swagger / actuator
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()

                        // Public browsing: the homepage and the service catalogue need to be
                        // readable by a visitor deciding whether to book, before they have an
                        // account. Service photos ride along the same rule.
                        .requestMatchers(HttpMethod.GET, "/api/services").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/services/images/**").permitAll()

                        // Available slots reveal only busy/free, never who holds a slot — see
                        // AppointmentController — so this is safe to leave public. Letting an
                        // anonymous visitor check availability before creating an account is
                        // the point: nobody signs in just to find out the clinic is full.
                        .requestMatchers(HttpMethod.GET, "/api/appointments/available-slots").permitAll()

                        // The admin screens are part of the portfolio demo, so any signed-in
                        // visitor can look around read-only — only the mutating verbs (create,
                        // edit, delete, change an appointment's status) require the
                        // clinic-admin role. Ordered before the narrower authenticated rules
                        // below so they don't shadow this by accident.
                        .requestMatchers(HttpMethod.GET, "/api/admin/**").authenticated()
                        .requestMatchers("/api/admin/**").hasRole("clinic-admin")

                        // Booking a slot and reading your own appointment history both need
                        // to know who is asking; the controllers take the identity from the
                        // token (the JWT "sub"), never from the request body or path.
                        .requestMatchers("/api/appointments/**").authenticated()
                        .requestMatchers("/api/account/**").authenticated()

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /**
     * Realm roles out of a Keycloak token.
     *
     * Copied from fanvote-communication on purpose: a {@code JwtGrantedAuthoritiesConverter}
     * configured with {@code setAuthoritiesClaimName("realm_access.roles")} looks like it
     * reads a nested claim and does not — that setter names a claim, and the lookup behind
     * it is a flat map get, so it resolves to null and produces an empty authority list for
     * every token. hasRole("clinic-admin") would refuse every admin, silently, without this.
     *
     * ROLE_ is prepended because hasRole("x") checks for authority "ROLE_x".
     */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            if (!(jwt.getClaim("realm_access") instanceof Map<?, ?> realmAccess)) {
                return List.of();
            }
            if (!(realmAccess.get("roles") instanceof Collection<?> roles)) {
                return List.of();
            }
            return roles.stream()
                    .map(String::valueOf)
                    .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
        });
        return jwtAuthenticationConverter;
    }

    @Configuration
    public class WebConfig implements WebMvcConfigurer {
        @Override
        public void addCorsMappings(CorsRegistry registry) {
            registry.addMapping("/**")
                    .allowedOrigins(allowedOrigin)
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    }
}
