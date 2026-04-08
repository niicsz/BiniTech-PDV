import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../services/auth.service';
import { AuthResponse } from '../../../shared/models/api.models';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="register-container">
      <mat-card class="register-card" appearance="outlined">
        <mat-card-header class="register-header">
          <mat-icon class="logo-icon">person_add</mat-icon>
          <mat-card-title>Registrar Usuário</mat-card-title>
          <mat-card-subtitle>Crie uma nova conta de acesso ao sistema</mat-card-subtitle>
        </mat-card-header>

        <mat-card-content>
          @if (successMessage) {
            <div class="success-message">
              <mat-icon>check_circle</mat-icon>
              {{ successMessage }}
            </div>
          }

          <form (ngSubmit)="onRegister()" class="register-form">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Usuário</mat-label>
              <mat-icon matPrefix>person</mat-icon>
              <input matInput [(ngModel)]="username" name="username"
                     placeholder="Digite o nome de usuário" required autofocus />
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Senha</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hidePassword ? 'password' : 'text'"
                     [(ngModel)]="password" name="password"
                     placeholder="Digite a senha (mín. 6 caracteres)" required minlength="6" />
              <button mat-icon-button matSuffix type="button"
                      (click)="hidePassword = !hidePassword">
                <mat-icon>{{ hidePassword ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Confirmar Senha</mat-label>
              <mat-icon matPrefix>lock</mat-icon>
              <input matInput [type]="hideConfirm ? 'password' : 'text'"
                     [(ngModel)]="confirmPassword" name="confirmPassword"
                     placeholder="Confirme a senha" required />
              <button mat-icon-button matSuffix type="button"
                      (click)="hideConfirm = !hideConfirm">
                <mat-icon>{{ hideConfirm ? 'visibility_off' : 'visibility' }}</mat-icon>
              </button>
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Perfil</mat-label>
              <mat-icon matPrefix>badge</mat-icon>
              <mat-select [(ngModel)]="role" name="role" required>
                <mat-option value="OPERATOR">Operador</mat-option>
                <mat-option value="ADMIN">Administrador</mat-option>
              </mat-select>
            </mat-form-field>

            @if (errorMessage) {
              <div class="error-message">
                <mat-icon>error</mat-icon>
                {{ errorMessage }}
              </div>
            }

            <button mat-flat-button color="primary" type="submit"
                    [disabled]="loading" class="register-btn">
              <mat-icon *ngIf="!loading">person_add</mat-icon>
              <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
              {{ loading ? 'Registrando...' : 'Registrar' }}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .register-container {
      display: flex;
      justify-content: center;
      padding-top: 24px;
    }
    .register-card {
      width: 100%;
      max-width: 480px;
      padding: 32px;
    }
    .register-header {
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
    .register-form {
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
    .success-message {
      display: flex;
      align-items: center;
      gap: 8px;
      background: var(--success-bg);
      color: var(--success);
      padding: 12px 16px;
      border-radius: 8px;
      font-size: 13px;
      margin-bottom: 16px;
    }
    .register-btn {
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
export class RegisterComponent {
  username = '';
  password = '';
  confirmPassword = '';
  role: 'ADMIN' | 'OPERATOR' = 'OPERATOR';
  errorMessage = '';
  successMessage = '';
  loading = false;
  hidePassword = true;
  hideConfirm = true;

  constructor(private authService: AuthService, private router: Router) {}

  onRegister(): void {
    this.errorMessage = '';
    this.successMessage = '';

    if (!this.username || !this.password || !this.confirmPassword) {
      console.warn('[RegisterComponent] Tentativa de registro com campos vazios');
      this.errorMessage = 'Preencha todos os campos.';
      return;
    }

    if (this.password.length < 6) {
      console.warn('[RegisterComponent] Senha muito curta');
      this.errorMessage = 'A senha deve ter no mínimo 6 caracteres.';
      return;
    }

    if (this.password !== this.confirmPassword) {
      console.warn('[RegisterComponent] Senhas não coincidem');
      this.errorMessage = 'As senhas não coincidem.';
      return;
    }

    this.loading = true;
    console.info('[RegisterComponent] Registrando usuário:', this.username, 'role:', this.role);
    this.authService.register(this.username, this.password, this.role).subscribe({
      next: (res: AuthResponse) => {
        this.loading = false;
        this.successMessage = `Usuário "${res.username}" criado com sucesso como ${res.role}.`;
        console.info('[RegisterComponent] Usuário registrado com sucesso:', res.username);
        this.username = '';
        this.password = '';
        this.confirmPassword = '';
        this.role = 'OPERATOR';
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err?.error?.message || 'Erro ao registrar usuário.';
        console.error('[RegisterComponent] Erro no registro:', this.errorMessage);
      }
    });
  }
}

