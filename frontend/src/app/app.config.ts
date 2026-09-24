import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { routes } from './app.routes';
import { provideApi } from './api';
import { ClockService } from './core/clock.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    // Same origin: the dev server proxies /api to the backend (research R-2).
    provideApi(''),
    provideAppInitializer(() => inject(ClockService).load()),
  ]
};
