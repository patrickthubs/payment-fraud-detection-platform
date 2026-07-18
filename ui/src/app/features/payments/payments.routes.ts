import { Routes } from '@angular/router';

export const PAYMENTS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/payments-page/payments-page').then((m) => m.PaymentsPage)
  }
];
