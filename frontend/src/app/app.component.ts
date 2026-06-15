import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { MatBadgeModule } from '@angular/material/badge';
import { MatSnackBarModule, MatSnackBar } from '@angular/material/snack-bar';
import { filter } from 'rxjs/operators';
import { AuthService } from './auth/services/auth.service';
import { ThemeService } from './shared/services/theme.service';
import { SettingsModalComponent } from './shared/components/settings-modal.component';
import { SaleService } from './pos/services/sale.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [
    CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule, MatTooltipModule, MatChipsModule,
    MatBadgeModule, MatSnackBarModule,
    SettingsModalComponent
  ],
  template: `
    @if (authService.isLoggedIn() && !isPublicRoute()) {
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
            <a routerLink="/debtors" routerLinkActive="active-link" class="nav-item debtors-nav"
               [matBadge]="debtorsCount > 0 ? debtorsCount : null"
               matBadgeColor="warn" matBadgeSize="small" matBadgeOverlap="false">
              <mat-icon>account_balance_wallet</mat-icon>
              <span>Devedores</span>
            </a>
            @if (authService.canManageUsers()) {
              <a routerLink="/register" routerLinkActive="active-link" class="nav-item">
                <mat-icon>group_add</mat-icon>
                <span>Usuários</span>
              </a>
            }
            @if (!authService.isSuperAdmin()) {
              <a routerLink="/billing" routerLinkActive="active-link" class="nav-item">
                <mat-icon>credit_card</mat-icon>
                <span>Assinatura</span>
              </a>
            }
            @if (authService.isSuperAdmin()) {
              <a routerLink="/admin" class="nav-item nav-item-admin">
                <mat-icon>admin_panel_settings</mat-icon>
                <span>Admin</span>
              </a>
            }
          </nav>

          <span class="spacer"></span>

          <div class="toolbar-actions">
            <button mat-icon-button matTooltip="Alternar Tema" (click)="themeService.toggleTheme()" class="action-btn">
              <mat-icon>{{ themeService.isDarkMode() ? 'light_mode' : 'dark_mode' }}</mat-icon>
            </button>
            @if (!authService.isSuperAdmin()) {
              <button mat-icon-button matTooltip="Alterar senha" routerLink="/change-password" class="action-btn">
                <mat-icon>vpn_key</mat-icon>
              </button>
            }
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

    <main [class.app-main]="authService.isLoggedIn() && !isPublicRoute()">
      <router-outlet></router-outlet>
    </main>

    @if (showSettings) {
      <app-settings-modal (close)="showSettings = false"></app-settings-modal>
    }
  `,
  styles: [`
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

    .nav-divider {
      width: 1px;
      height: 24px;
      background: rgba(255, 255, 255, 0.12);
      margin: 0 20px;
      flex-shrink: 0;
    }

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
    .nav-item-admin {
      color: #a78bfa !important;
    }
    .nav-item-admin mat-icon { color: #a78bfa !important; }
    .nav-item-admin:hover { background: rgba(124,58,237,0.12) !important; }

    .spacer {
      flex: 1;
    }

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

    .logout-btn {
      color: rgba(255, 255, 255, 0.45) !important;
      transition: color 0.2s !important;
    }
    .logout-btn:hover {
      color: #ef5350 !important;
      background: rgba(239, 83, 80, 0.1);
    }

    .app-main {
      padding: 24px;
      max-width: 1440px;
      margin: 0 auto;
      width: 100%;
    }

    @media (max-width: 1366px) {
      .header-inner {
        padding: 0 16px;
      }
      .nav-divider {
        margin: 0 14px;
      }
      .nav-item {
        padding: 7px 10px;
        font-size: 12.5px;
        gap: 5px;
      }
      .nav-item mat-icon {
        font-size: 18px;
        height: 18px;
        width: 18px;
      }
      .user-chip {
        gap: 7px;
        padding: 4px 10px 4px 4px;
        margin: 0 4px;
      }
      .user-name {
        font-size: 12px;
      }
      .app-main {
        padding: 16px;
      }
    }

    @media (max-width: 1280px) {
      .nav-item {
        padding: 6px 8px;
        font-size: 12px;
        gap: 4px;
      }
      .nav-item mat-icon {
        font-size: 17px;
        height: 17px;
        width: 17px;
      }
      .nav-divider {
        margin: 0 10px;
      }
      .logo-text {
        font-size: 15px;
      }
      .user-details {
        display: none;
      }
      .user-chip {
        padding: 4px;
        margin: 0 2px;
      }
      .app-main {
        padding: 12px;
      }
    }

    @media (max-width: 1024px) {
      .header-inner {
        padding: 0 12px;
        height: 48px;
      }
      .nav-item {
        padding: 5px 7px;
        font-size: 11.5px;
      }
      .nav-item span {
        display: none;
      }
      .nav-item mat-icon {
        font-size: 20px;
        height: 20px;
        width: 20px;
      }
      .nav-divider {
        margin: 0 8px;
        height: 20px;
      }
      .logo-wrapper {
        width: 30px;
        height: 30px;
        border-radius: 8px;
      }
      .logo-text {
        font-size: 14px;
      }
      .app-main {
        padding: 10px;
      }
    }
  `]
})
export class AppComponent implements OnInit {
  title = 'BiniTech PDV';
  authService = inject(AuthService);
  themeService = inject(ThemeService);
  private router = inject(Router);
  private saleService = inject(SaleService);
  private snackBar = inject(MatSnackBar);

  showSettings = false;
  debtorsCount = 0;

  ngOnInit(): void {
    this.loadDebtorsCount();
    this.router.events.pipe(
      filter(event => event instanceof NavigationEnd)
    ).subscribe(() => {
      if (this.authService.isLoggedIn()) {
        this.loadDebtorsCount();
      }
    });
  }

  private loadDebtorsCount(): void {
    if (!this.authService.isLoggedIn()) return;
    this.saleService.listDebtors().subscribe({
      next: (debtors) => {
        this.debtorsCount = debtors.length;
        this.checkDailyReminder(debtors.length);
      },
      error: () => {}
    });
  }

  private checkDailyReminder(count: number): void {
    if (count === 0) return;
    const storageKey = 'pdv_debtor_reminder_date';
    const today = new Date().toISOString().split('T')[0];
    const lastReminder = localStorage.getItem(storageKey);

    if (lastReminder !== today) {
      localStorage.setItem(storageKey, today);
      this.snackBar.open(
        `⚠️ Você tem ${count} venda(s) no crediário pendente(s) de cobrança!`,
        'Ver Devedores',
        { duration: 8000, horizontalPosition: 'right', verticalPosition: 'top' }
      ).onAction().subscribe(() => {
        this.router.navigate(['/debtors']);
      });
    }
  }

  isPublicRoute(): boolean {
    const url = this.router.url.split('?')[0];
    return url === '/login' || url === '/' || url.startsWith('/signup')
      || url === '/forgot-password' || url === '/reset-password'
      || url === '/termos' || url === '/privacidade' || url === '/sobre-nos' || url === '/admin';
  }

  isLoginRoute(): boolean {
    return this.isPublicRoute();
  }

  onLogout(): void {
    this.authService.logout();
  }
}

