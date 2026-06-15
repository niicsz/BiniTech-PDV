import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterLink, MatButtonModule, MatIconModule],
  template: `
    <div class="lp">
      <!-- NAVBAR -->
      <header class="nav">
        <div class="nav-inner">
          <a class="brand" (click)="scrollTo('top')">
            <span class="brand-logo"><mat-icon>point_of_sale</mat-icon></span>
            <span class="brand-name">BiniTech <strong>PDV</strong></span>
          </a>
          <nav class="nav-links">
            <a (click)="scrollTo('recursos')">Recursos</a>
            <a (click)="scrollTo('planos')">Planos</a>
            <a routerLink="/sobre-nos">Sobre</a>
          </nav>
          <div class="nav-cta">
            <a class="ghost" routerLink="/login">Entrar</a>
            <a mat-flat-button color="primary" routerLink="/signup">Começar grátis</a>
          </div>
        </div>
      </header>

      <!-- HERO -->
      <section class="hero" id="top">
        <div class="glow glow-1"></div>
        <div class="glow glow-2"></div>
        <div class="hero-inner">
          <div class="hero-copy">
            <span class="pill"><span class="dot"></span> Gestão multi-loja na nuvem</span>
            <h1>O ponto de venda que faz sua loja <span class="grad">vender mais</span>.</h1>
            <p>
              Frente de caixa, estoque, crediário e relatórios em uma plataforma simples,
              rápida e segura. Comece em minutos — sem instalar nada.
            </p>
            <div class="hero-actions">
              <a mat-flat-button color="primary" class="btn-lg" routerLink="/signup">
                Criar conta grátis <mat-icon>arrow_forward</mat-icon>
              </a>
              <a mat-stroked-button class="btn-lg ghost-btn" (click)="scrollTo('planos')">Ver planos</a>
            </div>
            <div class="trust">
              <mat-icon>verified</mat-icon> Pagamento seguro · Cancele quando quiser
            </div>
          </div>

          <!-- MOCK PDV -->
          <div class="hero-visual">
            <div class="mock">
              <div class="mock-bar">
                <span></span><span></span><span></span>
                <div class="mock-title">Frente de Caixa</div>
              </div>
              <div class="mock-body">
                <div class="mock-row">
                  <div class="mock-prod"><mat-icon>inventory_2</mat-icon> Arroz 5kg</div>
                  <div class="mock-price">R$ 27,90</div>
                </div>
                <div class="mock-row">
                  <div class="mock-prod"><mat-icon>inventory_2</mat-icon> Refrigerante 2L</div>
                  <div class="mock-price">R$ 8,50</div>
                </div>
                <div class="mock-row">
                  <div class="mock-prod"><mat-icon>inventory_2</mat-icon> Café 500g</div>
                  <div class="mock-price">R$ 19,90</div>
                </div>
                <div class="mock-total">
                  <span>Total</span>
                  <strong>R$ 56,30</strong>
                </div>
                <button class="mock-finish"><mat-icon>check_circle</mat-icon> Finalizar venda</button>
              </div>
            </div>
            <div class="float-card">
              <div class="fc-icon"><mat-icon>trending_up</mat-icon></div>
              <div>
                <div class="fc-value">+ R$ 4.280</div>
                <div class="fc-label">Vendas hoje</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- FEATURES -->
      <section class="features" id="recursos">
        <div class="section-head">
          <span class="eyebrow">Tudo em um só lugar</span>
          <h2>Recursos feitos para o seu balcão</h2>
          <p>Do caixa ao relatório, sem planilha e sem dor de cabeça.</p>
        </div>
        <div class="feature-grid">
          <div class="feature" *ngFor="let f of features">
            <div class="feature-ico" [style.background]="f.bg"><mat-icon>{{ f.icon }}</mat-icon></div>
            <h3>{{ f.title }}</h3>
            <p>{{ f.desc }}</p>
          </div>
        </div>
      </section>

      <!-- PRICING -->
      <section class="pricing" id="planos">
        <div class="section-head">
          <span class="eyebrow">Planos</span>
          <h2>Preço justo, sem surpresa</h2>
          <p>Escolha o tamanho da sua operação. Excedentes só se você crescer.</p>
        </div>
        <div class="plan-grid">
          <div class="plan" *ngFor="let p of plans" [class.featured]="p.featured">
            <div class="plan-badge" *ngIf="p.featured">Mais popular</div>
            <h3>{{ p.name }}</h3>
            <div class="plan-price"><span class="cur">R$</span>{{ p.price }}<span class="per">/mês</span></div>
            <ul>
              <li *ngFor="let item of p.items"><mat-icon>check</mat-icon> {{ item }}</li>
            </ul>
            <a mat-flat-button [color]="p.featured ? 'primary' : undefined"
               [class.outline]="!p.featured" class="plan-cta" routerLink="/signup">
              Assinar {{ p.name }}
            </a>
          </div>
        </div>
      </section>

      <!-- CTA BAND -->
      <section class="cta-band">
        <div class="cta-inner">
          <h2>Pronto para modernizar sua loja?</h2>
          <p>Crie sua conta agora e venda ainda hoje.</p>
          <a mat-flat-button class="btn-lg cta-btn" routerLink="/signup">
            Começar grátis <mat-icon>arrow_forward</mat-icon>
          </a>
        </div>
      </section>

      <!-- FOOTER -->
      <footer class="footer">
        <div class="footer-inner">
          <div class="footer-brand">
            <span class="brand-logo"><mat-icon>point_of_sale</mat-icon></span>
            <span class="brand-name">BiniTech <strong>PDV</strong></span>
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
        <div class="footer-bottom">© {{ year }} BiniTech PDV. Todos os direitos reservados.</div>
      </footer>
    </div>
  `,
  styles: [`
    :host { display:block; }
    .lp {
      --ink:#0f172a; --ink-2:#1e293b; --muted:#64748b;
      --brand:#6d28d9; --brand-2:#4f46e5; --accent:#a78bfa;
      font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
      color:var(--ink); background:#fff; overflow-x:hidden;
    }
    .lp a { cursor:pointer; }

    /* NAV */
    .nav { position:sticky; top:0; z-index:50; background:rgba(15,23,42,0.72);
      backdrop-filter:blur(14px); -webkit-backdrop-filter:blur(14px);
      border-bottom:1px solid rgba(255,255,255,0.08); }
    .nav-inner { max-width:1180px; margin:0 auto; padding:14px 24px; display:flex; align-items:center; gap:24px; }
    .brand { display:flex; align-items:center; gap:10px; color:#fff; text-decoration:none; font-size:18px; }
    .brand-logo { display:grid; place-items:center; width:36px; height:36px; border-radius:10px;
      background:linear-gradient(135deg,#7c3aed,#4f46e5); box-shadow:0 6px 16px rgba(79,70,229,0.45); }
    .brand-logo mat-icon { color:#fff; font-size:20px; width:20px; height:20px; }
    .brand-name { font-weight:600; letter-spacing:-0.3px; }
    .brand-name strong { color:var(--accent); }
    .nav-links { display:flex; gap:26px; margin-left:18px; }
    .nav-links a { color:#cbd5e1; text-decoration:none; font-size:14.5px; font-weight:500; transition:color .2s; }
    .nav-links a:hover { color:#fff; }
    .nav-cta { margin-left:auto; display:flex; align-items:center; gap:14px; }
    .nav-cta .ghost { color:#e2e8f0; text-decoration:none; font-size:14.5px; font-weight:600; }

    /* HERO */
    .hero { position:relative; background:radial-gradient(1200px 600px at 70% -10%, #312e81 0%, transparent 60%),
      linear-gradient(180deg,#0f172a 0%, #111827 100%); color:#e2e8f0; overflow:hidden; }
    .glow { position:absolute; border-radius:50%; filter:blur(90px); opacity:.55; pointer-events:none; }
    .glow-1 { width:420px; height:420px; background:#7c3aed; top:-120px; right:-60px; }
    .glow-2 { width:360px; height:360px; background:#2563eb; bottom:-160px; left:-80px; opacity:.4; }
    .hero-inner { position:relative; max-width:1180px; margin:0 auto; padding:80px 24px 96px;
      display:grid; grid-template-columns:1.05fr 0.95fr; gap:56px; align-items:center; }
    .pill { display:inline-flex; align-items:center; gap:8px; font-size:13px; font-weight:600;
      color:#ddd6fe; background:rgba(124,58,237,0.18); border:1px solid rgba(167,139,250,0.35);
      padding:6px 14px; border-radius:100px; }
    .pill .dot { width:8px; height:8px; border-radius:50%; background:#a78bfa; box-shadow:0 0 0 4px rgba(167,139,250,0.25); }
    .hero h1 { font-size:54px; line-height:1.05; letter-spacing:-1.5px; margin:22px 0 18px; color:#fff; font-weight:800; }
    .grad { background:linear-gradient(120deg,#a78bfa,#60a5fa); -webkit-background-clip:text; background-clip:text; color:transparent; }
    .hero-copy > p { font-size:18px; line-height:1.6; color:#94a3b8; max-width:520px; margin:0 0 30px; }
    .hero-actions { display:flex; gap:14px; flex-wrap:wrap; }
    .btn-lg { height:52px; padding:0 26px !important; font-size:15.5px !important; font-weight:600 !important;
      border-radius:12px !important; display:inline-flex; align-items:center; gap:8px; }
    .ghost-btn { color:#e2e8f0 !important; border-color:rgba(255,255,255,0.25) !important; }
    .trust { display:flex; align-items:center; gap:8px; margin-top:22px; font-size:13.5px; color:#94a3b8; }
    .trust mat-icon { color:#34d399; font-size:18px; width:18px; height:18px; }

    /* MOCK */
    .hero-visual { position:relative; }
    .mock { background:#fff; border-radius:18px; box-shadow:0 30px 60px rgba(2,6,23,0.55);
      overflow:hidden; transform:rotate(1.5deg); }
    .mock-bar { display:flex; align-items:center; gap:7px; padding:14px 18px; background:#f1f5f9; position:relative; }
    .mock-bar > span { width:11px; height:11px; border-radius:50%; background:#cbd5e1; }
    .mock-bar > span:first-child { background:#f87171; }
    .mock-bar > span:nth-child(2) { background:#fbbf24; }
    .mock-bar > span:nth-child(3) { background:#34d399; }
    .mock-title { position:absolute; left:50%; transform:translateX(-50%); font-size:13px; font-weight:600; color:#475569; }
    .mock-body { padding:20px; }
    .mock-row { display:flex; justify-content:space-between; align-items:center; padding:12px 14px;
      border-radius:10px; background:#f8fafc; margin-bottom:10px; }
    .mock-prod { display:flex; align-items:center; gap:10px; color:#1e293b; font-weight:500; font-size:14.5px; }
    .mock-prod mat-icon { color:#7c3aed; font-size:20px; width:20px; height:20px; }
    .mock-price { font-weight:700; color:#0f172a; }
    .mock-total { display:flex; justify-content:space-between; align-items:center; margin:16px 4px 14px; }
    .mock-total span { color:#64748b; font-size:14px; }
    .mock-total strong { font-size:24px; color:#0f172a; }
    .mock-finish { width:100%; border:0; cursor:default; color:#fff; font-weight:700; font-size:15px;
      padding:14px; border-radius:12px; display:flex; align-items:center; justify-content:center; gap:8px;
      background:linear-gradient(135deg,#16a34a,#22c55e); box-shadow:0 10px 20px rgba(34,197,94,0.35); }
    .float-card { position:absolute; bottom:-22px; left:-26px; display:flex; align-items:center; gap:12px;
      background:#fff; padding:14px 18px; border-radius:14px; box-shadow:0 20px 40px rgba(2,6,23,0.4); }
    .fc-icon { display:grid; place-items:center; width:42px; height:42px; border-radius:11px;
      background:linear-gradient(135deg,#7c3aed,#4f46e5); }
    .fc-icon mat-icon { color:#fff; }
    .fc-value { font-weight:800; color:#0f172a; font-size:17px; }
    .fc-label { font-size:12px; color:#64748b; }

    /* SECTIONS */
    .section-head { text-align:center; max-width:640px; margin:0 auto 48px; }
    .eyebrow { display:inline-block; font-size:13px; font-weight:700; letter-spacing:.6px; text-transform:uppercase; color:var(--brand-2); margin-bottom:12px; }
    .section-head h2 { font-size:36px; letter-spacing:-1px; margin:0 0 12px; color:var(--ink); font-weight:800; }
    .section-head p { font-size:17px; color:var(--muted); margin:0; }

    .features { padding:90px 24px; max-width:1180px; margin:0 auto; }
    .feature-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:22px; }
    .feature { background:#fff; border:1px solid #eef2f7; border-radius:16px; padding:28px;
      transition:transform .25s, box-shadow .25s; }
    .feature:hover { transform:translateY(-4px); box-shadow:0 18px 40px rgba(15,23,42,0.08); }
    .feature-ico { display:grid; place-items:center; width:52px; height:52px; border-radius:14px; margin-bottom:18px; }
    .feature-ico mat-icon { color:#fff; font-size:26px; width:26px; height:26px; }
    .feature h3 { font-size:19px; margin:0 0 8px; color:var(--ink); }
    .feature p { font-size:14.5px; line-height:1.6; color:var(--muted); margin:0; }

    /* PRICING */
    .pricing { padding:40px 24px 100px; max-width:1180px; margin:0 auto; }
    .plan-grid { display:grid; grid-template-columns:repeat(3,1fr); gap:24px; align-items:stretch; }
    .plan { position:relative; background:#fff; border:1px solid #e8edf3; border-radius:20px; padding:32px 28px;
      display:flex; flex-direction:column; }
    .plan.featured { border:2px solid var(--brand); box-shadow:0 24px 60px rgba(109,40,217,0.18); transform:translateY(-8px); }
    .plan-badge { position:absolute; top:-13px; left:50%; transform:translateX(-50%); background:linear-gradient(135deg,#7c3aed,#4f46e5);
      color:#fff; font-size:12px; font-weight:700; padding:6px 16px; border-radius:100px; box-shadow:0 8px 18px rgba(79,70,229,0.4); }
    .plan h3 { font-size:20px; margin:0 0 6px; color:var(--ink); }
    .plan-price { font-size:46px; font-weight:800; color:var(--ink); letter-spacing:-1.5px; margin-bottom:22px; }
    .plan-price .cur { font-size:20px; font-weight:700; vertical-align:super; margin-right:4px; color:var(--muted); }
    .plan-price .per { font-size:15px; font-weight:500; color:var(--muted); }
    .plan ul { list-style:none; padding:0; margin:0 0 26px; display:flex; flex-direction:column; gap:12px; flex:1; }
    .plan li { display:flex; align-items:center; gap:10px; color:#334155; font-size:14.5px; }
    .plan li mat-icon { color:#16a34a; font-size:19px; width:19px; height:19px; }
    .plan-cta { width:100%; height:48px; font-weight:700 !important; border-radius:12px !important; }
    .plan-cta.outline { border:1.5px solid #c7d2fe !important; color:var(--brand-2) !important; }

    /* CTA BAND */
    .cta-band { padding:0 24px 90px; }
    .cta-inner { max-width:1000px; margin:0 auto; text-align:center; color:#fff; border-radius:28px;
      padding:64px 32px; position:relative; overflow:hidden;
      background:radial-gradient(800px 300px at 50% -40%, rgba(167,139,250,0.5), transparent),
        linear-gradient(135deg,#6d28d9,#4f46e5); box-shadow:0 30px 70px rgba(79,70,229,0.4); }
    .cta-inner h2 { font-size:34px; margin:0 0 10px; letter-spacing:-0.8px; }
    .cta-inner p { font-size:17px; color:#e0e7ff; margin:0 0 26px; }
    .cta-btn { background:#fff !important; color:#4f46e5 !important; }

    /* FOOTER */
    .footer { background:#0f172a; color:#94a3b8; padding:56px 24px 28px; }
    .footer-inner { max-width:1180px; margin:0 auto; display:grid; grid-template-columns:1.6fr 1fr 1fr 1fr; gap:32px; }
    .footer-brand .brand-logo { margin-bottom:12px; }
    .footer-brand .brand-name { color:#fff; font-size:18px; }
    .footer-brand p { margin:14px 0 0; font-size:14px; max-width:280px; line-height:1.6; }
    .footer-col h4 { color:#e2e8f0; font-size:14px; margin:0 0 16px; }
    .footer-col a { display:block; color:#94a3b8; text-decoration:none; font-size:14px; margin-bottom:10px; transition:color .2s; }
    .footer-col a:hover { color:#fff; }
    .footer-bottom { max-width:1180px; margin:40px auto 0; padding-top:24px; border-top:1px solid rgba(255,255,255,0.08);
      font-size:13px; text-align:center; }

    /* RESPONSIVE */
    @media (max-width:980px) {
      .hero-inner { grid-template-columns:1fr; gap:64px; padding:56px 24px 80px; }
      .hero h1 { font-size:42px; }
      .feature-grid, .plan-grid { grid-template-columns:1fr 1fr; }
      .footer-inner { grid-template-columns:1fr 1fr; }
    }
    @media (max-width:680px) {
      .nav-links { display:none; }
      .hero h1 { font-size:34px; }
      .feature-grid, .plan-grid { grid-template-columns:1fr; }
      .plan.featured { transform:none; }
      .footer-inner { grid-template-columns:1fr 1fr; }
      .section-head h2 { font-size:28px; }
    }
  `]
})
export class LandingComponent {
  year = new Date().getFullYear();

  features = [
    { icon: 'storefront', title: 'Frente de caixa ágil', desc: 'Venda por código de barras, múltiplos pagamentos e troco automático.', bg: 'linear-gradient(135deg,#7c3aed,#4f46e5)' },
    { icon: 'inventory_2', title: 'Controle de estoque', desc: 'Baixa automática a cada venda e alerta de produtos em falta.', bg: 'linear-gradient(135deg,#0ea5e9,#2563eb)' },
    { icon: 'account_balance_wallet', title: 'Crediário e devedores', desc: 'Registre fiados e acompanhe quem precisa pagar, sem caderninho.', bg: 'linear-gradient(135deg,#f59e0b,#ef4444)' },
    { icon: 'bar_chart', title: 'Relatórios de vendas', desc: 'Veja faturamento por dia e período e tome decisões com dados.', bg: 'linear-gradient(135deg,#16a34a,#22c55e)' },
    { icon: 'groups', title: 'Vários operadores', desc: 'Cada vendedor com seu acesso; o admin gerencia toda a equipe.', bg: 'linear-gradient(135deg,#ec4899,#8b5cf6)' },
    { icon: 'cloud_done', title: '100% na nuvem', desc: 'Acesse de qualquer lugar, com segurança e backup automático.', bg: 'linear-gradient(135deg,#06b6d4,#3b82f6)' },
  ];

  plans = [
    { name: 'Starter', price: 99, featured: false, items: ['Até 200 produtos', 'Até 300 vendas/mês', '1 operador', 'Relatórios essenciais'] },
    { name: 'Pro', price: 199, featured: true, items: ['Até 500 produtos', 'Até 1.000 vendas/mês', '3 operadores', 'Crediário e devedores', 'Suporte prioritário'] },
    { name: 'Enterprise', price: 349, featured: false, items: ['Até 2.000 produtos', 'Até 5.000 vendas/mês', '10 operadores', 'Todos os recursos'] },
  ];

  scrollTo(id: string): void {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }
}
