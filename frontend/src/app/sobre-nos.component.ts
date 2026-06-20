import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-sobre-nos',
  standalone: true,
  imports: [RouterLink, MatIconModule],
  template: `
    <div class="page">
      <article>
        <a routerLink="/" class="back"><mat-icon>west</mat-icon> Voltar</a>
        <span class="kicker">[ quem somos ]</span>
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
        <a class="btn-cta" routerLink="/signup">Criar minha conta <mat-icon>arrow_forward</mat-icon></a>
      </article>
    </div>
  `,
  styles: [`
    .page { min-height:100vh; background:var(--bg); padding:64px 24px;
      background-image:radial-gradient(circle at 85% 0%, rgba(212,57,26,0.05), transparent 40%); }
    article { max-width:720px; margin:0 auto; background:var(--surface); border:2px solid var(--text);
      border-radius:var(--radius-md); padding:48px 44px; box-shadow:10px 10px 0 var(--text); }
    .back { display:inline-flex; align-items:center; gap:5px; color:var(--primary-dark); font-weight:600;
      font-size:14px; }
    .back mat-icon { font-size:18px; width:18px; height:18px; }
    .back:hover { text-decoration:underline; }
    .kicker { display:block; margin-top:20px; font-family:var(--font-mono); font-size:12px; font-weight:600;
      letter-spacing:1px; text-transform:uppercase; color:var(--primary-dark); }
    h1 { font-family:var(--font-display); font-size:42px; font-weight:800; letter-spacing:-1.5px;
      margin:8px 0 24px; color:var(--text); line-height:1; }
    p { color:var(--text-secondary); line-height:1.75; font-size:16px; margin:0 0 18px; }
    .btn-cta { display:inline-flex; align-items:center; gap:8px; margin-top:14px; font-weight:700; font-size:15px;
      color:#fff; background:var(--primary); border:2px solid var(--text); border-radius:var(--radius);
      padding:13px 24px; box-shadow:4px 4px 0 var(--text); transition:transform .18s, box-shadow .18s; }
    .btn-cta:hover { transform:translate(-2px,-2px); box-shadow:6px 6px 0 var(--text); }
    .btn-cta mat-icon { font-size:18px; width:18px; height:18px; }
  `]
})
export class SobreNosComponent {}
