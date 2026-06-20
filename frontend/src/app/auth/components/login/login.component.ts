import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
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
    :host { display:block; font-family:var(--font-family); }
    .login-shell { min-height:100vh; display:grid; grid-template-columns:1.05fr 1fr; }

    /* BRAND SIDE — espresso panel */
    .brand-side { position:relative; overflow:hidden; color:#e9dfce; padding:52px;
      display:flex; flex-direction:column; justify-content:space-between;
      background:#241a12;
      background-image:
        radial-gradient(700px 420px at 82% -8%, rgba(212,57,26,0.28) 0%, transparent 58%),
        radial-gradient(560px 360px at 0% 110%, rgba(28,107,70,0.22) 0%, transparent 60%); }
    .glow { position:absolute; border-radius:50%; filter:blur(90px); opacity:.4; pointer-events:none; }
    .glow-1 { width:380px; height:380px; background:#d4391a; top:-100px; right:-80px; }
    .glow-2 { width:320px; height:320px; background:#1c6b46; bottom:-120px; left:-60px; opacity:.35; }
    .brand-content { position:relative; max-width:440px; margin:auto 0; }
    .brand { display:flex; align-items:center; gap:11px; }
    .brand-logo { display:grid; place-items:center; width:42px; height:42px; border-radius:var(--radius);
      background:var(--primary); border:2px solid #120d09; box-shadow:3px 3px 0 #120d09; }
    .brand-logo mat-icon { color:#fff; }
    .brand-name { font-family:var(--font-display); font-size:21px; font-weight:800; color:#fff; letter-spacing:-.4px; }
    .brand-name strong { color:var(--primary-light); }
    .brand-content h1 { font-family:var(--font-display); font-size:44px; line-height:1.02; letter-spacing:-1.5px;
      margin:34px 0 16px; color:#fff; font-weight:800; }
    .brand-sub { font-size:16px; color:#b8a892; line-height:1.6; margin:0 0 30px; }
    .perks { list-style:none; padding:0; margin:0; display:flex; flex-direction:column; gap:14px; }
    .perks li { display:flex; align-items:center; gap:12px; font-size:15px; color:#d8cab4; }
    .perks mat-icon { color:#fff; background:var(--primary); border:1.5px solid #120d09; border-radius:var(--radius);
      padding:6px; font-size:20px; width:20px; height:20px; box-sizing:content-box; }
    .perks li:nth-child(2) mat-icon { background:var(--accent); }
    .perks li:nth-child(3) mat-icon { background:var(--success); }
    .brand-foot { position:relative; font-family:var(--font-mono); font-size:12px; letter-spacing:.5px; color:#8a7d6e; }

    /* FORM SIDE — paper */
    .form-side { display:flex; align-items:center; justify-content:center; padding:40px 24px; background:var(--bg);
      background-image:radial-gradient(circle at 90% 5%, rgba(212,57,26,0.05), transparent 38%); }
    .form-wrap { width:100%; max-width:400px; }
    .brand-mobile { display:none; justify-content:center; margin-bottom:24px; }
    .brand-mobile .brand-name { color:var(--text); }
    .brand-mobile .brand-name strong { color:var(--primary-dark); }
    .brand-mobile .brand-logo { border-color:var(--text); box-shadow:3px 3px 0 var(--text); }
    .form-side h2 { font-family:var(--font-display); font-size:30px; font-weight:800; letter-spacing:-1px;
      margin:0 0 6px; color:var(--text); }
    .form-sub { color:var(--text-secondary); font-size:14.5px; margin:0 0 26px; }
    .login-form { display:flex; flex-direction:column; gap:8px; }
    .full-width { width:100%; }
    .row-forgot { display:flex; justify-content:flex-end; margin:-2px 2px 6px; }
    .row-forgot a { font-size:13px; color:var(--primary-dark); font-weight:600; text-decoration:none; }
    .row-forgot a:hover { text-decoration:underline; }
    .error-message { display:flex; align-items:center; gap:8px; background:var(--danger-bg); color:var(--danger);
      padding:12px 16px; border-radius:var(--radius); border:1.5px solid var(--danger); font-size:13px; margin-bottom:8px; }
    .login-btn { width:100%; height:50px; font-size:15px; font-weight:700; border-radius:var(--radius) !important;
      display:flex; align-items:center; justify-content:center; gap:8px; }
    .divider { display:flex; align-items:center; gap:14px; margin:24px 0; color:var(--text-tertiary);
      font-family:var(--font-mono); font-size:12px; text-transform:uppercase; letter-spacing:1px; }
    .divider::before, .divider::after { content:''; flex:1; height:2px; background:var(--border); }
    .signup-hint { text-align:center; font-size:13.5px; color:var(--text-secondary); margin:0; }
    .signup-hint a { color:var(--primary-dark); font-weight:700; text-decoration:none; }
    .signup-hint a:hover { text-decoration:underline; }

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
    this.resolveTenantId(slug).subscribe({
      next: (tenantId) => this.doLogin(tenantId),
      error: () => {
        this.loading = false;
        this.errorMessage = 'Loja não encontrada. Verifique o identificador informado.';
      }
    });
  }

  private resolveTenantId(slug: string): Observable<string | undefined> {
    if (!slug) {
      return of(undefined);
    }
    return this.http
      .get<{ id: string; name: string }>(`/api/public/tenants/slug/${encodeURIComponent(slug)}`)
      .pipe(map((tenant) => tenant.id));
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
