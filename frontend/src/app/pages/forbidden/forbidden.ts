import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-forbidden',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="container">
      <h1>Acces interzis</h1>
      <p class="muted">Nu ai drepturile necesare pentru a accesa această pagină.</p>
      <a routerLink="/" class="btn btn-primary">Înapoi la pagina principală</a>
    </div>
  `,
})
export class ForbiddenPage {
}
