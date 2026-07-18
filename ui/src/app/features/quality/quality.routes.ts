import { Routes } from '@angular/router';

export const QUALITY_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/quality-page/quality-page').then((m) => m.QualityPage)
  }
];
