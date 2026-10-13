import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class CookieService {
  // NOTE: why not get cookie?
  private doc = inject(DOCUMENT)

  getCookie(name: string): string | null {
    const matches = this.doc.cookie.match(name);
    return matches ? decodeURIComponent(matches[1]) : null;
  }

  setCookie(name:string, value:string, days = 7): void {
    const date = new Date();
    date.setTime(date.getTime() + days * 86400000);
    this.doc.cookie = `${name}=${encodeURIComponent(value)};expires=${date.toUTCString()};path=/;SameSite=Lax`;
  }
}
