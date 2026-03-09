import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="app-header">
      <div class="header-content">
        <h1 class="logo">
          <span class="material-icons">point_of_sale</span>
          BiniTech PDV
        </h1>
        <nav class="nav-links">
          <a routerLink="/pdv" routerLinkActive="active">
            <span class="material-icons">shopping_cart</span> Frente de Caixa
          </a>
          <a routerLink="/products" routerLinkActive="active">
            <span class="material-icons">inventory_2</span> Produtos
          </a>
          <a routerLink="/sales-report" routerLinkActive="active">
            <span class="material-icons">assessment</span> Relatórios
          </a>
        </nav>
      </div>
    </header>
    <main class="app-main">
      <router-outlet></router-outlet>
    </main>
  `,
  styles: [`
    .app-header {
      background: var(--primary-dark);
      color: #fff;
      padding: 0 24px;
      height: 56px;
      box-shadow: var(--shadow);
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .header-content {
      max-width: 1400px;
      margin: 0 auto;
      display: flex;
      align-items: center;
      height: 100%;
      gap: 32px;
    }
    .logo {
      font-size: 20px;
      font-weight: 700;
      display: flex;
      align-items: center;
      gap: 8px;
      white-space: nowrap;
    }
    .nav-links {
      display: flex;
      gap: 4px;
      height: 100%;
    }
    .nav-links a {
      color: rgba(255,255,255,0.8);
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 0 16px;
      height: 100%;
      font-size: 14px;
      font-weight: 500;
      transition: all 0.2s;
      border-bottom: 3px solid transparent;
    }
    .nav-links a:hover {
      color: #fff;
      background: rgba(255,255,255,0.08);
    }
    .nav-links a.active {
      color: #fff;
      border-bottom-color: var(--accent);
      background: rgba(255,255,255,0.05);
    }
    .nav-links .material-icons { font-size: 20px; }
    .app-main {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }
  `]
})
export class AppComponent {
  title = 'BiniTech PDV';
}

