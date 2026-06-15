import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { BillingService, InvoiceDTO, SubscriptionDTO } from './billing.service';

const PLAN_LABELS: Record<string, string> = {
  starter: 'Starter',
  pro: 'Pro',
  enterprise: 'Enterprise',
  free: 'Cortesia',
};

const PLAN_PRICE: Record<string, number> = {
  starter: 99,
  pro: 199,
  enterprise: 349,
  free: 0,
};

const SUB_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendente',
  ACTIVE: 'Ativa',
  PAST_DUE: 'Pagamento atrasado',
  CANCELLED: 'Cancelada',
};

const INVOICE_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendente',
  PAID: 'Paga',
  OVERDUE: 'Vencida',
  CANCELLED: 'Cancelada',
};

@Component({
  selector: 'app-billing',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="billing">
      <header class="billing-head">
        <div>
          <a routerLink="/pdv" class="back">← Voltar ao PDV</a>
          <h1>Minha Assinatura</h1>
        </div>
        <button mat-stroked-button (click)="load()"><mat-icon>refresh</mat-icon> Atualizar</button>
      </header>

      <div *ngIf="loading()" class="loading"><mat-spinner diameter="36"></mat-spinner></div>

      <ng-container *ngIf="!loading()">
        <div *ngIf="needsAction()" class="alert" [class.alert-danger]="isBlocked()">
          <mat-icon>{{ isBlocked() ? 'error' : 'info' }}</mat-icon>
          <div>
            <strong>{{ isBlocked() ? 'Assinatura suspensa' : 'Ative sua assinatura' }}</strong>
            <p>{{ alertMessage() }}</p>
          </div>
        </div>

        <section class="card sub-card">
          <div class="sub-main">
            <div>
              <span class="plan-name">Plano {{ planLabel() }}</span>
              <span class="plan-price" *ngIf="!isFree()">R$ {{ planPrice() }}<small>/mês</small></span>
              <span class="plan-price" *ngIf="isFree()">Grátis<small>vitalício</small></span>
            </div>
            <span class="badge" [class]="'st-' + (subscription()?.status || 'NONE')">
              {{ statusLabel() }}
            </span>
          </div>

          <div class="sub-meta" *ngIf="subscription() as s">
            <div class="meta-item">
              <span class="meta-lbl">Próxima cobrança</span>
              <span class="meta-val">{{ formatDate(s.nextBillingDate) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-lbl">Último pagamento</span>
              <span class="meta-val">{{ formatDate(s.lastPaymentDate) }}</span>
            </div>
            <div class="meta-item" *ngIf="s.failedPaymentCount > 0">
              <span class="meta-lbl">Tentativas falhas</span>
              <span class="meta-val danger">{{ s.failedPaymentCount }}</span>
            </div>
          </div>

          <ng-container *ngIf="!isFree()">
            <button mat-flat-button color="primary" class="pay-btn" (click)="pay()" [disabled]="paying()">
              <mat-spinner *ngIf="paying()" diameter="20"></mat-spinner>
              <mat-icon *ngIf="!paying()">credit_card</mat-icon>
              {{ payButtonLabel() }}
            </button>
            <p class="pay-hint">Você será redirecionado ao Stripe para concluir o pagamento com segurança.</p>
            <button mat-stroked-button class="manage-btn" *ngIf="canManage()" (click)="managePortal()" [disabled]="managing()">
              <mat-spinner *ngIf="managing()" diameter="20"></mat-spinner>
              <mat-icon *ngIf="!managing()">settings</mat-icon>
              Gerenciar assinatura
            </button>
          </ng-container>
          <p class="free-note" *ngIf="isFree()">
            <mat-icon>verified</mat-icon> Plano cortesia vitalício — acesso liberado, sem cobrança.
          </p>
        </section>

        <section class="card">
          <h2>Faturas</h2>
          <table class="invoice-table" *ngIf="invoices().length > 0">
            <thead>
              <tr><th>Descrição</th><th>Vencimento</th><th>Valor</th><th>Status</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let inv of invoices()">
                <td>{{ inv.description }}</td>
                <td>{{ formatDate(inv.dueDate) }}</td>
                <td>R$ {{ inv.amount.toFixed(2) }}</td>
                <td><span class="badge inv-{{ inv.status }}">{{ invoiceStatusLabel(inv.status) }}</span></td>
              </tr>
            </tbody>
          </table>
          <p *ngIf="invoices().length === 0" class="empty">Nenhuma fatura emitida ainda.</p>
        </section>
      </ng-container>
    </div>
  `,
  styles: [`
    .billing { max-width:860px; margin:0 auto; padding:32px 24px; }
    .billing-head { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:24px; }
    .back { color:#1976d2; text-decoration:none; font-size:13px; }
    h1 { margin:6px 0 0; font-size:26px; }
    h2 { font-size:17px; margin:0 0 16px; }
    .loading { display:flex; justify-content:center; padding:60px; }
    .card { background:#fff; border-radius:12px; padding:24px; box-shadow:0 2px 8px rgba(15,23,42,0.05); margin-bottom:20px; }
    .alert { display:flex; gap:12px; align-items:flex-start; background:#eff6ff; color:#1e40af; border-radius:12px; padding:16px 20px; margin-bottom:20px; }
    .alert p { margin:4px 0 0; font-size:13.5px; }
    .alert-danger { background:#fee2e2; color:#b91c1c; }
    .sub-main { display:flex; justify-content:space-between; align-items:center; gap:16px; }
    .plan-name { display:block; font-size:13px; color:#64748b; }
    .plan-price { font-size:30px; font-weight:700; }
    .plan-price small { font-size:14px; font-weight:500; color:#94a3b8; }
    .sub-meta { display:flex; gap:32px; flex-wrap:wrap; margin:24px 0; padding:20px 0; border-top:1px solid #f1f5f9; border-bottom:1px solid #f1f5f9; }
    .meta-item { display:flex; flex-direction:column; gap:4px; }
    .meta-lbl { font-size:12px; color:#94a3b8; }
    .meta-val { font-size:15px; font-weight:600; }
    .meta-val.danger { color:#b91c1c; }
    .badge { padding:4px 12px; border-radius:100px; font-size:12px; font-weight:600; white-space:nowrap; }
    .st-ACTIVE { background:#dcfce7; color:#15803d; }
    .st-PENDING { background:#fef9c3; color:#a16207; }
    .st-PAST_DUE { background:#ffedd5; color:#c2410c; }
    .st-CANCELLED, .st-NONE { background:#e2e8f0; color:#475569; }
    .pay-btn { width:100%; height:48px; font-weight:600; margin-top:20px; display:flex; align-items:center; justify-content:center; gap:8px; }
    .pay-hint { text-align:center; font-size:12px; color:#94a3b8; margin:10px 0 0; }
    .manage-btn { width:100%; height:44px; font-weight:600; margin-top:12px; display:flex; align-items:center; justify-content:center; gap:8px; }
    .free-note { display:flex; align-items:center; justify-content:center; gap:8px; margin:20px 0 0; padding:14px; background:#dcfce7; color:#15803d; border-radius:10px; font-size:13.5px; font-weight:500; }
    .invoice-table { width:100%; border-collapse:collapse; }
    .invoice-table th, .invoice-table td { text-align:left; padding:10px 12px; border-bottom:1px solid #f1f5f9; font-size:13.5px; }
    .invoice-table th { color:#64748b; font-weight:600; font-size:12px; text-transform:uppercase; }
    .inv-PAID { background:#dcfce7; color:#15803d; }
    .inv-PENDING { background:#fef9c3; color:#a16207; }
    .inv-OVERDUE { background:#fee2e2; color:#b91c1c; }
    .inv-CANCELLED { background:#e2e8f0; color:#475569; }
    .empty { color:#94a3b8; text-align:center; padding:24px; }
  `]
})
export class BillingComponent implements OnInit {
  private billingService = inject(BillingService);
  private snackBar = inject(MatSnackBar);

  subscription = signal<SubscriptionDTO | null>(null);
  invoices = signal<InvoiceDTO[]>([]);
  loading = signal(false);
  paying = signal(false);
  managing = signal(false);

  // Plano vindo da assinatura; quando ainda não há assinatura, cai no fallback do checkout.
  planTier = computed(() => this.subscription()?.planTier?.toLowerCase() ?? '');

  isFree = computed(() => this.planTier() === 'free');

  needsAction = computed(() => {
    if (this.isFree()) {
      return false;
    }
    const status = this.subscription()?.status;
    return !status || status !== 'ACTIVE';
  });

  isBlocked = computed(() => {
    const status = this.subscription()?.status;
    return status === 'PAST_DUE' || status === 'CANCELLED';
  });

  // Portal de autoatendimento só faz sentido com assinatura ativa já vinculada ao Stripe.
  canManage = computed(() => {
    const sub = this.subscription();
    return sub?.status === 'ACTIVE' && !!sub?.stripeCustomerId;
  });

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.billingService.getSubscription().subscribe({
      next: (sub) => {
        this.subscription.set(sub);
        this.loading.set(false);
      },
      error: (err: HttpErrorResponse) => {
        // 404 = tenant ainda sem assinatura; não é erro fatal.
        this.subscription.set(null);
        this.loading.set(false);
        if (err.status !== 404) {
          this.snackBar.open('Erro ao carregar assinatura.', 'OK', { duration: 4000 });
        }
      },
    });
    this.billingService.getInvoices().subscribe({
      next: (data) => this.invoices.set(data),
      error: () => this.invoices.set([]),
    });
  }

  pay(): void {
    this.paying.set(true);
    this.billingService.getCheckoutUrl().subscribe({
      next: ({ url }) => {
        window.location.href = url;
      },
      error: () => {
        this.paying.set(false);
        this.snackBar.open('Não foi possível gerar o link de pagamento.', 'OK', { duration: 4000 });
      },
    });
  }

  managePortal(): void {
    this.managing.set(true);
    this.billingService.getPortalUrl().subscribe({
      next: ({ url }) => {
        window.location.href = url;
      },
      error: () => {
        this.managing.set(false);
        this.snackBar.open('Não foi possível abrir a gestão de assinatura.', 'OK', { duration: 4000 });
      },
    });
  }

  planLabel(): string {
    return PLAN_LABELS[this.planTier()] ?? '—';
  }

  planPrice(): number {
    return PLAN_PRICE[this.planTier()] ?? 0;
  }

  statusLabel(): string {
    const status = this.subscription()?.status;
    return status ? (SUB_STATUS_LABEL[status] ?? status) : 'Sem assinatura';
  }

  invoiceStatusLabel(status: string): string {
    return INVOICE_STATUS_LABEL[status] ?? status;
  }

  payButtonLabel(): string {
    return this.isBlocked() ? 'Regularizar pagamento' : 'Assinar / Pagar agora';
  }

  alertMessage(): string {
    if (this.isBlocked()) {
      return 'Seu acesso ao sistema está suspenso por pagamento pendente. Regularize para reativar.';
    }
    return 'Conclua o pagamento para liberar todos os recursos da sua loja.';
  }

  formatDate(value: string | null): string {
    if (!value) {
      return '—';
    }
    const date = new Date(value);
    return isNaN(date.getTime()) ? '—' : date.toLocaleDateString('pt-BR');
  }
}
