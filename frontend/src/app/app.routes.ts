import { Routes } from '@angular/router';
import { canActivateAuthRole } from './guard/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./pages/home/home').then(m => m.HomePage),
  },
  {
    path: 'services',
    loadComponent: () => import('./pages/services/services-page').then(m => m.ServicesPage),
  },
  {
    // Auth-guarded: booking needs to know who is booking.
    path: 'book',
    loadComponent: () => import('./pages/book/book').then(m => m.BookPage),
    canActivate: [canActivateAuthRole],
  },
  {
    path: 'account',
    loadComponent: () => import('./pages/account/account').then(m => m.AccountPage),
    canActivate: [canActivateAuthRole],
  },
  {
    path: 'admin',
    loadComponent: () => import('./pages/admin/admin').then(m => m.AdminPage),
    canActivate: [canActivateAuthRole],
    data: { role: 'clinic-admin' },
  },
  {
    path: 'forbidden',
    loadComponent: () => import('./pages/forbidden/forbidden').then(m => m.ForbiddenPage),
  },
  { path: '**', redirectTo: '' },
];
