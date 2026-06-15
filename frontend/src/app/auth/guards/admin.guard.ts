import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  if (authService.isLoggedIn() && authService.canManageUsers()) {
    console.debug('[AdminGuard] Acesso permitido - usuário pode gerenciar usuários');
    return true;
  }

  console.warn('[AdminGuard] Acesso negado - sem permissão de gestão de usuários, redirecionando para /pdv');
  router.navigate(['/pdv']);
  return false;
};
