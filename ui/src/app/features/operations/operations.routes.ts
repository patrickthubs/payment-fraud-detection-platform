import { Routes } from '@angular/router';

export const OPERATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/operations-page/operations-page').then((m) => m.OperationsPage)
  }
];
