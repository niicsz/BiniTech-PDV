import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentDTO, PaymentMethodEnum } from '../../../shared/models/api.models';

@Component({
  selector: 'app-payment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="cancel.emit()">
      <div class="modal-content modal-payment" (click)="$event.stopPropagation()">
        <h3>
          <span class="material-icons">payment</span>
          Finalizar Venda
        </h3>

        <div class="payment-total">
          <span>Total da Venda:</span>
          <strong>R$ {{ total | number:'1.2-2' }}</strong>
        </div>

        <div class="payments-list" *ngIf="payments.length > 0">
          <h4>Pagamentos adicionados:</h4>
          <div class="payment-entry" *ngFor="let p of payments; let i = index">
            <span class="material-icons entry-icon">{{ getMethodIcon(p.method) }}</span>
            <span class="entry-label">{{ getPaymentLabel(p.method) }}</span>
            <strong class="entry-amount">R$ {{ p.amount | number:'1.2-2' }}</strong>
            <button class="btn-icon-sm btn-icon-danger" (click)="removePayment(i)" title="Remover">
              <span class="material-icons">close</span>
            </button>
          </div>
          <div class="payments-summary">
            <div class="summary-line">
              <span>Total pago:</span>
              <strong>R$ {{ getTotalPaid() | number:'1.2-2' }}</strong>
            </div>
            <div class="summary-line remaining" *ngIf="getRemaining() > 0">
              <span>Falta pagar:</span>
              <strong class="text-danger">R$ {{ getRemaining() | number:'1.2-2' }}</strong>
            </div>
            <div class="summary-line change" *ngIf="getTotalChange() > 0">
              <span>Troco:</span>
              <strong class="text-success">R$ {{ getTotalChange() | number:'1.2-2' }}</strong>
            </div>
          </div>
        </div>

        <div class="add-payment-section">
          <h4>{{ payments.length > 0 ? 'Adicionar outro pagamento:' : 'Forma de pagamento:' }}</h4>
          <div class="method-buttons">
            <button class="btn method-btn"
              *ngFor="let m of availableMethods"
              [class.active]="selectedMethod === m.value"
              (click)="selectMethod(m.value)">
              <span class="material-icons">{{ m.icon }}</span>
              {{ m.label }}
            </button>
          </div>

          <div class="amount-section" *ngIf="selectedMethod">
            <div class="form-group" *ngIf="selectedMethod === 'CASH'">
              <label for="amountInput">Valor Recebido (R$):</label>
              <input
                #amountInput
                id="amountInput"
                type="number"
                step="0.01"
                min="0"
                class="form-input amount-input"
                [(ngModel)]="currentAmount"
                (keydown.enter)="addPayment()"
                autofocus
              />
            </div>

            <div class="form-group" *ngIf="selectedMethod !== 'CASH'">
              <label for="amountInputCard">Valor (R$):</label>
              <input
                #amountInputCard
                id="amountInputCard"
                type="number"
                step="0.01"
                min="0.01"
                class="form-input amount-input"
                [(ngModel)]="currentAmount"
                (keydown.enter)="addPayment()"
                autofocus
              />
            </div>

            <button class="btn btn-primary btn-add" (click)="addPayment()">
              <span class="material-icons">add</span>
              Adicionar Pagamento
            </button>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" (click)="cancel.emit()">
            Cancelar (Esc)
          </button>
          <button class="btn btn-success btn-lg"
            (click)="confirmAll()"
            [disabled]="payments.length === 0 || getRemaining() > 0.001">
            <span class="material-icons">check</span>
            Confirmar Pagamento
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      animation: fadeIn 0.2s ease;
    }

    @keyframes fadeIn {
      from { opacity: 0; }
      to { opacity: 1; }
    }

    .modal-content {
      background: var(--surface);
      border-radius: 12px;
      padding: 28px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
      animation: scaleIn 0.2s ease;
      max-height: 90vh;
      overflow-y: auto;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        margin-bottom: 20px;
        color: var(--text);
      }

      h4 {
        font-size: 14px;
        font-weight: 600;
        color: var(--text-secondary);
        margin-bottom: 10px;
      }
    }

    .modal-payment { width: 520px; }

    @keyframes scaleIn {
      from { transform: scale(0.95); opacity: 0; }
      to { transform: scale(1); opacity: 1; }
    }

    .payment-total {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: var(--primary-dark);
      color: var(--header-text);
      border-radius: var(--radius);
      margin-bottom: 20px;
      font-size: 16px;

      strong { font-size: 24px; }
    }

    .payments-list {
      margin-bottom: 20px;
      padding: 16px;
      background: var(--bg);
      border-radius: var(--radius);
    }

    .payment-entry {
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 8px 0;
      border-bottom: 1px solid var(--border);

      .entry-icon { font-size: 20px; color: var(--primary); }
      .entry-label { flex: 1; font-size: 14px; }
      .entry-amount { font-size: 15px; color: var(--text); }
    }

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

      &.remaining { color: var(--danger); }
      &.change {
        padding: 8px 12px;
        background: var(--success-bg);
        border-radius: var(--radius);
        margin-top: 4px;
      }
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
      padding: 12px 16px;
      background: var(--surface);
      border: 2px solid var(--border);
      border-radius: var(--radius);
      font-size: 14px;
      font-weight: 600;
      color: var(--text);
      cursor: pointer;
      transition: all 0.2s;

      .material-icons { font-size: 20px; }

      &:hover {
        border-color: var(--primary);
        background: var(--info-bg);
      }

      &.active {
        border-color: var(--primary);
        background: var(--primary);
        color: #ffffff;
      }
    }

    .amount-section {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .form-group {
      margin-bottom: 0;

      label {
        display: block;
        font-size: 13px;
        font-weight: 600;
        color: var(--text-secondary);
        margin-bottom: 6px;
      }
    }

    .form-input {
      width: 100%;
      padding: 12px;
      border: 2px solid var(--border);
      background: var(--input-bg);
      color: var(--text);
      border-radius: var(--radius);
      font-size: 16px;
      transition: border-color 0.2s;
      box-sizing: border-box;

      &:focus {
        outline: none;
        border-color: var(--primary);
        box-shadow: 0 0 0 3px rgba(21, 101, 192, 0.12);
      }
    }

    .amount-input {
      font-size: 24px;
      font-weight: 700;
      text-align: right;
    }

    .btn-add {
      align-self: flex-end;
    }

    .modal-actions {
      display: flex;
      justify-content: flex-end;
      gap: 10px;
      margin-top: 24px;
    }

    .btn {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      padding: 12px 20px;
      border: none;
      border-radius: var(--radius);
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.2s;

      .material-icons { font-size: 20px; }
      &:hover { filter: brightness(1.1); transform: translateY(-1px); }
      &:active { transform: translateY(0); }
      &:disabled { opacity: 0.5; cursor: not-allowed; transform: none; filter: none; }
    }

    .btn-lg { padding: 14px 20px; font-size: 15px; }
    .btn-primary { background: var(--primary); color: #ffffff; }
    .btn-success { background: var(--success); color: #ffffff; }
    .btn-secondary { background: var(--border); color: var(--text); }

    .btn-icon-sm {
      background: none;
      border: none;
      cursor: pointer;
      padding: 4px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: background 0.2s;

      .material-icons { font-size: 18px; }

      &.btn-icon-danger {
        color: var(--danger);
        &:hover { background: var(--danger-bg); }
      }
    }

    .add-payment-section {
      margin-bottom: 8px;
    }
  `]
})
export class PaymentModalComponent implements AfterViewInit {
  @ViewChild('amountInput') amountInput!: ElementRef<HTMLInputElement>;
  @ViewChild('amountInputCard') amountInputCard!: ElementRef<HTMLInputElement>;

  @Input() total = 0;
  @Input() initialMethod: PaymentMethodEnum | null = null;
  @Input() getPaymentLabel!: (method: string) => string;

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
