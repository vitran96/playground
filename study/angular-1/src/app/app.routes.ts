import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./features/login/page/login.component').then(m => m.LoginComponent),
  },
  {
    path: '',
    loadComponent: () => import('./core/layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    loadChildren: () => [
      {
        path: 'products',
        loadComponent: () => import('./features/products/page/products.component').then(m => m.ProductsComponent),
        loadChildren: () => [
          {
            path: ':id',
            loadComponent: () => import('./features/products/components/product-card.component').then(m => m.ProductCardComponent),
          }
        ],
      },
      {
        path: 'me',
        loadComponent: () => import('./features/profile/page/profile.component').then(m => m.ProfileComponent),
      },
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
