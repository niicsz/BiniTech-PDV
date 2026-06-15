import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';

let isRefreshing = false;

export const authInterceptor: HttpInterceptorFn = (req: HttpRequest<any>, next: HttpHandlerFn) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (req.url.includes('/api/auth/login') || req.url.includes('/api/auth/refresh')) {
    return next(req);
  }

  const token = authService.getToken();
  let authReq = req;

  if (token) {
    authReq = req.clone({
      setHeaders: { Authorization: `Bearer ${token}` }
    });
  } else {
    console.warn('[AuthInterceptor] Requisição sem token para:', req.url);
  }

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !isRefreshing) {
        console.warn('[AuthInterceptor] Recebido 401, tentando renovar token para:', req.url);
        isRefreshing = true;
        return authService.refreshToken().pipe(
          switchMap(res => {
            isRefreshing = false;
            console.info('[AuthInterceptor] Token renovado, reenviando requisição:', req.url);
            const newReq = req.clone({
              setHeaders: { Authorization: `Bearer ${res.accessToken}` }
            });
            return next(newReq);
          }),
          catchError(refreshError => {
            isRefreshing = false;
            console.error('[AuthInterceptor] Falha ao renovar token, realizando logout');
            authService.logout();
            return throwError(() => refreshError);
          })
        );
      }
      if (error.status === 402) {
        // Tenant bloqueado/suspenso por pagamento: leva o usuário para a tela de billing.
        console.warn('[AuthInterceptor] Recebido 402 (pagamento pendente), redirecionando para /billing');
        router.navigate(['/billing']);
        return throwError(() => error);
      }
      if (error.status >= 500) {
        console.error('[AuthInterceptor] Erro do servidor:', error.status, req.url, error.message);
      } else if (error.status >= 400) {
        console.warn('[AuthInterceptor] Erro do cliente:', error.status, req.url, error.error?.message);
      }
      return throwError(() => error);
    })
  );
};
