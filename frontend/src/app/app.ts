import { Component, effect, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEventType, ReadyArgs, typeEventArgs } from 'keycloak-angular';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  authenticated = false;

  private readonly keycloak = inject(Keycloak);
  private readonly keycloakSignal = inject(KEYCLOAK_EVENT_SIGNAL);

  /** Realm roles straight off the token — the same claim the backend enforces on. */
  private get realmRoles(): string[] {
    return (this.keycloak?.tokenParsed as any)?.realm_access?.roles ?? [];
  }

  get isAdmin(): boolean {
    return this.realmRoles.includes('clinic-admin');
  }

  constructor() {
    effect(() => {
      const event = this.keycloakSignal();
      if (event.type === KeycloakEventType.Ready) {
        this.authenticated = typeEventArgs<ReadyArgs>(event.args);
      }
      if (event.type === KeycloakEventType.AuthLogout) {
        this.authenticated = false;
      }
    });
  }

  login(): void {
    this.keycloak.login({ redirectUri: window.location.href });
  }

  logout(): void {
    this.keycloak.logout({ redirectUri: window.location.origin + '/' });
  }
}
