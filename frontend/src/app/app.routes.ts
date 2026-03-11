import { Routes } from '@angular/router';
import { authGuard } from './auth/guards/auth.guard';
import { adminGuard } from './auth/guards/admin.guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'pdv',
    pathMatch: 'full'
  },
  {
    path: 'login',
    loadComponent: () =>
      import('./auth/components/login/login.component').then(m => m.LoginComponent)
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
    path: 'register',
    loadComponent: () =>
      import('./auth/components/register/register.component').then(m => m.RegisterComponent),
    canActivate: [adminGuard]
  }
];

