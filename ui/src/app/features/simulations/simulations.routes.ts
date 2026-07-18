import { Routes } from '@angular/router';

export const SIMULATIONS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./pages/simulations-page/simulations-page').then((m) => m.SimulationsPage)
  }
];
