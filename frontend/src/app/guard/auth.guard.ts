import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { inject } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KeycloakReady } from '../keycloak.config';

/**
 * Guards the routes that need an account, and some that need the clinic-admin role.
 *
 * Copied from fanvote-frontend's auth.guard.ts. It awaits {@link KeycloakReady} first,
 * because startup does not block on the silent SSO check any more — reading
 * `keycloak.authenticated` at the very start of a guard would answer "signed out" for
 * everybody, including people who are perfectly well signed in, and bounce them to a
 * login screen they do not need.
 *
 * Roles are read from the token here rather than from the library's snapshot, since
 * that snapshot is taken before this function runs and would still be the stale
 * "signed out" one.
 */
const isAccessAllowed = async (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
): Promise<boolean | UrlTree> => {
  const keycloak = inject(Keycloak);
  const router = inject(Router);
  const ready = inject(KeycloakReady);

  const authenticated = await ready.whenReady;

  if (!authenticated) {
    await keycloak.login({
      redirectUri: window.location.origin + state.url, // preserve target route
    });
    return false;
  }

  const requiredRole = route.data['role'];
  if (!requiredRole) {
    return true;
  }

  // Both kinds of role, not just client ones — clinic-admin is a realm role, so the
  // gateway checks realm_access.roles. Checking resourceRoles alone would bounce an
  // admin holding a perfectly good token to /forbidden.
  const claims = (keycloak.tokenParsed ?? {}) as any;
  const realmRoles: string[] = claims.realm_access?.roles ?? [];
  const resourceRoles: string[] = Object.values(
    (claims.resource_access ?? {}) as Record<string, { roles?: string[] }>
  ).flatMap((r) => r.roles ?? []);

  if (realmRoles.includes(requiredRole) || resourceRoles.includes(requiredRole)) {
    return true;
  }

  return router.parseUrl('/forbidden');
};

export const canActivateAuthRole: CanActivateFn = isAccessAllowed;
