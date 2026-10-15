import { Component, inject, input } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './../../../../core/auth/auth.service';
import { MatButton } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [MatButton, MatCardModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private authService = inject(AuthService);
  private router = inject(Router);

  returnUrl = input<string>('');

  login() {
    // NOTE: should we handle failure here?
    this.authService.login();

    const returnValue = this.returnUrl();
    if (returnValue && '/' !== returnValue) {
      console.log('aaa')
      this.router.navigate([returnValue]);
    } else {
      this.router.navigate(['/']);
    }
  }

}
