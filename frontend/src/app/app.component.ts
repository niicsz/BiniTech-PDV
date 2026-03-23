import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { AuthService } from './auth/services/auth.service';
import { ThemeService } from './shared/services/theme.service';
import { SettingsModalComponent } from './shared/components/settings-modal.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive, SettingsModalComponent],
  template: `
    @if (authService.isLoggedIn() && !isLoginRoute()) {
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
            @if (authService.isAdmin()) {
              <a routerLink="/register" routerLinkActive="active">
                <span class="material-icons">person_add</span> Usuários
              </a>
            }
          </nav>
          <div class="user-info">
            <button class="theme-btn" (click)="themeService.toggleTheme()" title="Alternar Tema">
              <span class="material-icons">{{ themeService.isDarkMode() ? 'light_mode' : 'dark_mode' }}</span>
            </button>
            <button class="settings-btn" (click)="showSettings = true" title="Configurações">
              <span class="material-icons">account_circle</span>
            </button>
            <span class="username">{{ authService.getUsername() }}</span>
            <span class="role-badge">{{ authService.getRole() }}</span>
            <button class="logout-btn" (click)="onLogout()" title="Sair">
              <span class="material-icons">logout</span>
            </button>
          </div>
        </div>
      </header>
    }
    <main [class.app-main]="authService.isLoggedIn() && !isLoginRoute()">
      <router-outlet></router-outlet>
    </main>

    @if (showSettings) {
      <app-settings-modal (close)="showSettings = false"></app-settings-modal>
    }
  `,
  styles: [`
    .app-header {
      background: var(--header-bg);
      color: var(--header-text);
      padding: 0 24px;
      height: 56px;
      box-shadow: var(--shadow);
      position: sticky;
      top: 0;
      z-index: 100;
      transition: background-color 0.3s, color 0.3s;
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
      flex: 1;
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
    .dark-theme .nav-links a {
      color: rgba(224, 224, 224, 0.8);
    }
    .dark-theme .nav-links a:hover {
      color: #fff;
      background: rgba(255,255,255,0.08);
    }
    .dark-theme .nav-links a.active {
      color: #fff;
      background: rgba(255,255,255,0.05);
    }
    .nav-links .material-icons { font-size: 20px; }
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-left: auto;
      white-space: nowrap;
    }
    .user-info .material-icons {
      font-size: 22px;
    }
    .username {
      font-size: 14px;
      font-weight: 500;
    }
    .role-badge {
      font-size: 11px;
      background: rgba(255,255,255,0.2);
      padding: 2px 8px;
      border-radius: 10px;
      font-weight: 600;
    }
    .logout-btn, .theme-btn, .settings-btn {
      background: none;
      border: none;
      color: var(--header-text);
      opacity: 0.8;
      cursor: pointer;
      padding: 4px;
      display: flex;
      align-items: center;
      border-radius: 50%;
      transition: all 0.2s;
    }
    .logout-btn:hover, .theme-btn:hover, .settings-btn:hover {
      opacity: 1;
      background: rgba(255,255,255,0.15);
    }
    .app-main {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }
  `]
})
export class AppComponent {
  title = 'BiniTech PDV';
  authService = inject(AuthService);
  themeService = inject(ThemeService);
  private router = inject(Router);
  
  showSettings = false;

  isLoginRoute(): boolean {
    return this.router.url === '/login';
  }

  onLogout(): void {
    this.authService.logout();
  }
}


