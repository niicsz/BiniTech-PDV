import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn()) {
    console.debug('[AuthGuard] Acesso permitido - usuário autenticado');
    return true;
  }

  console.warn('[AuthGuard] Acesso negado - usuário não autenticado, redirecionando para /login');
  router.navigate(['/login']);
  return false;
};
