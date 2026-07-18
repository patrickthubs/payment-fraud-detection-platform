import { Routes } from '@angular/router';

export const OVERVIEW_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/overview-page/overview-page').then((m) => m.OverviewPage)
  }
];
