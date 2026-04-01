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
      <header class="app-header">
        <div class="header-inner">
          <div class="toolbar-brand" routerLink="/pdv">
            <div class="logo-wrapper">
              <mat-icon class="logo-icon">point_of_sale</mat-icon>
            </div>
            <span class="logo-text">BiniTech <span class="logo-accent">PDV</span></span>
          </div>

          <div class="nav-divider"></div>

          <nav class="nav-links">
            <a routerLink="/pdv" routerLinkActive="active-link" class="nav-item">
              <mat-icon>storefront</mat-icon>
              <span>Frente de Caixa</span>
            </a>
            <a routerLink="/products" routerLinkActive="active-link" class="nav-item">
              <mat-icon>inventory_2</mat-icon>
              <span>Produtos</span>
            </a>
            <a routerLink="/sales-report" routerLinkActive="active-link" class="nav-item">
              <mat-icon>bar_chart</mat-icon>
              <span>Relatórios</span>
            </a>
            @if (authService.isAdmin()) {
              <a routerLink="/register" routerLinkActive="active-link" class="nav-item">
                <mat-icon>group_add</mat-icon>
                <span>Usuários</span>
              </a>
            }
          </nav>

          <span class="spacer"></span>

          <div class="toolbar-actions">
            <button mat-icon-button matTooltip="Alternar Tema" (click)="themeService.toggleTheme()" class="action-btn">
              <mat-icon>{{ themeService.isDarkMode() ? 'light_mode' : 'dark_mode' }}</mat-icon>
            </button>
            <button mat-icon-button matTooltip="Configurações" (click)="showSettings = true" class="action-btn">
              <mat-icon>settings</mat-icon>
            </button>

            <div class="user-chip">
              <div class="user-avatar-wrapper">
                <mat-icon class="user-avatar">person</mat-icon>
              </div>
              <div class="user-details">
                <span class="user-name">{{ authService.getUsername() }}</span>
                <span class="user-role">{{ authService.getRole() }}</span>
              </div>
            </div>

            <button mat-icon-button matTooltip="Sair" (click)="onLogout()" class="logout-btn">
              <mat-icon>logout</mat-icon>
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
    /* ── Header Shell ── */
    .app-header {
      position: sticky;
      top: 0;
      z-index: 100;
      background: var(--header-bg);
      border-bottom: 1px solid rgba(255, 255, 255, 0.06);
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
    }
    .header-inner {
      display: flex;
      align-items: center;
      height: 56px;
      padding: 0 20px;
      max-width: 1600px;
      margin: 0 auto;
    }

    /* ── Brand ── */
    .toolbar-brand {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      flex-shrink: 0;
      text-decoration: none;
    }
    .logo-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 34px;
      height: 34px;
      border-radius: 10px;
      background: linear-gradient(135deg, #42a5f5, #1976d2);
      box-shadow: 0 2px 8px rgba(25, 118, 210, 0.35);
    }
    .logo-icon {
      font-size: 20px;
      height: 20px;
      width: 20px;
      color: #fff;
    }
    .logo-text {
      font-size: 17px;
      font-weight: 700;
      color: var(--header-text);
      white-space: nowrap;
      letter-spacing: -0.3px;
    }
    .logo-accent {
      font-weight: 800;
      color: var(--primary-light);
    }

    /* ── Divider ── */
    .nav-divider {
      width: 1px;
      height: 24px;
      background: rgba(255, 255, 255, 0.12);
      margin: 0 20px;
      flex-shrink: 0;
    }

    /* ── Navigation ── */
    .nav-links {
      display: flex;
      align-items: center;
      gap: 4px;
      height: 100%;
    }
    .nav-item {
      position: relative;
      display: inline-flex;
      align-items: center;
      gap: 7px;
      padding: 8px 14px;
      color: rgba(255, 255, 255, 0.55);
      font-weight: 500;
      font-size: 13px;
      letter-spacing: 0.1px;
      text-decoration: none;
      border-radius: 8px;
      transition: color 0.2s, background 0.2s;
      white-space: nowrap;
    }
    .nav-item mat-icon {
      font-size: 19px;
      height: 19px;
      width: 19px;
      transition: color 0.2s;
    }
    .nav-item:hover {
      color: rgba(255, 255, 255, 0.9);
      background: rgba(255, 255, 255, 0.07);
    }
    .nav-item.active-link {
      color: #fff;
      background: rgba(255, 255, 255, 0.12);
    }
    .nav-item.active-link::after {
      content: '';
      position: absolute;
      bottom: -1px;
      left: 50%;
      transform: translateX(-50%);
      width: 0;
      height: 0;
    }
    /* pill glow under active */
    .nav-item.active-link::before {
      content: '';
      position: absolute;
      inset: 0;
      border-radius: 8px;
      box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
      pointer-events: none;
    }
    .nav-item.active-link mat-icon {
      color: var(--primary-light);
    }

    .spacer {
      flex: 1;
    }

    /* ── Actions ── */
    .toolbar-actions {
      display: flex;
      align-items: center;
      gap: 2px;
    }
    .action-btn {
      color: rgba(255, 255, 255, 0.55) !important;
      transition: color 0.2s, background 0.2s;
    }
    .action-btn:hover {
      color: #fff !important;
      background: rgba(255, 255, 255, 0.07);
    }

    /* ── User Chip ── */
    .user-chip {
      display: flex;
      align-items: center;
      gap: 9px;
      padding: 5px 14px 5px 5px;
      margin: 0 6px;
      background: rgba(255, 255, 255, 0.07);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 100px;
      transition: background 0.2s;
    }
    .user-chip:hover {
      background: rgba(255, 255, 255, 0.1);
    }
    .user-avatar-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 28px;
      height: 28px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba(255,255,255,0.15), rgba(255,255,255,0.05));
    }
    .user-avatar {
      font-size: 18px;
      height: 18px;
      width: 18px;
      color: rgba(255, 255, 255, 0.85);
    }
    .user-details {
      display: flex;
      flex-direction: column;
      line-height: 1.15;
    }
    .user-name {
      font-size: 12.5px;
      font-weight: 600;
      color: rgba(255, 255, 255, 0.92);
    }
    .user-role {
      font-size: 10px;
      color: rgba(255, 255, 255, 0.45);
      text-transform: uppercase;
      letter-spacing: 0.6px;
      font-weight: 500;
    }

    /* ── Logout ── */
    .logout-btn {
      color: rgba(255, 255, 255, 0.45) !important;
      transition: color 0.2s !important;
    }
    .logout-btn:hover {
      color: #ef5350 !important;
      background: rgba(239, 83, 80, 0.1);
    }

    /* ── Main Content ── */
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

