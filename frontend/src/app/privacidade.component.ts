import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-privacidade',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  template: `
    <div class="page">
      <article>
        <a routerLink="/" class="back"><mat-icon>west</mat-icon> Voltar</a>
        <span class="kicker">[ legal · lgpd ]</span>
        <h1>Política de Privacidade</h1>
        <p>Respeitamos a sua privacidade e protegemos os dados tratados na plataforma.</p>
        <h2>1. Dados coletados</h2>
        <p>Coletamos dados de cadastro da empresa, de usuários e informações operacionais necessárias ao funcionamento do PDV.</p>
        <h2>2. Uso dos dados</h2>
        <p>Os dados são utilizados exclusivamente para a prestação do serviço, cobrança e suporte.</p>
        <h2>3. Compartilhamento</h2>
        <p>Não compartilhamos seus dados com terceiros, exceto provedores essenciais (ex.: processamento de pagamentos).</p>
        <h2>4. Seus direitos</h2>
        <p>Você pode solicitar acesso, correção ou exclusão dos seus dados conforme a LGPD.</p>
      </article>
    </div>
  `,
  styles: [`
    .page { min-height:100vh; background:var(--bg); padding:64px 24px;
      background-image:radial-gradient(circle at 85% 0%, rgba(212,57,26,0.05), transparent 40%); }
    article { max-width:720px; margin:0 auto; background:var(--surface); border:2px solid var(--text);
      border-radius:var(--radius-md); padding:48px 44px; box-shadow:10px 10px 0 var(--text); }
    .back { display:inline-flex; align-items:center; gap:5px; color:var(--primary-dark); font-weight:600; font-size:14px; }
    .back mat-icon { font-size:18px; width:18px; height:18px; }
    .back:hover { text-decoration:underline; }
    .kicker { display:block; margin-top:20px; font-family:var(--font-mono); font-size:12px; font-weight:600;
      letter-spacing:1px; text-transform:uppercase; color:var(--primary-dark); }
    h1 { font-family:var(--font-display); font-size:42px; font-weight:800; letter-spacing:-1.5px;
      margin:8px 0 22px; color:var(--text); line-height:1; }
    h2 { font-family:var(--font-display); font-size:21px; font-weight:700; letter-spacing:-.3px;
      margin:30px 0 8px; color:var(--text); }
    p { color:var(--text-secondary); line-height:1.75; font-size:15.5px; margin:0; }
  `]
})
export class PrivacidadeComponent {}
