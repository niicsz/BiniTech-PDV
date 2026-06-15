import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="cp-container">
      <mat-card class="cp-card" appearance="outlined">
        <mat-card-header class="cp-header">
          <mat-icon class="logo-icon">vpn_key</mat-icon>
          <mat-card-title>Alterar Senha</mat-card-title>
          <mat-card-subtitle>Atualize sua senha de acesso</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          @if (successMessage) {
            <div class="success-message"><mat-icon>check_circle</mat-icon>{{ successMessage }}</div>
          }

          <form (ngSubmit)="onSubmit()" class="cp-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Senha atual</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hideCurrent ? 'password' : 'text'"
                     [(ngModel)]="currentPassword" name="currentPassword" required />
              <button mat-icon-button matSuffix type="button" (click)="hideCurrent = !hideCurrent">
                <mat-icon>{{ hideCurrent ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Nova senha</mat-label>
              <mat-icon matPrefix>lock_reset</mat-icon>
              <input matInput [type]="hideNew ? 'password' : 'text'"
                     [(ngModel)]="newPassword" name="newPassword" required minlength="6"
                     placeholder="Mínimo 6 caracteres" />
              <button mat-icon-button matSuffix type="button" (click)="hideNew = !hideNew">
                <mat-icon>{{ hideNew ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Confirmar nova senha</mat-label>
              <mat-icon matPrefix>lock_reset</mat-icon>
              <input matInput [type]="hideConfirm ? 'password' : 'text'"
                     [(ngModel)]="confirmPassword" name="confirmPassword" required />
              <button mat-icon-button matSuffix type="button" (click)="hideConfirm = !hideConfirm">
                <mat-icon>{{ hideConfirm ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            @if (errorMessage) {
              <div class="error-message"><mat-icon>error</mat-icon>{{ errorMessage }}</div>
            }

            <div class="cp-actions">
              <button mat-stroked-button type="button" (click)="cancel()">Cancelar</button>
              <button mat-flat-button color="primary" type="submit" [disabled]="loading" class="cp-btn">
                <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
                {{ loading ? 'Salvando...' : 'Alterar senha' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .cp-container { display:flex; justify-content:center; padding-top:24px; }
    .cp-card { width:100%; max-width:460px; padding:32px; }
    .cp-header { display:flex; flex-direction:column; align-items:center; text-align:center; margin-bottom:24px; }
    .logo-icon { font-size:48px; height:48px; width:48px; color:var(--primary-dark); margin-bottom:8px; }
    .cp-form { display:flex; flex-direction:column; gap:4px; }
    .full-width { width:100%; }
    .error-message { display:flex; align-items:center; gap:8px; background:var(--danger-bg); color:var(--danger); padding:12px 16px; border-radius:8px; font-size:13px; margin-bottom:8px; }
    .success-message { display:flex; align-items:center; gap:8px; background:var(--success-bg); color:var(--success); padding:12px 16px; border-radius:8px; font-size:13px; margin-bottom:16px; }
    .cp-actions { display:flex; gap:12px; justify-content:flex-end; margin-top:8px; }
    .cp-btn { display:flex; align-items:center; gap:8px; }
  `]
})
export class ChangePasswordComponent implements OnInit {
  currentPassword = '';
  newPassword = '';
  confirmPassword = '';
  errorMessage = '';
  successMessage = '';
  loading = false;
  hideCurrent = true;
  hideNew = true;
  hideConfirm = true;

  constructor(private authService: AuthService, private router: Router) {}

  ngOnInit(): void {
    // Super admin não altera senha por aqui (gerenciada por variável de ambiente).
    if (this.authService.isSuperAdmin()) {
      this.router.navigate(['/admin']);
    }
  }

  onSubmit(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.currentPassword || !this.newPassword || !this.confirmPassword) {
      this.errorMessage = 'Preencha todos os campos.';
      return;
    }
    if (this.newPassword.length < 6) {
      this.errorMessage = 'A nova senha deve ter no mínimo 6 caracteres.';
      return;
    }
    if (this.newPassword !== this.confirmPassword) {
      this.errorMessage = 'A confirmação não coincide com a nova senha.';
      return;
    }

    this.loading = true;
    this.authService.changePassword(this.currentPassword, this.newPassword).subscribe({
      next: () => {
        this.loading = false;
        this.successMessage = 'Senha alterada com sucesso!';
        this.currentPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Erro ao alterar a senha.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/pdv']);
  }
}
