import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="login-shell">
      <!-- BRAND SIDE -->
      <aside class="brand-side">
        <div class="glow glow-1"></div>
        <div class="glow glow-2"></div>
        <div class="brand-content">
          <div class="brand">
            <span class="brand-logo"><mat-icon>point_of_sale</mat-icon></span>
            <span class="brand-name">BiniTech <strong>PDV</strong></span>
          </div>
          <h1>Bem-vindo de volta 👋</h1>
          <p class="brand-sub">Entre para gerenciar suas vendas, estoque e relatórios em um só lugar.</p>
          <ul class="perks">
            <li><mat-icon>bolt</mat-icon> Frente de caixa rápida</li>
            <li><mat-icon>inventory_2</mat-icon> Estoque sempre em dia</li>
            <li><mat-icon>insights</mat-icon> Relatórios em tempo real</li>
          </ul>
        </div>
        <div class="brand-foot">© {{ year }} BiniTech PDV</div>
      </aside>

      <!-- FORM SIDE -->
      <main class="form-side">
        <div class="form-wrap">
          <div class="brand brand-mobile">
            <span class="brand-logo"><mat-icon>point_of_sale</mat-icon></span>
            <span class="brand-name">BiniTech <strong>PDV</strong></span>
          </div>

          <h2>Entrar na sua conta</h2>
          <p class="form-sub">Use suas credenciais para acessar o painel.</p>

          <form (ngSubmit)="onLogin()" class="login-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Loja (identificador)</mat-label>
              <mat-icon matPrefix>store</mat-icon>
              <input matInput [(ngModel)]="tenantSlug" name="tenantSlug" placeholder="ex.: minha-loja" />
              <mat-hint>Em branco apenas para administrador da plataforma.</mat-hint>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Usuário</mat-label>
              <mat-icon matPrefix>person</mat-icon>
              <input matInput [(ngModel)]="username" name="username" placeholder="Digite seu usuário" required autofocus />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Senha</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hidePassword ? 'password' : 'text'" [(ngModel)]="password"
                     name="password" placeholder="Digite sua senha" required />
              <button mat-icon-button matSuffix type="button" (click)="hidePassword = !hidePassword">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <div class="row-forgot">
              <a routerLink="/forgot-password" [queryParams]="tenantSlug ? { tenant: tenantSlug } : {}">
                Esqueci minha senha
              </a>
            </div>

            <div *ngIf="errorMessage" class="error-message">
              <mat-icon>error</mat-icon>{{ errorMessage }}
            </div>

            <button mat-flat-button color="primary" type="submit" [disabled]="loading" class="login-btn">
              <mat-icon *ngIf="!loading">login</mat-icon>
              <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
              {{ loading ? 'Entrando...' : 'Entrar' }}
            </button>
          </form>

          <div class="divider"><span>ou</span></div>

          <p class="signup-hint">
            Ainda não tem uma conta?
            <a routerLink="/signup">Cadastre sua empresa</a>
          </p>
        </div>
      </main>
    </div>
  `,
  styles: [`
    :host { display:block; }
    .login-shell { min-height:100vh; display:grid; grid-template-columns:1.05fr 1fr; font-family:'Inter',-apple-system,'Segoe UI',Roboto,sans-serif; }

    /* BRAND SIDE */
    .brand-side { position:relative; overflow:hidden; color:#e2e8f0; padding:48px;
      display:flex; flex-direction:column; justify-content:space-between;
      background:radial-gradient(900px 500px at 80% -10%, #4338ca 0%, transparent 60%), linear-gradient(160deg,#0f172a,#1e1b4b); }
    .glow { position:absolute; border-radius:50%; filter:blur(90px); opacity:.5; pointer-events:none; }
    .glow-1 { width:380px; height:380px; background:#7c3aed; top:-100px; right:-80px; }
    .glow-2 { width:320px; height:320px; background:#2563eb; bottom:-120px; left:-60px; opacity:.4; }
    .brand-content { position:relative; max-width:420px; margin:auto 0; }
    .brand { display:flex; align-items:center; gap:11px; }
    .brand-logo { display:grid; place-items:center; width:40px; height:40px; border-radius:12px;
      background:linear-gradient(135deg,#7c3aed,#4f46e5); box-shadow:0 8px 18px rgba(79,70,229,0.5); }
    .brand-logo mat-icon { color:#fff; }
    .brand-name { font-size:19px; font-weight:600; color:#fff; }
    .brand-name strong { color:#a78bfa; }
    .brand-content h1 { font-size:38px; line-height:1.1; letter-spacing:-1px; margin:32px 0 14px; color:#fff; font-weight:800; }
    .brand-sub { font-size:16px; color:#94a3b8; line-height:1.6; margin:0 0 28px; }
    .perks { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:14px; }
    .perks li { display:flex; align-items:center; gap:12px; font-size:15px; color:#cbd5e1; }
    .perks mat-icon { color:#a78bfa; background:rgba(167,139,250,0.14); border-radius:9px; padding:6px; font-size:20px; width:20px; height:20px; box-sizing:content-box; }
    .brand-foot { position:relative; font-size:12.5px; color:#64748b; }

    /* FORM SIDE */
    .form-side { display:flex; align-items:center; justify-content:center; padding:40px 24px; background:var(--bg,#f8fafc); }
    .form-wrap { width:100%; max-width:400px; }
    .brand-mobile { display:none; justify-content:center; margin-bottom:24px; }
    .form-side h2 { font-size:26px; letter-spacing:-0.5px; margin:0 0 6px; color:#0f172a; }
    .form-sub { color:#64748b; font-size:14.5px; margin:0 0 26px; }
    .login-form { display:flex; flex-direction:column; gap:8px; }
    .full-width { width:100%; }
    .row-forgot { display:flex; justify-content:flex-end; margin:-2px 2px 6px; }
    .row-forgot a { font-size:13px; color:#4f46e5; font-weight:600; text-decoration:none; }
    .error-message { display:flex; align-items:center; gap:8px; background:var(--danger-bg,#fee2e2); color:var(--danger,#b91c1c);
      padding:12px 16px; border-radius:8px; font-size:13px; margin-bottom:8px; }
    .login-btn { width:100%; height:50px; font-size:15px; font-weight:600; border-radius:12px !important;
      display:flex; align-items:center; justify-content:center; gap:8px; }
    .divider { display:flex; align-items:center; gap:14px; margin:24px 0; color:#94a3b8; font-size:12.5px; }
    .divider::before, .divider::after { content:''; flex:1; height:1px; background:#e2e8f0; }
    .signup-hint { text-align:center; font-size:13.5px; color:#64748b; margin:0; }
    .signup-hint a { color:#4f46e5; font-weight:600; text-decoration:none; }

    @media (max-width:900px) {
      .login-shell { grid-template-columns:1fr; }
      .brand-side { display:none; }
      .brand-mobile { display:flex; }
    }
  `]
})
export class LoginComponent implements OnInit {
  username = '';
  password = '';
  tenantSlug = '';
  errorMessage = '';
  loading = false;
  hidePassword = true;
  year = new Date().getFullYear();

  constructor(
    private authService: AuthService,
    private router: Router,
    private route: ActivatedRoute,
    private http: HttpClient
  ) {}

  ngOnInit(): void {
    const slug = this.route.snapshot.queryParamMap.get('tenant');
    if (slug) {
      this.tenantSlug = slug;
    }
  }

  onLogin(): void {
    if (!this.username || !this.password) {
      this.errorMessage = 'Preencha usuário e senha.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';

    const slug = this.tenantSlug.trim().toLowerCase();
    if (slug) {
      this.http
        .get<{ id: string; name: string }>(`/api/public/tenants/slug/${encodeURIComponent(slug)}`)
        .subscribe({
          next: (tenant) => this.doLogin(tenant.id),
          error: () => {
            this.loading = false;
            this.errorMessage = 'Loja não encontrada. Verifique o identificador informado.';
          }
        });
    } else {
      this.doLogin(undefined);
    }
  }

  private doLogin(tenantId?: string): void {
    this.authService.login(this.username, this.password, tenantId).subscribe({
      next: () => {
        this.loading = false;
        const target = this.authService.isSuperAdmin() ? '/admin' : '/pdv';
        this.router.navigate([target]);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Erro ao fazer login. Verifique suas credenciais.';
      }
    });
  }
}
