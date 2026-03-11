import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-container">
      <div class="login-card">
        <div class="login-header">
          <span class="material-icons logo-icon">point_of_sale</span>
          <h1>BiniTech PDV</h1>
          <p>Faça login para continuar</p>
        </div>
        <form (ngSubmit)="onLogin()" class="login-form">
          <div class="form-group">
            <label for="username">
              <span class="material-icons">person</span> Usuário
            </label>
            <input
              id="username"
              type="text"
              [(ngModel)]="username"
              name="username"
              placeholder="Digite seu usuário"
              required
              autofocus
            />
          </div>
          <div class="form-group">
            <label for="password">
              <span class="material-icons">lock</span> Senha
            </label>
            <input
              id="password"
              type="password"
              [(ngModel)]="password"
              name="password"
              placeholder="Digite sua senha"
              required
            />
          </div>
          @if (errorMessage) {
            <div class="error-message">
              <span class="material-icons">error</span>
              {{ errorMessage }}
            </div>
          }
          <button type="submit" [disabled]="loading" class="login-btn">
            @if (loading) {
              <span>Entrando...</span>
            } @else {
              <span class="material-icons">login</span> Entrar
            }
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      min-height: 100vh;
      background: var(--bg, #f0f2f5);
    }
    .login-card {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.10);
      padding: 40px;
      width: 100%;
      max-width: 400px;
    }
    .login-header {
      text-align: center;
      margin-bottom: 32px;
    }
    .logo-icon {
      font-size: 48px;
      color: var(--primary-dark, #1a237e);
    }
    .login-header h1 {
      margin: 8px 0 4px;
      font-size: 24px;
      color: var(--primary-dark, #1a237e);
    }
    .login-header p {
      color: #666;
      font-size: 14px;
    }
    .form-group {
      margin-bottom: 20px;
    }
    .form-group label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 14px;
      font-weight: 500;
      margin-bottom: 6px;
      color: #333;
    }
    .form-group label .material-icons {
      font-size: 18px;
    }
    .form-group input {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
      transition: border-color 0.2s;
      box-sizing: border-box;
    }
    .form-group input:focus {
      outline: none;
      border-color: var(--primary-dark, #1a237e);
    }
    .error-message {
      display: flex;
      align-items: center;
      gap: 6px;
      background: #fdecea;
      color: #c62828;
      padding: 10px 14px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 16px;
    }
    .error-message .material-icons {
      font-size: 18px;
    }
    .login-btn {
      width: 100%;
      padding: 12px;
      background: var(--primary-dark, #1a237e);
      color: #fff;
      border: none;
      border-radius: 8px;
      font-size: 15px;
      font-weight: 600;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      transition: background 0.2s;
    }
    .login-btn:hover:not(:disabled) {
      background: #283593;
    }
    .login-btn:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  errorMessage = '';
  loading = false;

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

