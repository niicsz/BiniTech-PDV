import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-sobre-nos',
  standalone: true,
  imports: [RouterLink, MatButtonModule],
  template: `
    <div class="page">
      <article>
        <a routerLink="/" class="back">← Voltar</a>
        <h1>Sobre nós</h1>
        <p>
          A BiniTech PDV nasceu para simplificar a gestão de pequenos e médios comércios.
          Nossa plataforma reúne controle de estoque, frente de caixa, crediário e relatórios
          em uma solução na nuvem, acessível de qualquer lugar.
        </p>
        <p>
          Acreditamos que tecnologia de ponto de venda deve ser simples, confiável e acessível.
          Por isso oferecemos planos flexíveis que crescem junto com o seu negócio.
        </p>
        <a mat-flat-button color="primary" routerLink="/signup">Criar minha conta</a>
      </article>
    </div>
  `,
  styles: [`
    .page { min-height:100vh; background:#f0f4f8; padding:48px 24px; }
    article { max-width:720px; margin:0 auto; background:#fff; border-radius:16px; padding:40px; }
    .back { color:#4f46e5; text-decoration:none; font-size:14px; }
    h1 { font-size:28px; margin:16px 0 20px; }
    p { color:#475569; line-height:1.7; margin:0 0 16px; }
  `]
})
export class SobreNosComponent {}
