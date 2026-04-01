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
      <mat-toolbar color="primary" class="app-toolbar">
        <mat-icon class="logo-icon">point_of_sale</mat-icon>
        <span class="logo-text">BiniTech PDV</span>

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

        <button mat-icon-button matTooltip="Alternar Tema" (click)="themeService.toggleTheme()">
          <mat-icon>{{ themeService.isDarkMode() ? 'light_mode' : 'dark_mode' }}</mat-icon>
        </button>
        <button mat-icon-button matTooltip="Configurações" (click)="showSettings = true">
          <mat-icon>settings</mat-icon>
        </button>

        <mat-chip class="user-chip">
          <mat-icon matChipAvatar>account_circle</mat-icon>
          {{ authService.getUsername() }}
          <span class="role-badge">{{ authService.getRole() }}</span>
        </mat-chip>

        <button mat-icon-button matTooltip="Sair" (click)="onLogout()">
          <mat-icon>logout</mat-icon>
        </button>
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
      gap: 8px;
    }
    .logo-icon {
      margin-right: 4px;
    }
    .logo-text {
      font-size: 20px;
      font-weight: 700;
      margin-right: 16px;
      white-space: nowrap;
    }
    .nav-links {
      display: flex;
      gap: 4px;
      height: 100%;
    }
    .nav-links a {
      color: rgba(255,255,255,0.85);
      font-weight: 500;
    }
    .nav-links a:hover {
      color: #fff;
    }
    .nav-links a.active-link {
      color: #fff;
      background: rgba(255,255,255,0.12);
    }
    .spacer {
      flex: 1;
    }
    .user-chip {
      margin: 0 4px;
      font-size: 13px;
    }
    .role-badge {
      font-size: 10px;
      background: rgba(255,255,255,0.2);
      padding: 1px 6px;
      border-radius: 8px;
      font-weight: 600;
      margin-left: 6px;
    }
    .app-main {
      padding: 24px;
      max-width: 1400px;
      margin: 0 auto;
    }
    :host ::ng-deep .mat-toolbar {
      background: var(--header-bg) !important;
      color: var(--header-text) !important;
    }
    :host ::ng-deep .mat-toolbar .mat-mdc-icon-button {
      color: var(--header-text);
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

