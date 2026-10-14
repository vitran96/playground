import { inject, Injectable } from '@angular/core';
import { LocalStorageService } from '../storage/local-storage.service';

const AUTH_KEY = 'auth';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private localStorageService = inject(LocalStorageService);

  isLoggedIn(): boolean {
    const key: string | null = this.localStorageService.getItem(AUTH_KEY);
    return key ? true : false;
  }

  login(): void {
    this.localStorageService.setItem(AUTH_KEY, 'token');
  }

  getToken(): string {
    const token = this.localStorageService.getItem<string>(AUTH_KEY);
    return token ?? "";
  }

  logout(): void {
    this.localStorageService.removeItem(AUTH_KEY);
  }
}
