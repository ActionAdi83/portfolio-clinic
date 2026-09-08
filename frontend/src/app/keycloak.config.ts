import {
  provideKeycloak,
  createInterceptorCondition,
  IncludeBearerTokenCondition,
  INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG,
  withAutoRefreshToken,
  AutoRefreshTokenService,
  UserActivityService
} from 'keycloak-angular';
import Keycloak from 'keycloak-js';
import {
  EnvironmentInjector,
  Injectable,
  inject,
  provideAppInitializer,
  runInInjectionContext
} from '@angular/core';
import { environment } from '../environment/environment';

const apiUrlRegex = new RegExp(`${environment.baseurl.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(\\/.*)?`, 'i');

const apiInterceptorCondition = createInterceptorCondition<IncludeBearerTokenCondition>({
  urlPattern: apiUrlRegex
});

/**
 * Resolves once Keycloak has finished working out whether this browser is signed in.
 *
 * Anything that needs a definite answer — the route guards especially — awaits this
 * rather than reading `keycloak.authenticated`, which is simply `false` for the first
 * moment of every page load since the check no longer blocks startup (see below).
 *
 * Copied from fanvote-frontend's keycloak.config.ts, realm/client swapped for this
 * project's own Keycloak realm.
 */
@Injectable({ providedIn: 'root' })
export class KeycloakReady {
  private settle!: (authenticated: boolean) => void;
  private settled = false;

  /** Resolves to whether the visitor turned out to be signed in. Never rejects. */
  readonly whenReady = new Promise<boolean>((resolve) => (this.settle = resolve));

  resolve(authenticated: boolean): void {
    if (this.settled) return;
    this.settled = true;
    this.settle(authenticated);
  }
}

const initOptions = {
  onLoad: 'check-sso' as const,
  checkLoginIframe: false,
  silentCheckSsoRedirectUri: `${window.location.origin}/assets/silent-check-sso.html`,
  redirectUri: `${window.location.origin}/`,
  pkceMethod: 'S256' as const
};

const autoRefreshToken = withAutoRefreshToken({ onInactivityTimeout: 'none', sessionTimeout: 0 });

/**
 * Keycloak, started but not waited for.
 *
 * `provideKeycloak({ config, initOptions })` registers an app initializer that awaits
 * `keycloak.init()`, so nothing renders until the silent SSO check has completed —
 * measured at several seconds on the reference project, most of it wasted on visitors
 * who were never signed in. `initOptions` is deliberately absent from the call below so
 * the library does not register that blocking initializer, and `init()` is started by
 * hand instead, without being awaited: the page paints immediately, and everything that
 * cares reacts to the answer once it lands, through {@link KeycloakReady}.
 *
 * Two things follow from doing it this way:
 *
 * Features are normally configured by that same initializer, so dropping it drops them
 * too — `autoRefreshToken.configure()` is called explicitly below, or tokens quietly
 * stop refreshing and people are signed out mid-session.
 *
 * `keycloak.authenticated` reads false until the check lands, so a component reading it
 * in ngOnInit answers "not signed in" for everyone at first — correct only as a starting
 * assumption. Anything that matters awaits `whenReady` or listens for the Keycloak event.
 */
export const provideKeycloakAngular = () => [
  provideKeycloak({
    config: environment.keycloak,
    // No initOptions on purpose — see above. init() is called in the initializer below.
    providers: [
      AutoRefreshTokenService,
      UserActivityService,
      { provide: INCLUDE_BEARER_TOKEN_INTERCEPTOR_CONFIG, useValue: [apiInterceptorCondition] }
    ]
  }),
  provideAppInitializer(() => {
    const injector = inject(EnvironmentInjector);
    const keycloak = inject(Keycloak);
    const ready = inject(KeycloakReady);

    runInInjectionContext(injector, () => autoRefreshToken.configure());

    // Started, and deliberately not returned. Returning the promise here would restore
    // exactly the blocking behaviour this exists to remove.
    keycloak
      .init(initOptions)
      .then((authenticated) => ready.resolve(authenticated))
      .catch((error) => {
        // A failed check means signed out, not broken. The site is usable either way,
        // and a guard that never resolves would be far worse than one that says no.
        console.error('Keycloak initialisation failed — continuing as signed out', error);
        ready.resolve(false);
      });
  })
];
