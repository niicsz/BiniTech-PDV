import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'pdv',
    pathMatch: 'full'
  },
  {
    path: 'pdv',
    loadComponent: () =>
      import('./pos/components/pos-screen/pos-screen.component').then(m => m.PosScreenComponent)
  },
  {
    path: 'products',
    loadComponent: () =>
      import('./products/components/product-list/product-list.component').then(m => m.ProductListComponent)
  },
  {
    path: 'sales-report',
    loadComponent: () =>
      import('./sales/components/sales-report/sales-report.component').then(m => m.SalesReportComponent)
  }
];

