import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

/**
 * Landing page — "O Balcão" (The Counter).
 *
 * Aesthetic direction: warm mercantile / editorial. The whole product lives at a
 * physical retail counter, so the page borrows that world: warm paper-cream
 * surfaces, deep espresso ink, a vivid commerce-vermillion accent and a
 * money-green secondary. Figures and receipts use a monospace face for a tactile,
 * ledger-like feel. The hero showcase is a real perforated thermal receipt.
 *
 * Type: Fraunces (display serif), Hanken Grotesk (body), Spline Sans Mono (figures).
 * Loaded globally in index.html.
 */
@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, MatIconModule],
  template: `
    <div class="lp">
      <!-- TICKER -->
      <div class="ticker" aria-hidden="true">
        <div class="ticker-track">
          <span *ngFor="let _ of [0,1]" class="ticker-run">
            Mercadinhos <i>✺</i> Padarias <i>✺</i> Lojas de bairro <i>✺</i> Hortifrúti
            <i>✺</i> Açougues <i>✺</i> Papelarias <i>✺</i> Adegas <i>✺</i> Conveniências
            <i>✺</i> Farmácias <i>✺</i> Pet shops <i>✺</i>
          </span>
        </div>
      </div>

      <!-- NAVBAR -->
      <header class="nav">
        <div class="nav-inner">
          <a class="brand" (click)="scrollTo('top')">
            <span class="brand-mark"><mat-icon>point_of_sale</mat-icon></span>
            <span class="brand-name">BiniTech<em>PDV</em></span>
          </a>
          <nav class="nav-links">
            <a (click)="scrollTo('recursos')">Recursos</a>
            <a (click)="scrollTo('planos')">Planos</a>
            <a routerLink="/sobre-nos">Sobre</a>
          </nav>
          <div class="nav-cta">
            <a class="ghost" routerLink="/login">Entrar</a>
            <a class="btn btn-solid" routerLink="/signup">
              Começar grátis <mat-icon>north_east</mat-icon>
            </a>
          </div>
        </div>
      </header>

      <!-- HERO -->
      <section class="hero" id="top">
        <div class="hero-inner">
          <div class="hero-copy reveal">
            <span class="eyebrow"><span class="blip"></span> Gestão multi-loja na nuvem</span>
            <h1>
              O caixa que faz<br />
              sua loja <span class="mark-word">vender mais</span>
              <span class="serif-stamp">.</span>
            </h1>
            <p class="lede">
              Frente de caixa, estoque, crediário e relatórios numa plataforma simples,
              rápida e segura. Comece em minutos — sem instalar nada no balcão.
            </p>
            <div class="hero-actions">
              <a class="btn btn-solid btn-lg" routerLink="/signup">
                Criar conta grátis <mat-icon>arrow_forward</mat-icon>
              </a>
              <a class="btn btn-outline btn-lg" (click)="scrollTo('planos')">Ver planos</a>
            </div>
            <ul class="hero-stats">
              <li><strong>2 min</strong><span>pra abrir o caixa</span></li>
              <li><strong>0</strong><span>instalação</span></li>
              <li><strong>100%</strong><span>na nuvem</span></li>
            </ul>
          </div>

          <!-- RECEIPT MOCK -->
          <div class="hero-visual reveal" style="animation-delay:.15s">
            <div class="receipt">
              <div class="receipt-head">
                <span class="r-store">BINITECH&nbsp;PDV</span>
                <span class="r-sub">FRENTE DE CAIXA · CUPOM</span>
              </div>
              <div class="r-rule"></div>
              <div class="r-line"><span>Arroz tipo 1 · 5kg</span><b>27,90</b></div>
              <div class="r-line"><span>Refrigerante · 2L</span><b>8,50</b></div>
              <div class="r-line"><span>Café torrado · 500g</span><b>19,90</b></div>
              <div class="r-rule dashed"></div>
              <div class="r-total"><span>TOTAL</span><b>R$ 56,30</b></div>
              <div class="r-pay"><span>Pix · aprovado</span><b>56,30</b></div>
              <div class="r-rule dashed"></div>
              <button class="r-finish" type="button">
                <mat-icon>check_circle</mat-icon> Venda finalizada
              </button>
              <div class="r-barcode" aria-hidden="true"></div>
              <div class="r-foot">OBRIGADO · VOLTE SEMPRE</div>
            </div>
            <div class="stamp">
              <div class="stamp-num">+ R$ 4.280</div>
              <div class="stamp-label">vendas hoje</div>
            </div>
          </div>
        </div>
      </section>

      <!-- MARQUEE STAT STRIP -->
      <section class="strip">
        <div class="strip-inner">
          <div class="strip-item"><b>R$ 27,90</b><span>código de barras</span></div>
          <div class="strip-item"><b>Pix · Cartão · Fiado</b><span>todas as formas</span></div>
          <div class="strip-item"><b>Baixa automática</b><span>estoque sempre certo</span></div>
          <div class="strip-item"><b>Relatório do dia</b><span>em um toque</span></div>
        </div>
      </section>

      <!-- FEATURES -->
      <section class="features" id="recursos">
        <div class="section-head">
          <span class="kicker">[ tudo em um só lugar ]</span>
          <h2>Recursos feitos para<br /> o seu balcão</h2>
          <p>Do caixa ao relatório, sem planilha e sem dor de cabeça.</p>
        </div>
        <div class="feature-grid">
          <article class="feature reveal" *ngFor="let f of features; let i = index"
                   [style.animation-delay]="(i * 0.06) + 's'">
            <span class="feature-num">{{ pad(i + 1) }}</span>
            <div class="feature-ico"><mat-icon>{{ f.icon }}</mat-icon></div>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </article>
        </div>
      </section>

      <!-- PRICING -->
      <section class="pricing" id="planos">
        <div class="section-head">
          <span class="kicker">[ planos ]</span>
          <h2>Preço justo,<br /> sem surpresa</h2>
          <p>Escolha o tamanho da sua operação. Excedentes só se você crescer.</p>
        </div>
        <div class="plan-grid">
          <article class="plan reveal" *ngFor="let p of plans; let i = index"
                   [class.featured]="p.featured" [style.animation-delay]="(i * 0.08) + 's'">
            <div class="plan-badge" *ngIf="p.featured">Mais popular</div>
            <h3>{{ p.name }}</h3>
            <div class="plan-price">
              <span class="cur">R$</span><span class="num">{{ p.price }}</span><span class="per">/mês</span>
            </div>
            <div class="plan-rule"></div>
            <ul>
              <li *ngFor="let item of p.items"><mat-icon>check</mat-icon> {{ item }}</li>
            </ul>
            <a class="btn plan-cta" [class.btn-solid]="p.featured" [class.btn-outline]="!p.featured"
               routerLink="/signup">
              Assinar {{ p.name }}
            </a>
          </article>
        </div>
      </section>

      <!-- CTA BAND -->
      <section class="cta-band">
        <div class="cta-inner">
          <span class="cta-kicker">✺ Comece hoje ✺</span>
          <h2>Pronto para modernizar sua loja?</h2>
          <p>Crie sua conta agora e venda ainda hoje. Sem cartão, sem compromisso.</p>
          <a class="btn cta-btn btn-lg" routerLink="/signup">
            Começar grátis <mat-icon>arrow_forward</mat-icon>
          </a>
        </div>
      </section>

      <!-- FOOTER -->
      <footer class="footer">
        <div class="footer-inner">
          <div class="footer-brand">
            <a class="brand" (click)="scrollTo('top')">
              <span class="brand-mark"><mat-icon>point_of_sale</mat-icon></span>
              <span class="brand-name">BiniTech<em>PDV</em></span>
            </a>
            <p>Gestão de vendas simples para o comércio brasileiro.</p>
          </div>
          <div class="footer-col">
            <h4>Produto</h4>
            <a (click)="scrollTo('recursos')">Recursos</a>
            <a (click)="scrollTo('planos')">Planos</a>
            <a routerLink="/login">Entrar</a>
          </div>
          <div class="footer-col">
            <h4>Empresa</h4>
            <a routerLink="/sobre-nos">Sobre nós</a>
            <a routerLink="/signup">Criar conta</a>
          </div>
          <div class="footer-col">
            <h4>Legal</h4>
            <a routerLink="/termos">Termos de uso</a>
            <a routerLink="/privacidade">Privacidade</a>
          </div>
        </div>
        <div class="footer-bottom">
          <span>© {{ year }} BiniTech PDV</span>
          <span>Feito para o balcão brasileiro 🇧🇷</span>
        </div>
      </footer>
    </div>
  `,
  styles: [`
    /* ====== TOKENS ====== */
    .lp {
      --paper:#f3ede1;          /* warm cream background */
      --paper-2:#ece3d3;        /* deeper cream */
      --surface:#fbf7ef;        /* card surface */
      --ink:#241a12;            /* espresso near-black */
      --ink-2:#5d5043;          /* muted brown text */
      --ink-3:#8a7d6e;          /* faint */
      --line:#ddd1bd;           /* hairline on paper */
      --vermillion:#d4391a;     /* commerce accent */
      --vermillion-d:#b32f13;   /* darker for text-on-paper */
      --money:#1c6b46;          /* ledger green */
      --ochre:#c8881f;          /* warm detail */
      --shadow:30px 30px 0 rgba(36,26,18,0.06);

      font-family:'Hanken Grotesk',-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;
      color:var(--ink); background:var(--paper); overflow-x:hidden;
      /* paper grain */
      background-image:
        radial-gradient(circle at 18% 12%, rgba(212,57,26,0.05), transparent 38%),
        radial-gradient(circle at 88% 0%, rgba(28,107,70,0.05), transparent 34%);
    }
    .lp *{ box-sizing:border-box; }
    .lp a{ cursor:pointer; text-decoration:none; }

    /* ====== BUTTONS ====== */
    .btn{ display:inline-flex; align-items:center; gap:8px; font-family:inherit;
      font-weight:700; font-size:15px; line-height:1; border-radius:2px; border:2px solid var(--ink);
      padding:13px 20px; transition:transform .18s cubic-bezier(.2,.8,.2,1), box-shadow .18s, background .18s; }
    .btn mat-icon{ font-size:18px; width:18px; height:18px; }
    .btn-solid{ background:var(--vermillion); color:#fff; box-shadow:4px 4px 0 var(--ink); }
    .btn-solid:hover{ transform:translate(-2px,-2px); box-shadow:6px 6px 0 var(--ink); }
    .btn-outline{ background:transparent; color:var(--ink); box-shadow:4px 4px 0 transparent; }
    .btn-outline:hover{ background:var(--ink); color:var(--paper); }
    .btn-lg{ padding:16px 26px; font-size:16px; }

    /* ====== BRAND ====== */
    .brand{ display:inline-flex; align-items:center; gap:11px; color:var(--ink); }
    .brand-mark{ display:grid; place-items:center; width:38px; height:38px; border-radius:3px;
      background:var(--vermillion); border:2px solid var(--ink); box-shadow:3px 3px 0 var(--ink); }
    .brand-mark mat-icon{ color:#fff; font-size:21px; width:21px; height:21px; }
    .brand-name{ font-family:'Fraunces',serif; font-weight:800; font-size:21px; letter-spacing:-.4px; }
    .brand-name em{ font-style:normal; color:var(--vermillion-d); }

    /* ====== TICKER ====== */
    .ticker{ background:var(--ink); color:var(--paper); overflow:hidden; border-bottom:2px solid var(--ink); }
    .ticker-track{ display:flex; width:max-content; animation:ticker 38s linear infinite; }
    .ticker-run{ display:inline-flex; align-items:center; gap:18px; padding:8px 18px; white-space:nowrap;
      font-family:'Spline Sans Mono',monospace; font-size:12.5px; letter-spacing:1px; text-transform:uppercase; }
    .ticker-run i{ color:var(--vermillion); font-style:normal; }
    @keyframes ticker{ to{ transform:translateX(-50%); } }

    /* ====== NAV ====== */
    .nav{ position:sticky; top:0; z-index:50; background:rgba(243,237,225,0.82);
      backdrop-filter:blur(10px); -webkit-backdrop-filter:blur(10px); border-bottom:2px solid var(--ink); }
    .nav-inner{ max-width:1200px; margin:0 auto; padding:14px 28px; display:flex; align-items:center; gap:26px; }
    .nav-links{ display:flex; gap:28px; margin-left:14px; }
    .nav-links a{ color:var(--ink-2); font-weight:600; font-size:15px; position:relative; }
    .nav-links a::after{ content:''; position:absolute; left:0; bottom:-5px; width:0; height:2px;
      background:var(--vermillion); transition:width .22s; }
    .nav-links a:hover{ color:var(--ink); }
    .nav-links a:hover::after{ width:100%; }
    .nav-cta{ margin-left:auto; display:flex; align-items:center; gap:18px; }
    .nav-cta .ghost{ color:var(--ink); font-weight:700; font-size:15px; }
    .nav-cta .btn{ padding:10px 16px; font-size:14px; }

    /* ====== HERO ====== */
    .hero{ position:relative; }
    .hero-inner{ max-width:1200px; margin:0 auto; padding:74px 28px 88px;
      display:grid; grid-template-columns:1.08fr 0.92fr; gap:60px; align-items:center; }
    .eyebrow{ display:inline-flex; align-items:center; gap:9px; font-family:'Spline Sans Mono',monospace;
      font-size:12.5px; font-weight:600; letter-spacing:.5px; text-transform:uppercase; color:var(--vermillion-d);
      background:var(--surface); border:2px solid var(--ink); border-radius:100px; padding:7px 15px;
      box-shadow:3px 3px 0 var(--ink); }
    .blip{ width:9px; height:9px; border-radius:50%; background:var(--money);
      box-shadow:0 0 0 4px rgba(28,107,70,0.18); animation:pulse 2s ease-in-out infinite; }
    @keyframes pulse{ 50%{ box-shadow:0 0 0 7px rgba(28,107,70,0); } }

    .hero h1{ font-family:'Fraunces',serif; font-weight:800; font-size:clamp(40px,6vw,76px);
      line-height:0.98; letter-spacing:-2px; margin:26px 0 22px; color:var(--ink); }
    .mark-word{ position:relative; color:var(--vermillion-d); white-space:nowrap; }
    .mark-word::after{ content:''; position:absolute; left:-2px; right:-2px; bottom:6px; height:14px; z-index:-1;
      background:var(--ochre); opacity:.32; transform:rotate(-1deg); }
    .serif-stamp{ color:var(--money); }
    .lede{ font-size:19px; line-height:1.6; color:var(--ink-2); max-width:500px; margin:0 0 32px; }
    .hero-actions{ display:flex; gap:16px; flex-wrap:wrap; }

    .hero-stats{ list-style:none; display:flex; gap:34px; margin:38px 0 0; padding:30px 0 0;
      border-top:2px dashed var(--line); }
    .hero-stats strong{ display:block; font-family:'Fraunces',serif; font-weight:800; font-size:30px;
      letter-spacing:-1px; color:var(--ink); }
    .hero-stats span{ font-family:'Spline Sans Mono',monospace; font-size:11.5px; text-transform:uppercase;
      letter-spacing:.6px; color:var(--ink-3); }

    /* ====== RECEIPT MOCK ====== */
    .hero-visual{ position:relative; display:flex; justify-content:center; }
    .receipt{ position:relative; width:340px; max-width:100%; background:var(--surface);
      padding:26px 26px 18px; transform:rotate(1.4deg); filter:drop-shadow(14px 18px 0 rgba(36,26,18,0.12));
      /* perforated top & bottom edges */
      -webkit-mask:
        radial-gradient(9px at 9px 0, transparent 98%, #000) repeat-x 0 0/22px 9px,
        radial-gradient(9px at 9px 100%, transparent 98%, #000) repeat-x 0 100%/22px 9px,
        linear-gradient(#000,#000) no-repeat 0 9px/100% calc(100% - 18px);
      mask:
        radial-gradient(9px at 9px 0, transparent 98%, #000) repeat-x 0 0/22px 9px,
        radial-gradient(9px at 9px 100%, transparent 98%, #000) repeat-x 0 100%/22px 9px,
        linear-gradient(#000,#000) no-repeat 0 9px/100% calc(100% - 18px);
      transition:transform .4s cubic-bezier(.2,.8,.2,1); }
    .receipt:hover{ transform:rotate(0deg); }
    .receipt-head{ text-align:center; padding:6px 0 12px; }
    .r-store{ display:block; font-family:'Fraunces',serif; font-weight:900; font-size:24px; letter-spacing:1px; }
    .r-sub{ font-family:'Spline Sans Mono',monospace; font-size:10.5px; letter-spacing:1.5px; color:var(--ink-3); }
    .r-rule{ height:0; border-top:2px solid var(--ink); margin:10px 0; }
    .r-rule.dashed{ border-top:2px dashed var(--line); }
    .r-line,.r-total,.r-pay{ display:flex; justify-content:space-between; align-items:baseline;
      font-family:'Spline Sans Mono',monospace; font-size:13.5px; padding:7px 0; color:var(--ink); }
    .r-line span{ color:var(--ink-2); }
    .r-line b{ font-weight:600; }
    .r-total{ font-size:15px; font-weight:700; }
    .r-total b{ font-family:'Fraunces',serif; font-size:26px; font-weight:800; }
    .r-pay{ font-size:12px; color:var(--money); padding-top:2px; }
    .r-pay b{ color:var(--money); }
    .r-finish{ width:100%; border:2px solid var(--ink); background:var(--money); color:#fff;
      font-family:'Hanken Grotesk',sans-serif; font-weight:700; font-size:14.5px; padding:13px;
      display:flex; align-items:center; justify-content:center; gap:8px; cursor:default; margin:8px 0 16px;
      box-shadow:3px 3px 0 var(--ink); }
    .r-finish mat-icon{ font-size:18px; width:18px; height:18px; }
    .r-barcode{ height:46px; margin:4px 6px 10px;
      background:repeating-linear-gradient(90deg, var(--ink) 0 2px, transparent 2px 4px,
        var(--ink) 4px 7px, transparent 7px 9px, var(--ink) 9px 10px, transparent 10px 14px); }
    .r-foot{ text-align:center; font-family:'Spline Sans Mono',monospace; font-size:10.5px;
      letter-spacing:2px; color:var(--ink-3); }

    .stamp{ position:absolute; bottom:6px; left:-10px; background:var(--vermillion); color:#fff;
      border:2px solid var(--ink); padding:12px 16px; transform:rotate(-5deg); box-shadow:4px 4px 0 var(--ink);
      animation:float 4.5s ease-in-out infinite; }
    .stamp-num{ font-family:'Fraunces',serif; font-weight:800; font-size:21px; letter-spacing:-.5px; }
    .stamp-label{ font-family:'Spline Sans Mono',monospace; font-size:10px; text-transform:uppercase;
      letter-spacing:1px; opacity:.92; }
    @keyframes float{ 50%{ transform:rotate(-5deg) translateY(-9px); } }

    /* ====== STAT STRIP ====== */
    .strip{ background:var(--ink); color:var(--paper); border-top:2px solid var(--ink); border-bottom:2px solid var(--ink); }
    .strip-inner{ max-width:1200px; margin:0 auto; padding:26px 28px; display:grid;
      grid-template-columns:repeat(4,1fr); gap:24px; }
    .strip-item{ display:flex; flex-direction:column; gap:5px; position:relative; padding-left:18px; }
    .strip-item::before{ content:'✺'; position:absolute; left:0; top:1px; color:var(--vermillion); }
    .strip-item b{ font-family:'Fraunces',serif; font-weight:700; font-size:18px; }
    .strip-item span{ font-family:'Spline Sans Mono',monospace; font-size:11px; text-transform:uppercase;
      letter-spacing:.6px; color:#b3a795; }

    /* ====== SECTION HEAD ====== */
    .section-head{ max-width:1200px; margin:0 auto; padding:0 28px; }
    .kicker{ font-family:'Spline Sans Mono',monospace; font-size:13px; font-weight:600; letter-spacing:1px;
      text-transform:uppercase; color:var(--vermillion-d); }
    .section-head h2{ font-family:'Fraunces',serif; font-weight:800; font-size:clamp(32px,4.5vw,52px);
      line-height:1; letter-spacing:-1.5px; margin:14px 0 14px; }
    .section-head p{ font-size:18px; color:var(--ink-2); max-width:520px; margin:0; }

    /* ====== FEATURES ====== */
    .features{ padding:96px 0; }
    .feature-grid{ max-width:1200px; margin:46px auto 0; padding:0 28px; display:grid;
      grid-template-columns:repeat(3,1fr); gap:0; border-top:2px solid var(--ink); border-left:2px solid var(--ink); }
    .feature{ position:relative; padding:34px 30px 38px; border-right:2px solid var(--ink);
      border-bottom:2px solid var(--ink); background:var(--surface); transition:background .2s; }
    .feature:hover{ background:var(--paper-2); }
    .feature-num{ position:absolute; top:18px; right:20px; font-family:'Spline Sans Mono',monospace;
      font-size:13px; color:var(--ink-3); }
    .feature-ico{ display:grid; place-items:center; width:52px; height:52px; border-radius:3px;
      background:var(--vermillion); border:2px solid var(--ink); box-shadow:3px 3px 0 var(--ink); margin-bottom:22px; }
    .feature:nth-child(3n+2) .feature-ico{ background:var(--money); }
    .feature:nth-child(3n) .feature-ico{ background:var(--ochre); }
    .feature-ico mat-icon{ color:#fff; font-size:26px; width:26px; height:26px; }
    .feature h3{ font-family:'Fraunces',serif; font-weight:700; font-size:21px; letter-spacing:-.4px; margin:0 0 9px; }
    .feature p{ font-size:15px; line-height:1.6; color:var(--ink-2); margin:0; }

    /* ====== PRICING ====== */
    .pricing{ padding:30px 0 110px; }
    .plan-grid{ max-width:1200px; margin:46px auto 0; padding:0 28px; display:grid;
      grid-template-columns:repeat(3,1fr); gap:26px; align-items:stretch; }
    .plan{ position:relative; background:var(--surface); border:2px solid var(--ink); border-radius:3px;
      padding:34px 30px; display:flex; flex-direction:column; box-shadow:6px 6px 0 var(--ink);
      transition:transform .2s, box-shadow .2s; }
    .plan:hover{ transform:translate(-3px,-3px); box-shadow:9px 9px 0 var(--ink); }
    .plan.featured{ background:var(--ink); color:var(--paper); }
    .plan.featured h3{ color:var(--paper); }
    .plan-badge{ position:absolute; top:-15px; left:30px; background:var(--vermillion); color:#fff;
      font-family:'Spline Sans Mono',monospace; font-size:11px; font-weight:600; letter-spacing:.8px;
      text-transform:uppercase; padding:6px 13px; border:2px solid var(--ink); box-shadow:3px 3px 0 rgba(36,26,18,.4); }
    .plan h3{ font-family:'Fraunces',serif; font-weight:700; font-size:23px; margin:0 0 8px; letter-spacing:-.4px; }
    .plan-price{ display:flex; align-items:baseline; gap:3px; font-family:'Fraunces',serif; }
    .plan-price .cur{ font-size:20px; font-weight:600; color:var(--vermillion-d); }
    .plan.featured .plan-price .cur{ color:var(--ochre); }
    .plan-price .num{ font-size:60px; font-weight:800; letter-spacing:-3px; }
    .plan-price .per{ font-family:'Spline Sans Mono',monospace; font-size:13px; color:var(--ink-3); }
    .plan-rule{ border-top:2px dashed var(--line); margin:22px 0; }
    .plan.featured .plan-rule{ border-color:rgba(243,237,225,.25); }
    .plan ul{ list-style:none; padding:0; margin:0 0 28px; display:flex; flex-direction:column; gap:13px; flex:1; }
    .plan li{ display:flex; align-items:center; gap:11px; font-size:15px; color:var(--ink-2); }
    .plan.featured li{ color:#d9cfbf; }
    .plan li mat-icon{ color:var(--money); font-size:20px; width:20px; height:20px; flex-shrink:0; }
    .plan.featured li mat-icon{ color:#3ec98a; }
    .plan-cta{ justify-content:center; }
    .plan.featured .plan-cta.btn-solid{ background:var(--vermillion); border-color:var(--paper);
      box-shadow:4px 4px 0 var(--vermillion-d); }
    .plan .plan-cta.btn-outline:hover{ background:var(--ink); color:var(--paper); }

    /* ====== CTA BAND ====== */
    .cta-band{ padding:0 28px 100px; }
    .cta-inner{ max-width:1080px; margin:0 auto; text-align:center; color:#fff; padding:72px 32px;
      border:2px solid var(--ink); border-radius:4px; position:relative; overflow:hidden;
      background:var(--vermillion); box-shadow:14px 14px 0 var(--ink); }
    .cta-inner::before{ content:''; position:absolute; inset:0; opacity:.5; pointer-events:none;
      background:radial-gradient(420px 220px at 50% -30%, rgba(255,255,255,.4), transparent); }
    .cta-kicker{ position:relative; font-family:'Spline Sans Mono',monospace; font-size:13px; letter-spacing:2px;
      text-transform:uppercase; color:#ffe6db; }
    .cta-inner h2{ position:relative; font-family:'Fraunces',serif; font-weight:800; font-size:clamp(30px,4.5vw,48px);
      letter-spacing:-1.5px; margin:16px 0 12px; }
    .cta-inner p{ position:relative; font-size:18px; color:#ffe1d7; margin:0 0 30px; }
    .cta-btn{ position:relative; background:var(--paper); color:var(--ink); border-color:var(--ink);
      box-shadow:5px 5px 0 var(--ink); }
    .cta-btn:hover{ transform:translate(-2px,-2px); box-shadow:7px 7px 0 var(--ink); }

    /* ====== FOOTER ====== */
    .footer{ background:var(--ink); color:#b3a795; padding:64px 28px 30px; border-top:2px solid var(--ink); }
    .footer-inner{ max-width:1200px; margin:0 auto; display:grid; grid-template-columns:1.7fr 1fr 1fr 1fr; gap:36px; }
    .footer-brand .brand{ color:var(--paper); margin-bottom:14px; }
    .footer-brand .brand-name em{ color:var(--vermillion); }
    .footer-brand p{ margin:0; font-size:14.5px; max-width:280px; line-height:1.6; }
    .footer-col h4{ font-family:'Spline Sans Mono',monospace; color:var(--paper); font-size:12.5px;
      text-transform:uppercase; letter-spacing:1px; margin:0 0 18px; }
    .footer-col a{ display:block; color:#b3a795; font-size:14.5px; margin-bottom:11px; transition:color .18s; }
    .footer-col a:hover{ color:var(--vermillion); }
    .footer-bottom{ max-width:1200px; margin:46px auto 0; padding-top:24px; display:flex; justify-content:space-between;
      gap:12px; flex-wrap:wrap; border-top:2px solid rgba(243,237,225,.12);
      font-family:'Spline Sans Mono',monospace; font-size:12.5px; color:#8a7d6e; }

    /* ====== REVEAL ANIMATION ====== */
    .reveal{ opacity:0; transform:translateY(22px); animation:reveal .7s cubic-bezier(.2,.8,.2,1) forwards; }
    @keyframes reveal{ to{ opacity:1; transform:none; } }

    /* ====== RESPONSIVE ====== */
    @media (max-width:980px){
      .hero-inner{ grid-template-columns:1fr; gap:56px; padding:54px 24px 72px; }
      .feature-grid{ grid-template-columns:1fr 1fr; }
      .strip-inner{ grid-template-columns:1fr 1fr; gap:20px; }
      .plan-grid{ grid-template-columns:1fr; max-width:480px; }
      .footer-inner{ grid-template-columns:1fr 1fr; }
    }
    @media (max-width:640px){
      .nav-links{ display:none; }
      .nav-cta .ghost{ display:none; }
      .hero h1{ letter-spacing:-1px; }
      .hero-stats{ gap:22px; }
      .feature-grid{ grid-template-columns:1fr; }
      .strip-inner{ grid-template-columns:1fr; }
      .feature{ border-right:2px solid var(--ink); }
      .footer-inner{ grid-template-columns:1fr 1fr; }
      .footer-bottom{ justify-content:center; text-align:center; }
      .cta-inner{ box-shadow:8px 8px 0 var(--ink); padding:54px 22px; }
    }

    /* ====== REDUCED MOTION ====== */
    @media (prefers-reduced-motion:reduce){
      .ticker-track, .stamp, .blip{ animation:none; }
      .reveal{ opacity:1; transform:none; animation:none; }
      .btn, .plan, .receipt, .cta-btn{ transition:none; }
    }
  `]
})
export class LandingComponent {
  year = new Date().getFullYear();

  features = [
    { icon: 'storefront', title: 'Frente de caixa ágil', desc: 'Venda por código de barras, múltiplos pagamentos e troco automático.' },
    { icon: 'inventory_2', title: 'Controle de estoque', desc: 'Baixa automática a cada venda e alerta de produtos em falta.' },
    { icon: 'account_balance_wallet', title: 'Crediário e devedores', desc: 'Registre fiados e acompanhe quem precisa pagar, sem caderninho.' },
    { icon: 'bar_chart', title: 'Relatórios de vendas', desc: 'Veja faturamento por dia e período e tome decisões com dados.' },
    { icon: 'groups', title: 'Vários operadores', desc: 'Cada vendedor com seu acesso; o admin gerencia toda a equipe.' },
    { icon: 'cloud_done', title: '100% na nuvem', desc: 'Acesse de qualquer lugar, com segurança e backup automático.' },
  ];

  plans = [
    { name: 'Starter', price: 99, featured: false, items: ['Até 200 produtos', 'Até 300 vendas/mês', '1 operador', 'Relatórios essenciais'] },
    { name: 'Pro', price: 199, featured: true, items: ['Até 500 produtos', 'Até 1.000 vendas/mês', '3 operadores', 'Crediário e devedores', 'Suporte prioritário'] },
    { name: 'Enterprise', price: 349, featured: false, items: ['Até 2.000 produtos', 'Até 5.000 vendas/mês', '10 operadores', 'Todos os recursos'] },
  ];

  pad(n: number): string {
    return n.toString().padStart(2, '0');
  }

  scrollTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
