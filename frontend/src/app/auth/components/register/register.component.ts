import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../../shared/models/api.models';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="register-container">
      <div class="register-card">
        <div class="register-header">
          <span class="material-icons logo-icon">person_add</span>
          <h1>Registrar Usuário</h1>
          <p>Crie uma nova conta de acesso ao sistema</p>
        </div>

        @if (successMessage) {
          <div class="success-message">
            <span class="material-icons">check_circle</span>
            {{ successMessage }}
          </div>
        }

        <form (ngSubmit)="onRegister()" class="register-form">
          <div class="form-group">
            <label for="username">
              <span class="material-icons">person</span> Usuário
            </label>
            <input
              id="username"
              type="text"
              [(ngModel)]="username"
              name="username"
              placeholder="Digite o nome de usuário"
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
              placeholder="Digite a senha (mín. 6 caracteres)"
              required
              minlength="6"
            />
          </div>
          <div class="form-group">
            <label for="confirmPassword">
              <span class="material-icons">lock</span> Confirmar Senha
            </label>
            <input
              id="confirmPassword"
              type="password"
              [(ngModel)]="confirmPassword"
              name="confirmPassword"
              placeholder="Confirme a senha"
              required
            />
          </div>
          <div class="form-group">
            <label for="role">
              <span class="material-icons">badge</span> Perfil
            </label>
            <select id="role" [(ngModel)]="role" name="role" required>
              <option value="OPERATOR">Operador</option>
              <option value="ADMIN">Administrador</option>
            </select>
          </div>

          @if (errorMessage) {
            <div class="error-message">
              <span class="material-icons">error</span>
              {{ errorMessage }}
            </div>
          }

          <button type="submit" [disabled]="loading" class="register-btn">
            @if (loading) {
              <span>Registrando...</span>
            } @else {
              <span class="material-icons">person_add</span> Registrar
            }
          </button>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      padding-top: 24px;
    }
    .register-card {
      background: var(--surface);
      border-radius: 12px;
      box-shadow: 0 4px 24px rgba(0,0,0,0.10);
      padding: 40px;
      width: 100%;
      max-width: 460px;
    }
    .register-header {
      text-align: center;
      margin-bottom: 32px;
    }
    .logo-icon {
      font-size: 48px;
      color: var(--primary-dark);
    }
    .register-header h1 {
      margin: 8px 0 4px;
      font-size: 22px;
      color: var(--primary-dark);
    }
    .register-header p {
      color: var(--text-secondary);
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
      color: var(--text);
    }
    .form-group label .material-icons {
      font-size: 18px;
    }
    .form-group input,
    .form-group select {
      width: 100%;
      padding: 10px 12px;
      border: 1px solid var(--border);
      border-radius: 8px;
      font-size: 14px;
      transition: border-color 0.2s;
      box-sizing: border-box;
      background: var(--input-bg);
      color: var(--text);
    }
    .form-group input:focus,
    .form-group select:focus {
      outline: none;
      border-color: var(--primary-dark);
    }
    .error-message {
      display: flex;
      align-items: center;
      gap: 6px;
      background: var(--danger-bg);
      color: var(--danger);
      padding: 10px 14px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 16px;
    }
    .error-message .material-icons {
      font-size: 18px;
    }
    .success-message {
      display: flex;
      align-items: center;
      gap: 6px;
      background: var(--success-bg);
      color: var(--success);
      padding: 10px 14px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 16px;
    }
    .success-message .material-icons {
      font-size: 18px;
    }
    .register-btn {
      width: 100%;
      padding: 12px;
      background: var(--primary-dark);
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
    .register-btn:hover:not(:disabled) {
      background: var(--primary);
    }
    .register-btn:disabled {
      opacity: 0.7;
      cursor: not-allowed;
    }
  `]
})
export class RegisterComponent {
  username = '';
  password = '';
  confirmPassword = '';
  role: 'ADMIN' | 'OPERATOR' = 'OPERATOR';
  errorMessage = '';
  successMessage = '';
  loading = false;

  constructor(private authService: AuthService, private router: Router) {}

  onRegister(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.username || !this.password || !this.confirmPassword) {
      this.errorMessage = 'Preencha todos os campos.';
      return;
    }

    if (this.password.length < 6) {
      this.errorMessage = 'A senha deve ter no mínimo 6 caracteres.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      this.errorMessage = 'As senhas não coincidem.';
      return;
    }

    this.loading = true;
    this.authService.register(this.username, this.password, this.role).subscribe({
      next: (res: AuthResponse) => {
        this.loading = false;
        this.successMessage = `Usuário "${res.username}" criado com sucesso como ${res.role}.`;
        this.username = '';
        this.password = '';
        this.confirmPassword = '';
        this.role = 'OPERATOR';
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Erro ao registrar usuário.';
      }
    });
  }
}



