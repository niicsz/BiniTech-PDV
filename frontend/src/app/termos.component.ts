import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-termos',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="page">
      <article>
        <a routerLink="/" class="back">← Voltar</a>
        <h1>Termos de uso</h1>
        <p>Ao utilizar a plataforma BiniTech PDV, você concorda com os termos descritos abaixo.</p>
        <h2>1. Uso do serviço</h2>
        <p>O serviço é fornecido mediante assinatura de um dos planos disponíveis. O uso deve respeitar a legislação vigente.</p>
        <h2>2. Assinatura e cobrança</h2>
        <p>As assinaturas são cobradas mensalmente. O não pagamento pode resultar na suspensão do acesso.</p>
        <h2>3. Responsabilidades</h2>
        <p>O cliente é responsável pela veracidade dos dados cadastrados e pelo uso adequado das credenciais de acesso.</p>
        <h2>4. Cancelamento</h2>
        <p>A assinatura pode ser cancelada a qualquer momento, com efeito ao fim do período vigente.</p>
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
export class TermosComponent {}
