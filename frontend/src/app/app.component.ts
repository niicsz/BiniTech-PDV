import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { AuthService } from './auth/services/auth.service';
import { ThemeService } from './shared/services/theme.service';
import { SettingsModalComponent } from './shared/components/settings-modal.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule, MatChipsModule,
    SettingsModalComponent
  ],
  template: `
    @if (authService.isLoggedIn() && !isLoginRoute()) {
      <mat-toolbar class="app-toolbar">
        <div class="toolbar-brand">
          <mat-icon class="logo-icon">point_of_sale</mat-icon>
          <span class="logo-text">BiniTech PDV</span>
        </div>

        <nav class="nav-links">
          <a mat-button routerLink="/pdv" routerLinkActive="active-link">
            <mat-icon>shopping_cart</mat-icon>
            <span>Frente de Caixa</span>
          </a>
          <a mat-button routerLink="/products" routerLinkActive="active-link">
            <mat-icon>inventory_2</mat-icon>
            <span>Produtos</span>
          </a>
          <a mat-button routerLink="/sales-report" routerLinkActive="active-link">
            <mat-icon>assessment</mat-icon>
            <span>Relatórios</span>
          </a>
          @if (authService.isAdmin()) {
            <a mat-button routerLink="/register" routerLinkActive="active-link">
              <mat-icon>person_add</mat-icon>
              <span>Usuários</span>
            </a>
          }
        </nav>

        <span class="spacer"></span>

        <div class="toolbar-actions">
          <button mat-icon-button matTooltip="Alternar Tema" (click)="themeService.toggleTheme()">
            <mat-icon>{{ themeService.isDarkMode() ? 'light_mode' : 'dark_mode' }}</mat-icon>
          </button>
          <button mat-icon-button matTooltip="Configurações" (click)="showSettings = true">
            <mat-icon>settings</mat-icon>
          </button>

          <div class="user-info">
            <mat-icon class="user-avatar">account_circle</mat-icon>
            <div class="user-details">
              <span class="user-name">{{ authService.getUsername() }}</span>
              <span class="user-role">{{ authService.getRole() }}</span>
            </div>
          </div>

          <button mat-icon-button matTooltip="Sair" (click)="onLogout()" class="logout-btn">
            <mat-icon>logout</mat-icon>
          </button>
        </div>
      </mat-toolbar>
    }

    <main [class.app-main]="authService.isLoggedIn() && !isLoginRoute()">
      <router-outlet></router-outlet>
    </main>

    @if (showSettings) {
      <app-settings-modal (close)="showSettings = false"></app-settings-modal>
    }
  `,
  styles: [`
    .app-toolbar {
      position: sticky;
      top: 0;
      z-index: 100;
      background: var(--header-bg) !important;
      color: var(--header-text) !important;
      border-bottom: 1px solid rgba(255, 255, 255, 0.08);
      padding: 0 16px;
      gap: 0;
      min-height: 56px;
    }
    .toolbar-brand {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-right: 24px;
      flex-shrink: 0;
    }
    .logo-icon {
      font-size: 26px;
      height: 26px;
      width: 26px;
      color: var(--primary-light);
    }
    .logo-text {
      font-size: 18px;
      font-weight: 700;
      white-space: nowrap;
      letter-spacing: -0.3px;
    }
    .nav-links {
      display: flex;
      gap: 2px;
      height: 100%;
    }
    .nav-links a {
      color: rgba(255,255,255,0.7);
      font-weight: 500;
      font-size: 13px;
      border-radius: 8px;
      padding: 6px 12px;
      transition: all 0.2s ease;
    }
    .nav-links a mat-icon {
      font-size: 18px;
      height: 18px;
      width: 18px;
      margin-right: 4px;
    }
    .nav-links a:hover {
      color: #fff;
      background: rgba(255,255,255,0.08);
    }
    .nav-links a.active-link {
      color: #fff;
      background: rgba(255,255,255,0.15);
    }
    .spacer {
      flex: 1;
    }
    .toolbar-actions {
      display: flex;
      align-items: center;
      gap: 4px;
    }
    .toolbar-actions .mat-mdc-icon-button,
    :host ::ng-deep .app-toolbar .mat-mdc-icon-button {
      color: rgba(255,255,255,0.7);
    }
    .toolbar-actions .mat-mdc-icon-button:hover {
      color: #fff;
    }
    .user-info {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px 12px;
      margin: 0 4px;
      background: rgba(255,255,255,0.08);
      border-radius: 24px;
    }
    .user-avatar {
      font-size: 24px;
      height: 24px;
      width: 24px;
      color: rgba(255,255,255,0.8);
    }
    .user-details {
      display: flex;
      flex-direction: column;
      line-height: 1.2;
    }
    .user-name {
      font-size: 13px;
      font-weight: 600;
      color: #fff;
    }
    .user-role {
      font-size: 10px;
      color: rgba(255,255,255,0.6);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .logout-btn {
      color: rgba(255,255,255,0.6) !important;
    }
    .logout-btn:hover {
      color: #ef5350 !important;
    }
    .app-main {
      padding: 24px;
      max-width: 1440px;
      margin: 0 auto;
      width: 100%;
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

