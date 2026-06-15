import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-signup-success',
  standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="success-page">
      <div class="success-card">
        <mat-icon class="check">check_circle</mat-icon>
        <h1>Cadastro recebido!</h1>
        <p>
          Sua empresa foi cadastrada e está <strong>aguardando aprovação</strong>.
          Assim que for aprovada, você receberá um e-mail com as credenciais de acesso.
        </p>
        <a mat-flat-button color="primary" routerLink="/login">Ir para o login</a>
      </div>
    </div>
  `,
  styles: [`
    .success-page { display:flex; justify-content:center; align-items:center; min-height:100vh; background:var(--bg,#f0f4f8); padding:24px; }
    .success-card { background:#fff; border-radius:16px; padding:40px; max-width:480px; text-align:center; box-shadow:0 8px 24px rgba(15,23,42,0.08); }
    .check { font-size:64px; height:64px; width:64px; color:#22c55e; margin-bottom:8px; }
    h1 { font-size:24px; margin:0 0 12px; }
    p { color:#475569; line-height:1.6; margin:0 0 24px; }
  `]
})
export class SignupSuccessComponent {}
