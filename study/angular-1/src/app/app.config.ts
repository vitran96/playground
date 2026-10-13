import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { provideStoreDevtools } from '@ngrx/store-devtools';

// NOTE: is this like interceptor or injection?
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding())
    , provideHttpClient(
      // add authInterceptor and errorInterceptor
      withInterceptors([])
    )
    // NOTE: required by UI framework like Material UI / NgZorro
    , provideAnimationsAsync()
    // NOTE: what is this??
    // , {
    //   provide: MAT_FORM_FIELD_DEAFULT_OPTIONS
    //   , useValue: { appearance: 'outline' }
    // }
    // NOTE: required by NgRx
    , provideStore()
    , provideEffects()
    , provideStoreDevtools({ maxAge: 25 })
  ]
};
