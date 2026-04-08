import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.isAdmin()) {
    console.debug('[AdminGuard] Acesso permitido - usuário é ADMIN');
    return true;
  }

  console.warn('[AdminGuard] Acesso negado - usuário não é ADMIN, redirecionando para /pdv');
  router.navigate(['/pdv']);
  return false;
};
