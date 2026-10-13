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

    // NOTE: find better syntax
    // should use 4 sword s'l'll
    return key ? true : false;
  }
}
