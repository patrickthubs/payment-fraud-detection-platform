import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { AuthStateService } from './auth-state.service';

export const basicAuthInterceptor: HttpInterceptorFn = (request, next) => {
  const authState = inject(AuthStateService);
  const authHeader = authState.buildAuthHeader();

  if (!authHeader || request.headers.has('Authorization')) {
    return next(request);
  }

  return next(
    request.clone({
      setHeaders: {
        Authorization: authHeader
      }
    })
  );
};
