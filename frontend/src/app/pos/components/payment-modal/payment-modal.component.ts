import { Component, Input, Output, EventEmitter, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PaymentMethodEnum } from '../../../shared/models/api.models';

@Component({
  selector: 'app-payment-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="cancel.emit()">
      <div class="modal-content modal-payment" (click)="$event.stopPropagation()">
        <h3>
          <span class="material-icons">{{ getMethodIcon() }}</span>
          Finalizar Venda — {{ getPaymentLabel(paymentMethod) }}
        </h3>

        <div class="payment-total">
          <span>Total da Venda:</span>
          <strong>R$ {{ total | number:'1.2-2' }}</strong>
        </div>

        <div class="form-group" *ngIf="paymentMethod === 'CASH'">
          <label for="amountInput">Valor Recebido (R$):</label>
          <input
            #amountInput
            id="amountInput"
            type="number"
            step="0.01"
            min="0"
            class="form-input amount-input"
            [ngModel]="amountPaid"
            (ngModelChange)="onAmountChange($event)"
            (keydown.enter)="confirm.emit()"
            autofocus
          />
        </div>

        <div class="payment-change" *ngIf="paymentMethod === 'CASH' && changeValue > 0">
          <span>Troco:</span>
          <strong class="change-value">R$ {{ changeValue | number:'1.2-2' }}</strong>
        </div>

        <div class="payment-info" *ngIf="paymentMethod !== 'CASH'">
          <span class="material-icons info-icon">info</span>
          <p>O valor total será cobrado automaticamente via {{ getPaymentLabel(paymentMethod) }}.</p>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" (click)="cancel.emit()">
            Cancelar (Esc)
          </button>
          <button class="btn btn-success btn-lg" (click)="confirm.emit()">
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

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 18px;
        margin-bottom: 20px;
        color: var(--text);
      }
    }

    .modal-payment { width: 440px; }

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
      color: #fff;
      border-radius: var(--radius);
      margin-bottom: 20px;
      font-size: 16px;

      strong { font-size: 24px; }
    }

    .form-group {
      margin-bottom: 16px;

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
      border-radius: var(--radius);
      font-size: 16px;
      transition: border-color 0.2s;

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

    .payment-change {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 14px 16px;
      background: #e8f5e9;
      border-radius: var(--radius);
      margin-bottom: 16px;
      font-size: 16px;
      font-weight: 600;

      .change-value {
        font-size: 28px;
        color: var(--success);
      }
    }

    .payment-info {
      display: flex;
      align-items: flex-start;
      gap: 10px;
      padding: 14px 16px;
      background: #e3f2fd;
      border-radius: var(--radius);
      margin-bottom: 16px;
      color: var(--primary);

      .info-icon { font-size: 22px; margin-top: 1px; }
      p { font-size: 14px; line-height: 1.5; }
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
    }

    .btn-lg { padding: 14px 20px; font-size: 15px; }
    .btn-primary { background: var(--primary); color: #fff; }
    .btn-success { background: var(--success); color: #fff; }
    .btn-secondary { background: var(--border); color: var(--text); }
  `]
})
export class PaymentModalComponent implements AfterViewInit {
  @ViewChild('amountInput') amountInput!: ElementRef<HTMLInputElement>;

  @Input() total = 0;
  @Input() paymentMethod: PaymentMethodEnum = 'CASH';
  @Input() amountPaid = 0;
  @Input() changeValue = 0;
  @Input() getPaymentLabel!: (method: string) => string;

  @Output() amountPaidChange = new EventEmitter<number>();
  @Output() confirm = new EventEmitter<void>();
  @Output() cancel = new EventEmitter<void>();

  ngAfterViewInit(): void {
    if (this.paymentMethod === 'CASH') {
      setTimeout(() => {
        this.amountInput?.nativeElement?.focus();
        this.amountInput?.nativeElement?.select();
      }, 100);
    }
  }

  onAmountChange(value: number): void {
    this.amountPaid = value;
    this.amountPaidChange.emit(value);
  }

  getMethodIcon(): string {
    const icons: Record<string, string> = {
      'CASH': 'payments',
      'CREDIT_CARD': 'credit_card',
      'DEBIT_CARD': 'credit_card',
      'PIX': 'pix'
    };
    return icons[this.paymentMethod] || 'payment';
  }
}

