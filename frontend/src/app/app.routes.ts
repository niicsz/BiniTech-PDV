import { Routes } from '@angular/router';
import { authGuard } from './auth/guards/auth.guard';
import { adminGuard } from './auth/guards/admin.guard';
import { superAdminGuard } from './auth/guards/super-admin.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./landing.component').then(m => m.LandingComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./signup.component').then(m => m.SignupComponent)
  },
  {
    path: 'signup/success',
    loadComponent: () => import('./signup-success.component').then(m => m.SignupSuccessComponent)
  },
  {
    path: 'sobre-nos',
    loadComponent: () => import('./sobre-nos.component').then(m => m.SobreNosComponent)
  },
  {
    path: 'termos',
    loadComponent: () => import('./termos.component').then(m => m.TermosComponent)
  },
  {
    path: 'privacidade',
    loadComponent: () => import('./privacidade.component').then(m => m.PrivacidadeComponent)
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/components/login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'forgot-password',
    loadComponent: () =>
      import('./auth/components/forgot-password/forgot-password.component').then(
        m => m.ForgotPasswordComponent
      )
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import('./auth/components/reset-password/reset-password.component').then(
        m => m.ResetPasswordComponent
      )
  },
  {
    path: 'pdv',
    loadComponent: () =>
      import('./pos/components/pos-screen/pos-screen.component').then(m => m.PosScreenComponent),
    canActivate: [authGuard]
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./products/components/product-list/product-list.component').then(m => m.ProductListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'sales-report',
    loadComponent: () =>
      import('./sales/components/sales-report/sales-report.component').then(m => m.SalesReportComponent),
    canActivate: [authGuard]
  },
  {
    path: 'debtors',
    loadComponent: () =>
      import('./debtors/components/debtors-list/debtors-list.component').then(m => m.DebtorsListComponent),
    canActivate: [authGuard]
  },
  {
    path: 'admin',
    loadComponent: () => import('./admin-dashboard.component').then(m => m.AdminDashboardComponent),
    canActivate: [superAdminGuard]
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./auth/components/register/register.component').then(m => m.RegisterComponent),
    canActivate: [adminGuard]
  },
  {
    path: 'change-password',
    loadComponent: () =>
      import('./auth/components/change-password/change-password.component').then(
        m => m.ChangePasswordComponent
      ),
    canActivate: [authGuard]
  },
  {
    path: 'billing',
    loadComponent: () => import('./billing.component').then(m => m.BillingComponent),
    canActivate: [authGuard]
  }
];

