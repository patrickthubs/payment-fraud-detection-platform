import { Routes } from '@angular/router';

export const CASES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/cases-page/cases-page').then((m) => m.CasesPage)
  }
];
