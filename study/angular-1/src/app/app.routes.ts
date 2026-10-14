import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/page/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./features/home/page/home.component').then(m => m.HomeComponent),
      }
    ]
  },
  {
    path: '**',
    loadComponent: () => import('./features/not-found/page/not-found.component').then(m => m.NotFoundComponent),
  }
];
