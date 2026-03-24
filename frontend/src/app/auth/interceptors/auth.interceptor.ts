import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { BehaviorSubject, catchError, filter, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;
let refreshTokenSubject = new BehaviorSubject<string | null>(null);

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<any>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);

  if (req.url.includes('/api/auth/login') || req.url.includes('/api/auth/refresh')) {
    return next(req);
  }

  const token = authService.getToken();
  let authReq = req;

  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 || error.status === 403) {
        if (!isRefreshing) {
          isRefreshing = true;
          refreshTokenSubject.next(null);

          return authService.refreshToken().pipe(
            switchMap(res => {
              isRefreshing = false;
              refreshTokenSubject.next(res.accessToken);
              const newReq = req.clone({
                setHeaders: { Authorization: `Bearer ${res.accessToken}` }
              });
              return next(newReq);
            }),
            catchError(refreshError => {
              isRefreshing = false;
              authService.logout();
              return throwError(() => refreshError);
            })
          );
        } else {
          return refreshTokenSubject.pipe(
            filter(token => token !== null),
            take(1),
            switchMap(token => {
              const newReq = req.clone({
                setHeaders: { Authorization: `Bearer ${token}` }
              });
              return next(newReq);
            })
          );
        }
      }
      return throwError(() => error);
    })
  );
};
