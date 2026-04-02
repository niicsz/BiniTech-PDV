import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { PaymentDTO, PaymentMethodEnum } from '../../../shared/models/api.models';

@Component({
  selector: 'app-payment-modal',
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule
  ],
  template: `
    <div class="modal-overlay" (click)="cancel.emit()">
      <mat-card class="modal-content modal-payment" (click)="$event.stopPropagation()" appearance="outlined">
        <div class="modal-header">
          <mat-icon class="modal-header-icon">payment</mat-icon>
          <h3>Finalizar Venda</h3>
        </div>

        <mat-card-content>
          <div class="payment-total">
            <span class="total-label">Total da Venda</span>
            <strong class="total-amount">R$ {{ total | number:'1.2-2' }}</strong>
          </div>

          <div class="payments-list" *ngIf="payments.length > 0">
            <h4>Pagamentos adicionados</h4>
            <div class="payment-entry" *ngFor="let p of payments; let i = index">
              <mat-icon class="entry-icon">{{ getMethodIcon(p.method) }}</mat-icon>
              <span class="entry-label">{{ getPaymentLabel(p.method) }}</span>
              <strong class="entry-amount">R$ {{ p.amount | number:'1.2-2' }}</strong>
              <button mat-icon-button color="warn" (click)="removePayment(i)" class="entry-remove">
                <mat-icon>close</mat-icon>
              </button>
            </div>
            <div class="payments-summary">
              <div class="summary-line">
                <span>Total pago</span>
                <strong>R$ {{ getTotalPaid() | number:'1.2-2' }}</strong>
              </div>
              <div class="summary-line remaining" *ngIf="getRemaining() > 0">
                <span>Falta pagar</span>
                <strong class="text-danger">R$ {{ getRemaining() | number:'1.2-2' }}</strong>
              </div>
              <div class="summary-line change" *ngIf="getTotalChange() > 0">
                <span>Troco</span>
                <strong class="text-success">R$ {{ getTotalChange() | number:'1.2-2' }}</strong>
              </div>
            </div>
          </div>

          <div class="add-payment-section">
            <h4>{{ payments.length > 0 ? 'Adicionar outro pagamento' : 'Forma de pagamento' }}</h4>
            <div class="method-buttons">
              <button mat-stroked-button
                *ngFor="let m of availableMethods"
                [class.active]="selectedMethod === m.value"
                (click)="selectMethod(m.value)"
                class="method-btn">
                <mat-icon>{{ m.icon }}</mat-icon>
                {{ m.label }}
              </button>
            </div>

            <div class="amount-section" *ngIf="selectedMethod">
              <mat-form-field appearance="outline" class="full-width" *ngIf="selectedMethod === 'CASH'">
                <mat-label>Valor Recebido (R$)</mat-label>
                <input matInput #amountInput type="number" step="0.01" min="0"
                       class="amount-input" [(ngModel)]="currentAmount"
                       (keydown.enter)="addPayment()" autofocus />
              </mat-form-field>

              <mat-form-field appearance="outline" class="full-width" *ngIf="selectedMethod !== 'CASH'">
                <mat-label>Valor (R$)</mat-label>
                <input matInput #amountInputCard type="number" step="0.01" min="0.01"
                       class="amount-input" [(ngModel)]="currentAmount"
                       (keydown.enter)="addPayment()" autofocus />
              </mat-form-field>

              <button mat-flat-button color="primary" (click)="addPayment()" class="btn-add">
                <mat-icon>add</mat-icon>
                Adicionar Pagamento
              </button>
            </div>
          </div>
        </mat-card-content>

        <div class="error-banner" *ngIf="errorMessage">
          <mat-icon class="error-icon">error</mat-icon>
          <span>{{ errorMessage }}</span>
        </div>

        <mat-card-actions align="end">
          <button mat-stroked-button (click)="cancel.emit()">
            Cancelar (Esc)
          </button>
          <button mat-flat-button class="confirm-btn"
            (click)="confirmAll()"
            [disabled]="payments.length === 0 || getRemaining() > 0.001 || processing">
            <mat-icon>{{ processing ? 'hourglass_empty' : 'check' }}</mat-icon>
            {{ processing ? 'Processando...' : 'Confirmar Pagamento' }}
          </button>
        </mat-card-actions>
      </mat-card>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.6);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.2s ease;
      backdrop-filter: blur(4px);
    }
    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }
    .modal-content {
      animation: scaleIn 0.2s ease;
      max-height: 90vh;
      overflow-y: auto;
      padding: 24px;
      border-radius: var(--radius-md);
    }
    .modal-payment { width: 540px; max-width: 95vw; }
    @keyframes scaleIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }
    .modal-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;
    }
    .modal-header-icon {
      font-size: 28px;
      height: 28px;
      width: 28px;
      color: var(--primary);
    }
    .modal-header h3 {
      font-size: 20px;
      font-weight: 700;
      color: var(--text);
      margin: 0;
    }
    h4 {
      font-size: 12px;
      font-weight: 600;
      color: var(--text-secondary);
      margin-bottom: 10px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }
    .payment-total {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      background: var(--primary);
      color: #fff;
      border-radius: var(--radius);
      margin-bottom: 20px;
    }
    .total-label {
      font-size: 14px;
      font-weight: 500;
      opacity: 0.9;
    }
    .total-amount {
      font-size: 28px;
      font-family: var(--font-mono);
      letter-spacing: -0.5px;
    }
    .payments-list {
      margin-bottom: 20px;
      padding: 16px;
      background: var(--surface-alt);
      border-radius: var(--radius);
      border: 1px solid var(--border);
    }
    .payment-entry {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 0;
      border-bottom: 1px solid var(--border);
    }
    .entry-icon { font-size: 20px; height: 20px; width: 20px; color: var(--primary); }
    .entry-label { flex: 1; font-size: 14px; color: var(--text); }
    .entry-amount { font-size: 15px; color: var(--text); font-weight: 600; font-family: var(--font-mono); }
    .entry-remove { transform: scale(0.85); }
    .payments-summary {
      margin-top: 12px;
      padding-top: 12px;
      border-top: 2px solid var(--border);
    }
    .summary-line {
      display: flex;
      justify-content: space-between;
      font-size: 14px;
      padding: 4px 0;
      color: var(--text);
    }
    .summary-line.remaining { color: var(--danger); }
    .summary-line.change {
      padding: 10px 12px;
      background: var(--success-bg);
      border-radius: var(--radius);
      margin-top: 8px;
    }
    .text-danger { color: var(--danger); }
    .text-success { color: var(--success); }
    .method-buttons {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 8px;
      margin-bottom: 16px;
    }
    .method-btn {
      display: flex;
      align-items: center;
      gap: 8px;
      justify-content: center;
      height: 48px;
      font-weight: 600;
      border-radius: var(--radius) !important;
      transition: all 0.15s;
      &.active {
        background: var(--primary) !important;
        color: #fff !important;
        border-color: var(--primary) !important;
      }
    }
    .amount-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    .full-width { width: 100%; }
    .amount-input {
      font-size: 24px !important;
      font-weight: 700;
      text-align: right;
      font-family: var(--font-mono) !important;
    }
    .btn-add { align-self: flex-end; }
    .confirm-btn {
      background-color: var(--success) !important;
      color: #fff !important;
    }
    mat-card-actions {
      display: flex;
      gap: 10px;
      padding: 16px 0 0;
    }
    .add-payment-section {
      margin-bottom: 8px;
    }
    .error-banner {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 12px 16px;
      background: #fdecea;
      color: #b71c1c;
      border: 1px solid #f5c6cb;
      border-radius: var(--radius, 8px);
      margin: 12px 0 4px;
      font-size: 14px;
      font-weight: 500;
      animation: shakeIn 0.3s ease;
    }
    .error-banner .error-icon {
      font-size: 22px;
      height: 22px;
      width: 22px;
      color: #d32f2f;
      flex-shrink: 0;
    }
    @keyframes shakeIn {
      0%, 100% { transform: translateX(0); }
      20% { transform: translateX(-6px); }
      40% { transform: translateX(6px); }
      60% { transform: translateX(-4px); }
      80% { transform: translateX(4px); }
    }
  `]
})
export class PaymentModalComponent implements AfterViewInit {
  @ViewChild('amountInput') amountInput!: ElementRef<HTMLInputElement>;
  @ViewChild('amountInputCard') amountInputCard!: ElementRef<HTMLInputElement>;

  @Input() total = 0;
  @Input() initialMethod: PaymentMethodEnum | null = null;
  @Input() getPaymentLabel!: (method: string) => string;
  @Input() errorMessage: string = '';
  @Input() processing: boolean = false;

  @Output() paymentsConfirmed = new EventEmitter<PaymentDTO[]>();
  @Output() cancel = new EventEmitter<void>();

  payments: PaymentDTO[] = [];
  selectedMethod: PaymentMethodEnum | null = null;
  currentAmount = 0;

  availableMethods = [
    { value: 'CASH' as PaymentMethodEnum, label: 'Dinheiro', icon: 'payments' },
    { value: 'CREDIT_CARD' as PaymentMethodEnum, label: 'Crédito', icon: 'credit_card' },
    { value: 'DEBIT_CARD' as PaymentMethodEnum, label: 'Débito', icon: 'credit_card' },
    { value: 'PIX' as PaymentMethodEnum, label: 'PIX', icon: 'pix' },
  ];

  ngAfterViewInit(): void {
    if (this.initialMethod) {
      setTimeout(() => this.selectMethod(this.initialMethod!), 50);
    }
  }

  selectMethod(method: PaymentMethodEnum): void {
    this.selectedMethod = method;
    const remaining = this.getRemaining();
    this.currentAmount = remaining > 0 ? parseFloat(remaining.toFixed(2)) : parseFloat(this.total.toFixed(2));

    setTimeout(() => {
      if (method === 'CASH') {
        this.amountInput?.nativeElement?.focus();
        this.amountInput?.nativeElement?.select();
      } else {
        this.amountInputCard?.nativeElement?.focus();
        this.amountInputCard?.nativeElement?.select();
      }
    }, 100);
  }

  addPayment(): void {
    if (!this.selectedMethod || this.currentAmount <= 0) return;

    this.payments.push({
      method: this.selectedMethod,
      amount: parseFloat(this.currentAmount.toFixed(2))
    });

    this.selectedMethod = null;
    this.currentAmount = 0;
  }

  removePayment(index: number): void {
    this.payments.splice(index, 1);
  }

  getTotalPaid(): number {
    return this.payments.reduce((sum, p) => sum + p.amount, 0);
  }

  getRemaining(): number {
    return Math.max(0, this.total - this.getTotalPaid());
  }

  getTotalChange(): number {
    return Math.max(0, this.getTotalPaid() - this.total);
  }

  confirmAll(): void {
    if (this.payments.length === 0 || this.getRemaining() > 0.001) return;
    this.paymentsConfirmed.emit([...this.payments]);
  }

  getMethodIcon(method: PaymentMethodEnum): string {
    const icons: Record<string, string> = {
      'CASH': 'payments',
      'CREDIT_CARD': 'credit_card',
      'DEBIT_CARD': 'credit_card',
      'PIX': 'pix'
    };
    return icons[method] || 'payment';
  }
}
