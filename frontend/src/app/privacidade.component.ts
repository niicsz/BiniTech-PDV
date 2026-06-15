import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-privacidade',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page">
      <article>
        <a routerLink="/" class="back">← Voltar</a>
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
    .page { min-height:100vh; background:#f0f4f8; padding:48px 24px; }
    article { max-width:720px; margin:0 auto; background:#fff; border-radius:16px; padding:40px; }
    .back { color:#4f46e5; text-decoration:none; font-size:14px; }
    h1 { font-size:28px; margin:16px 0 20px; }
    h2 { font-size:18px; margin:24px 0 8px; }
    p { color:#475569; line-height:1.7; margin:0; }
  `]
})
export class PrivacidadeComponent {}
