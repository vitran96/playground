import { Routes } from '@angular/router';
import { HomeComponent } from './features/home/page/home/home.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: "",
    component: HomeComponent,
    canActivate: [authGuard]
  }
];
