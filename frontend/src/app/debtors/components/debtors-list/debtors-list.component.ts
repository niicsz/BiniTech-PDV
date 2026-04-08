import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { SaleService } from '../../../pos/services/sale.service';
import { SaleDTO } from '../../../shared/models/api.models';

interface DebtorGroup {
  customerName: string;
  customerPhone: string;
  sales: SaleDTO[];
  totalDebt: number;
  oldestDate: string;
  daysSinceOldest: number;
}

@Component({
  selector: 'app-debtors-list',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatButtonModule, MatIconModule, MatChipsModule
  ],
  template: `
    <div class="debtors-page">
      <div class="page-header">
        <div class="page-title-group">
          <mat-icon class="page-icon">account_balance_wallet</mat-icon>
          <div>
            <h2>Devedores - Crediário</h2>
            <p class="page-subtitle">Controle de vendas fiado pendentes de pagamento</p>
          </div>
        </div>
      </div>

      <div class="summary-cards">
        <div class="summary-card card-highlight">
          <div class="card-icon-wrapper card-icon-danger">
            <mat-icon>people</mat-icon>
          </div>
          <div class="card-content">
            <p class="card-label">Devedores</p>
            <p class="card-value">{{ debtorGroups.length }}</p>
          </div>
        </div>

        <div class="summary-card">
          <div class="card-icon-wrapper card-icon-warning">
            <mat-icon>receipt_long</mat-icon>
          </div>
          <div class="card-content">
            <p class="card-label">Vendas Pendentes</p>
            <p class="card-value">{{ allDebtSales.length }}</p>
          </div>
        </div>

        <div class="summary-card">
          <div class="card-icon-wrapper card-icon-primary">
            <mat-icon>attach_money</mat-icon>
          </div>
          <div class="card-content">
            <p class="card-label">Total a Receber</p>
            <p class="card-value">R$ {{ totalDebt | number:'1.2-2' }}</p>
          </div>
        </div>
      </div>

      <div class="debtors-list" *ngIf="debtorGroups.length > 0">
        <div class="debtor-card" *ngFor="let debtor of debtorGroups"
             [class.debtor-overdue]="debtor.daysSinceOldest >= 1">
          <div class="debtor-header">
            <div class="debtor-info">
              <div class="debtor-avatar">
                <mat-icon>person</mat-icon>
              </div>
              <div class="debtor-details">
                <h3 class="debtor-name">{{ debtor.customerName || 'Sem nome' }}</h3>
                <div class="debtor-phone" *ngIf="debtor.customerPhone">
                  <mat-icon class="phone-icon">phone</mat-icon>
                  <span>{{ debtor.customerPhone }}</span>
                  <a [href]="'https://wa.me/55' + cleanPhone(debtor.customerPhone)"
                     target="_blank" class="whatsapp-link" title="Enviar mensagem no WhatsApp">
                    <mat-icon class="whatsapp-icon">chat</mat-icon>
                  </a>
                </div>
              </div>
            </div>
            <div class="debtor-total">
              <span class="total-label">Total devido</span>
              <strong class="total-amount">R$ {{ debtor.totalDebt | number:'1.2-2' }}</strong>
            </div>
          </div>

          <div class="overdue-badge" *ngIf="debtor.daysSinceOldest >= 1">
            <mat-icon>warning</mat-icon>
            <span>{{ debtor.daysSinceOldest }} dia(s) sem pagar</span>
          </div>

          <div class="debtor-sales">
            <div class="sale-row" *ngFor="let sale of debtor.sales">
              <div class="sale-info">
                <span class="sale-id">#{{ sale.id?.substring(0, 8) }}</span>
                <span class="sale-date">{{ formatTimestamp(sale.timestamp) }}</span>
                <span class="sale-items">{{ sale.items?.length || 0 }} item(ns)</span>
              </div>
              <div class="sale-actions">
                <strong class="sale-amount">R$ {{ sale.totalAmount | number:'1.2-2' }}</strong>
                <button mat-flat-button color="primary" class="btn-mark-paid"
                        (click)="markAsPaid(sale)" [disabled]="sale.paid">
                  <mat-icon>check_circle</mat-icon>
                  {{ sale.paid ? 'Pago' : 'Marcar como Pago' }}
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="empty-state" *ngIf="debtorGroups.length === 0 && !loading">
        <mat-icon class="empty-icon">celebration</mat-icon>
        <h3>Nenhum devedor!</h3>
        <p>Não há vendas pendentes no crediário. Todas as contas estão em dia.</p>
      </div>

      <div class="loading-state" *ngIf="loading">
        <mat-icon class="loading-icon">hourglass_empty</mat-icon>
        <p>Carregando devedores...</p>
      </div>
    </div>
  `,
  styles: [`
    .debtors-page { padding: 0; }
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
    }
    .page-title-group {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .page-icon {
      font-size: 32px;
      height: 32px;
      width: 32px;
      color: var(--primary);
    }
    .page-title-group h2 {
      font-size: 22px;
      font-weight: 700;
      color: var(--text);
      margin: 0;
    }
    .page-subtitle {
      font-size: 13px;
      color: var(--text-secondary);
      margin: 2px 0 0;
    }

    .summary-cards {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 16px;
      margin-bottom: 24px;
    }
    .summary-card {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 20px;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius-md, 12px);
    }
    .card-highlight { border-left: 3px solid var(--danger, #ef5350); }
    .card-icon-wrapper {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 12px;
    }
    .card-icon-danger { background: rgba(239, 83, 80, 0.12); color: var(--danger, #ef5350); }
    .card-icon-warning { background: rgba(255, 167, 38, 0.12); color: var(--warning, #ffa726); }
    .card-icon-primary { background: rgba(66, 165, 245, 0.12); color: var(--primary, #42a5f5); }
    .card-label {
      font-size: 12px;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
      font-weight: 600;
      margin: 0;
    }
    .card-value {
      font-size: 24px;
      font-weight: 700;
      color: var(--text);
      margin: 4px 0 0;
      font-family: var(--font-mono, monospace);
    }

    .debtors-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    .debtor-card {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius-md, 12px);
      overflow: hidden;
    }
    .debtor-card.debtor-overdue {
      border-left: 3px solid var(--danger, #ef5350);
    }
    .debtor-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      background: var(--surface-alt, rgba(0,0,0,0.02));
    }
    .debtor-info {
      display: flex;
      align-items: center;
      gap: 14px;
    }
    .debtor-avatar {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 44px;
      height: 44px;
      border-radius: 50%;
      background: linear-gradient(135deg, rgba(66,165,245,0.15), rgba(25,118,210,0.15));
    }
    .debtor-avatar mat-icon {
      font-size: 24px;
      height: 24px;
      width: 24px;
      color: var(--primary);
    }
    .debtor-name {
      font-size: 16px;
      font-weight: 700;
      color: var(--text);
      margin: 0;
    }
    .debtor-phone {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 4px;
      font-size: 13px;
      color: var(--text-secondary);
    }
    .phone-icon {
      font-size: 16px;
      height: 16px;
      width: 16px;
    }
    .whatsapp-link {
      display: inline-flex;
      align-items: center;
      margin-left: 4px;
      color: #25d366;
      text-decoration: none;
      transition: transform 0.2s;
    }
    .whatsapp-link:hover { transform: scale(1.15); }
    .whatsapp-icon {
      font-size: 18px;
      height: 18px;
      width: 18px;
    }
    .debtor-total {
      text-align: right;
    }
    .total-label {
      font-size: 11px;
      color: var(--text-secondary);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .total-amount {
      display: block;
      font-size: 22px;
      color: var(--danger, #ef5350);
      font-family: var(--font-mono, monospace);
      margin-top: 2px;
    }

    .overdue-badge {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 24px;
      background: rgba(239, 83, 80, 0.08);
      color: var(--danger, #ef5350);
      font-size: 13px;
      font-weight: 600;
    }
    .overdue-badge mat-icon {
      font-size: 18px;
      height: 18px;
      width: 18px;
    }

    .debtor-sales {
      padding: 8px 24px 16px;
    }
    .sale-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid var(--border);
    }
    .sale-row:last-child { border-bottom: none; }
    .sale-info {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .sale-id {
      font-family: var(--font-mono, monospace);
      font-size: 13px;
      color: var(--text-secondary);
      font-weight: 600;
    }
    .sale-date {
      font-size: 13px;
      color: var(--text-secondary);
    }
    .sale-items {
      font-size: 12px;
      color: var(--text-secondary);
      background: var(--surface-alt, rgba(0,0,0,0.04));
      padding: 2px 8px;
      border-radius: 4px;
    }
    .sale-actions {
      display: flex;
      align-items: center;
      gap: 16px;
    }
    .sale-amount {
      font-size: 15px;
      font-family: var(--font-mono, monospace);
      color: var(--text);
    }
    .btn-mark-paid {
      font-size: 12px !important;
      padding: 0 12px !important;
      height: 34px !important;
      line-height: 34px !important;
    }
    .btn-mark-paid mat-icon {
      font-size: 16px;
      height: 16px;
      width: 16px;
      margin-right: 4px;
    }

    .empty-state, .loading-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-secondary);
    }
    .empty-icon, .loading-icon {
      font-size: 56px;
      height: 56px;
      width: 56px;
      color: var(--text-secondary);
      opacity: 0.4;
      margin-bottom: 16px;
    }
    .empty-state h3 {
      font-size: 18px;
      font-weight: 700;
      color: var(--text);
      margin: 0 0 8px;
    }
    .empty-state p, .loading-state p {
      font-size: 14px;
      margin: 0;
    }

    @media (max-width: 768px) {
      .summary-cards { grid-template-columns: 1fr; }
      .debtor-header { flex-direction: column; align-items: flex-start; gap: 12px; }
      .debtor-total { text-align: left; }
      .sale-row { flex-direction: column; align-items: flex-start; gap: 8px; }
      .sale-actions { width: 100%; justify-content: space-between; }
    }
  `]
})
export class DebtorsListComponent implements OnInit {
  allDebtSales: SaleDTO[] = [];
  debtorGroups: DebtorGroup[] = [];
  totalDebt = 0;
  loading = true;

  constructor(private saleService: SaleService) {}

  ngOnInit(): void {
    this.loadDebtors();
  }

  loadDebtors(): void {
    this.loading = true;
    console.info('[DebtorsList] Carregando devedores');
    this.saleService.listDebtors().subscribe({
      next: (sales) => {
        this.allDebtSales = sales;
        this.groupDebtors(sales);
        this.totalDebt = sales.reduce((sum, s) => sum + (s.totalAmount || 0), 0);
        this.loading = false;
        console.info('[DebtorsList] Devedores carregados:', this.debtorGroups.length, 'grupos, total:', this.totalDebt);
      },
      error: () => {
        console.error('[DebtorsList] Erro ao carregar devedores');
        this.allDebtSales = [];
        this.debtorGroups = [];
        this.loading = false;
      }
    });
  }

  groupDebtors(sales: SaleDTO[]): void {
    const grouped = new Map<string, SaleDTO[]>();

    for (const sale of sales) {
      const key = (sale.customerName || 'Desconhecido') + '|' + (sale.customerPhone || '');
      if (!grouped.has(key)) {
        grouped.set(key, []);
      }
      grouped.get(key)!.push(sale);
    }

    this.debtorGroups = Array.from(grouped.entries()).map(([key, groupSales]) => {
      const [name, phone] = key.split('|');
      const totalDebt = groupSales.reduce((sum, s) => sum + (s.totalAmount || 0), 0);
      const timestamps = groupSales
        .map(s => s.timestamp ? new Date(s.timestamp).getTime() : Date.now())
        .sort((a, b) => a - b);
      const oldestDate = new Date(timestamps[0]).toISOString();
      const daysSinceOldest = Math.floor((Date.now() - timestamps[0]) / (1000 * 60 * 60 * 24));

      return {
        customerName: name,
        customerPhone: phone,
        sales: groupSales,
        totalDebt,
        oldestDate,
        daysSinceOldest
      };
    }).sort((a, b) => b.daysSinceOldest - a.daysSinceOldest);
  }

  markAsPaid(sale: SaleDTO): void {
    if (!sale.id) return;
    console.info('[DebtorsList] Marcando venda como paga:', sale.id);
    this.saleService.markAsPaid(sale.id).subscribe({
      next: () => {
        console.info('[DebtorsList] Venda marcada como paga com sucesso:', sale.id);
        this.loadDebtors();
      },
      error: () => {
        console.error('[DebtorsList] Erro ao marcar venda como paga:', sale.id);
      }
    });
  }

  formatTimestamp(ts?: string): string {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleString('pt-BR');
  }

  cleanPhone(phone: string): string {
    return phone.replace(/\D/g, '');
  }
}








