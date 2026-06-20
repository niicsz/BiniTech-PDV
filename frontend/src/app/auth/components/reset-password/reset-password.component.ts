import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-reset-password',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="auth-bg">
      <div class="glow glow-1"></div>
      <div class="glow glow-2"></div>
      <mat-card class="auth-card" appearance="outlined">
        <div class="brand">
          <span class="brand-logo"><mat-icon>lock_reset</mat-icon></span>
          <span class="brand-name">BiniTech <strong>PDV</strong></span>
        </div>

        @if (done) {
          <div class="done">
            <mat-icon class="done-ico">check_circle</mat-icon>
            <h1>Senha redefinida!</h1>
            <p>Sua senha foi alterada com sucesso. Você já pode entrar com a nova senha.</p>
            <a mat-flat-button color="primary" routerLink="/login">Ir para o login</a>
          </div>
        } @else if (!token) {
          <div class="done">
            <mat-icon class="done-ico err">link_off</mat-icon>
            <h1>Link inválido</h1>
            <p>O link de redefinição é inválido ou está incompleto. Solicite um novo.</p>
            <a mat-flat-button color="primary" routerLink="/forgot-password">Solicitar novo link</a>
          </div>
        } @else {
          <h1>Criar nova senha</h1>
          <p class="subtitle">Escolha uma nova senha para a sua conta.</p>

          <form (ngSubmit)="onSubmit()" class="auth-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nova senha</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hideNew ? 'password' : 'text'" [(ngModel)]="newPassword"
                     name="newPassword" required minlength="6" placeholder="Mínimo 6 caracteres" />
              <button mat-icon-button matSuffix type="button" (click)="hideNew = !hideNew">
                <mat-icon>{{ hideNew ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Confirmar nova senha</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hideConfirm ? 'password' : 'text'" [(ngModel)]="confirmPassword"
                     name="confirmPassword" required />
              <button mat-icon-button matSuffix type="button" (click)="hideConfirm = !hideConfirm">
                <mat-icon>{{ hideConfirm ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            @if (errorMessage) {
              <div class="error-message"><mat-icon>error</mat-icon>{{ errorMessage }}</div>
            }

            <button mat-flat-button color="primary" type="submit" [disabled]="loading" class="submit-btn">
              <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
              {{ loading ? 'Salvando...' : 'Redefinir senha' }}
            </button>
          </form>
        }
      </mat-card>
    </div>
  `,
  styles: [`
    .auth-bg { position:relative; min-height:100vh; display:flex; align-items:center; justify-content:center; padding:24px;
      font-family:var(--font-family); background:#241a12;
      background-image:
        radial-gradient(800px 460px at 72% -8%, rgba(212,57,26,0.26) 0%, transparent 58%),
        radial-gradient(560px 360px at 10% 110%, rgba(28,107,70,0.20) 0%, transparent 60%); overflow:hidden; }
    .glow { position:absolute; border-radius:50%; filter:blur(90px); opacity:.4; pointer-events:none; }
    .glow-1 { width:380px; height:380px; background:#d4391a; top:-120px; right:-60px; }
    .glow-2 { width:320px; height:320px; background:#1c6b46; bottom:-140px; left:-80px; opacity:.35; }
    .auth-card { position:relative; width:100%; max-width:440px; padding:38px !important;
      background:var(--surface) !important; border:2px solid var(--text) !important;
      border-radius:var(--radius-md) !important; box-shadow:10px 10px 0 var(--text) !important; }
    .brand { display:flex; align-items:center; gap:11px; margin-bottom:22px; }
    .brand-logo { display:grid; place-items:center; width:40px; height:40px; border-radius:var(--radius);
      background:var(--primary); border:2px solid var(--text); box-shadow:3px 3px 0 var(--text); }
    .brand-logo mat-icon { color:#fff; }
    .brand-name { font-family:var(--font-display); font-size:20px; font-weight:800; letter-spacing:-.4px; color:var(--text); }
    .brand-name strong { color:var(--primary-dark); }
    h1 { font-family:var(--font-display); font-size:28px; font-weight:800; letter-spacing:-.8px; margin:0 0 6px; color:var(--text); }
    .subtitle { color:var(--text-secondary); font-size:14px; margin:0 0 22px; }
    .auth-form { display:flex; flex-direction:column; gap:6px; }
    .full-width { width:100%; }
    .error-message { display:flex; align-items:center; gap:8px; background:var(--danger-bg); color:var(--danger);
      padding:12px 16px; border-radius:var(--radius); border:1.5px solid var(--danger); font-size:13px; margin-bottom:8px; }
    .submit-btn { width:100%; height:48px; font-weight:700; display:flex; align-items:center; justify-content:center; gap:8px; border-radius:var(--radius) !important; }
    .done { text-align:center; padding:8px 0; }
    .done-ico { font-size:56px; height:56px; width:56px; color:var(--success); margin-bottom:8px; }
    .done-ico.err { color:var(--danger); }
    .done h1 { margin-bottom:12px; }
    .done p { color:var(--text-secondary); line-height:1.6; margin:0 0 24px; }
  `]
})
export class ResetPasswordComponent implements OnInit {
  token: string | null = null;
  newPassword = '';
  confirmPassword = '';
  loading = false;
  done = false;
  errorMessage = '';
  hideNew = true;
  hideConfirm = true;

  constructor(private authService: AuthService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token');
  }

  onSubmit(): void {
    this.errorMessage = '';
    if (!this.token) {
      return;
    }
    if (this.newPassword.length < 6) {
      this.errorMessage = 'A senha deve ter no mínimo 6 caracteres.';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'A confirmação não coincide com a nova senha.';
      return;
    }
    this.loading = true;
    this.authService.resetPassword(this.token, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.done = true;
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Não foi possível redefinir a senha. O link pode ter expirado.';
      }
    });
  }
}
