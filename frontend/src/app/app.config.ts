import { provideHttpClient, withInterceptors, withXhr } from '@angular/common/http';
import { includeBearerTokenInterceptor } from 'keycloak-angular';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { routes } from './app.routes';
import { provideKeycloakAngular } from './keycloak.config';

export const appConfig: ApplicationConfig = {
  providers: [
    provideKeycloakAngular(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes, withInMemoryScrolling({ anchorScrolling: 'enabled' })),
    provideHttpClient(withXhr(), withInterceptors([includeBearerTokenInterceptor])),
  ]
};
