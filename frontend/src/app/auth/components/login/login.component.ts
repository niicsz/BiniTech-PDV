import { Component } from '@angular/core';
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
  selector: 'app-login',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="login-container">
      <mat-card class="login-card" appearance="outlined">
        <mat-card-header class="login-header">
          <mat-icon class="logo-icon">point_of_sale</mat-icon>
          <mat-card-title>BiniTech PDV</mat-card-title>
          <mat-card-subtitle>Faça login para continuar</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          <form (ngSubmit)="onLogin()" class="login-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Usuário</mat-label>
              <mat-icon matPrefix>person</mat-icon>
              <input matInput [(ngModel)]="username" name="username"
                     placeholder="Digite seu usuário" required autofocus />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Senha</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hidePassword ? 'password' : 'text'"
                     [(ngModel)]="password" name="password"
                     placeholder="Digite sua senha" required />
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword = !hidePassword">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <div *ngIf="errorMessage" class="error-message">
              <mat-icon>error</mat-icon>
              {{ errorMessage }}
            </div>

            <button mat-flat-button color="primary" type="submit"
                    [disabled]="loading" class="login-btn">
              <mat-icon *ngIf="!loading">login</mat-icon>
              <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
              {{ loading ? 'Entrando...' : 'Entrar' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: var(--bg);
    }
    .login-card {
      width: 100%;
      max-width: 420px;
      padding: 32px;
    }
    .login-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      margin-bottom: 24px;
    }
    .logo-icon {
      font-size: 48px;
      height: 48px;
      width: 48px;
      color: var(--primary-dark);
      margin-bottom: 8px;
    }
    .login-form {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }
    .full-width {
      width: 100%;
    }
    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--danger-bg);
      color: var(--danger);
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 8px;
    }
    .login-btn {
      width: 100%;
      height: 48px;
      font-size: 15px;
      font-weight: 600;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';
  loading = false;
  hidePassword = true;

  constructor(private authService: AuthService, private router: Router) {}

  onLogin(): void {
    if (!this.username || !this.password) {
      this.errorMessage = 'Preencha usuário e senha.';
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    this.authService.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/pdv']);
      },
      error: (err) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Erro ao fazer login. Verifique suas credenciais.';
      }
    });
  }
}

