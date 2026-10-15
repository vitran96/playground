import { AuthService } from './../../auth/auth.service';
import { Component, inject } from '@angular/core';
import { Router, RouterOutlet, RouterLinkActive, RouterLink } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';

export type NavMenu = {
  label: string,
  type: 'routing'
  route: string,
  routeOptions: {
    exact: boolean,
  },
} | {
  label: string,
  type: 'action',
  onClick: () => unknown,
};

@Component({
  selector: 'app-main-layout',
  standalone: true,
  imports: [RouterOutlet, MatSidenavModule, MatListModule, MatButtonModule, MatIconModule, RouterLinkActive, RouterLink],
  templateUrl: './main-layout.component.html',
  styleUrl: './main-layout.component.scss'
})
export class MainLayoutComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  navMenuItems: NavMenu[] = [
    {
      label: 'Home',
      type: 'routing',
      route: '/',
      routeOptions: {
        exact: true,
      },
    },
    {
      label: 'Products',
      type: 'routing',
      route: '/products',
      routeOptions: {
        exact: false,
      },
    },
    {
      label: 'Profile',
      type: 'routing',
      route: '/me',
      routeOptions: {
        exact: false,
      },
    },
    {
      label: 'Logout',
      type: 'action',
      // NOTE: if use this.logout then this.authService will fail cause 'this' in logout() is not this class anymore
      onClick: () => this.logout(),
    },
  ];

  logout() {
    this.authService.logout();
    this.router.navigate(['/login'], {
      queryParams: {
        returnUrl: this.router.url,
      }
    });
  }
}
