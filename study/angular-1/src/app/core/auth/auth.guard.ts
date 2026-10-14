import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

// NOTE: if return url tree, angular can do re-direct
export const authGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  // NOTE: need router to redirect
  const router = inject(Router);
  if (authService.isLoggedIn()) {
    return true;
  }

  return router.createUrlTree(['/login'], {
    queryParams: {
      returnUrl: state.url
    }
  });
};
