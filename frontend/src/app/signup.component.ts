import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { TenantPublicService } from './tenant-public.service';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  template: `
    <div class="signup-page">
      <mat-card class="signup-card" appearance="outlined">
        <div class="brand">
          <mat-icon class="logo-icon">point_of_sale</mat-icon>
          <span>BiniTech <strong>PDV</strong></span>
        </div>
        <h1>Crie a conta da sua empresa</h1>
        <p class="subtitle">Comece agora. Após o cadastro, sua conta passará por uma rápida aprovação.</p>

        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Nome da empresa</mat-label>
            <input matInput formControlName="name" placeholder="Ex.: Mercado do João" />
            <mat-error *ngIf="form.get('name')?.hasError('required')">Informe o nome da empresa.</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>E-mail de cobrança</mat-label>
            <input matInput formControlName="billingEmail" type="email" placeholder="contato@empresa.com" />
            <mat-error *ngIf="form.get('billingEmail')?.hasError('required')">Informe o e-mail.</mat-error>
            <mat-error *ngIf="form.get('billingEmail')?.hasError('email')">E-mail inválido.</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Plano</mat-label>
            <mat-select formControlName="planId">
              <mat-option value="starter">Starter — R$ 99/mês</mat-option>
              <mat-option value="pro">Pro — R$ 199/mês</mat-option>
              <mat-option value="enterprise">Enterprise — R$ 349/mês</mat-option>
            </mat-select>
          </mat-form-field>

          <div *ngIf="errorMessage" class="error-message">
            <mat-icon>error</mat-icon>{{ errorMessage }}
          </div>

          <button mat-flat-button color="primary" type="submit" [disabled]="loading" class="submit-btn">
            <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
            {{ loading ? 'Enviando...' : 'Criar conta' }}
          </button>
        </form>

        <p class="login-hint">Já tem uma conta? <a routerLink="/login">Entrar</a></p>
      </mat-card>
    </div>
  `,
  styles: [`
    .signup-page { display:flex; justify-content:center; align-items:center; min-height:100vh; background:var(--bg,#f0f4f8); padding:24px; }
    .signup-card { width:100%; max-width:460px; padding:32px; }
    .brand { display:flex; align-items:center; gap:8px; font-size:18px; margin-bottom:16px; }
    .brand .logo-icon { color:#4f46e5; }
    h1 { font-size:22px; margin:0 0 6px; }
    .subtitle { color:#64748b; font-size:14px; margin:0 0 24px; }
    form { display:flex; flex-direction:column; gap:4px; }
    .full-width { width:100%; }
    .submit-btn { width:100%; height:48px; font-weight:600; display:flex; align-items:center; justify-content:center; gap:8px; }
    .error-message { display:flex; align-items:center; gap:8px; background:#fee2e2; color:#b91c1c; padding:10px 14px; border-radius:8px; font-size:13px; margin-bottom:8px; }
    .login-hint { text-align:center; font-size:13px; color:#64748b; margin-top:16px; }
    .login-hint a { color:#4f46e5; font-weight:600; text-decoration:none; }
  `]
})
export class SignupComponent {
  private fb = inject(FormBuilder);
  private tenantService = inject(TenantPublicService);
  private router = inject(Router);

  loading = false;
  errorMessage = '';

  form = this.fb.group({
    name: ['', Validators.required],
    billingEmail: ['', [Validators.required, Validators.email]],
    planId: ['starter', Validators.required]
  });

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.errorMessage = '';
    const { name, billingEmail, planId } = this.form.getRawValue();
    this.tenantService
      .createTenant({ name: name!, billingEmail: billingEmail!, planId: planId! })
      .subscribe({
        next: () => this.router.navigate(['/signup/success']),
        error: (err: HttpErrorResponse) => {
          this.loading = false;
          this.errorMessage = err?.error?.message || 'Não foi possível concluir o cadastro. Tente novamente.';
        }
      });
  }
}
