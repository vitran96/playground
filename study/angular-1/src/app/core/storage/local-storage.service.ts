import { DOCUMENT } from '@angular/common';
import { inject, Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class LocalStorageService {
  private win = inject(DOCUMENT).defaultView;

  getItem<T>(key: string): T | null {
    const val = this.win?.localStorage.getItem(key);
    return val ? (JSON.parse(val) as T): null
  }

  setItem(key: string, value: unknown): void {
    this.win?.localStorage.setItem(key, JSON.stringify(value));
  }

  removeItem(key: string): void {
    this.win?.localStorage.removeItem(key);
  }
}
