import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { CartItem } from '../../../shared/models/cart.model';

@Component({
  selector: 'app-cart-table',
  imports: [CommonModule, MatIconModule, MatButtonModule, MatTooltipModule],
  template: `
    <div class="cart-wrapper">
      <table class="cart-table" *ngIf="items.length > 0">
        <thead>
          <tr>
            <th class="col-seq">#</th>
            <th class="col-barcode">Código</th>
            <th class="col-desc">Descrição</th>
            <th class="col-qty">Qtd</th>
            <th class="col-price">Preço Unit.</th>
            <th class="col-subtotal">Subtotal</th>
            <th class="col-actions"></th>
          </tr>
        </thead>
        <tbody>
          <tr
            *ngFor="let item of items; let i = index"
            [class.selected]="i === selectedIndex"
            (click)="selectItem.emit(i)">
            <td class="col-seq">{{ i + 1 }}</td>
            <td class="col-barcode">{{ item.barcode }}</td>
            <td class="col-desc">{{ item.description }}</td>
            <td class="col-qty">{{ item.quantity }}</td>
            <td class="col-price">R$ {{ item.unitPrice | number:'1.2-2' }}</td>
            <td class="col-subtotal">R$ {{ item.subtotal | number:'1.2-2' }}</td>
            <td class="col-actions">
              <button mat-icon-button matTooltip="Remover item" color="warn"
                      (click)="removeItem.emit(i); $event.stopPropagation()">
                <mat-icon>close</mat-icon>
              </button>
            </td>
          </tr>
        </tbody>
      </table>

      <div class="cart-empty" *ngIf="items.length === 0">
        <mat-icon class="empty-icon">shopping_cart</mat-icon>
        <p>Carrinho vazio</p>
        <p class="empty-hint">Leia um código de barras ou digite e pressione Enter</p>
      </div>
    </div>
  `,
  styles: [`
    .cart-wrapper {
      flex: 1;
      background: var(--surface);
      border-radius: var(--radius);
      box-shadow: var(--shadow);
      overflow: auto;
    }

    .cart-table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: 10px 12px;
        text-align: left;
      }

      thead th {
        position: sticky;
        top: 0;
        background: var(--primary-dark);
        color: var(--header-text);
        font-size: 12px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }

      tbody tr {
        border-bottom: 1px solid var(--border);
        cursor: pointer;
        transition: background 0.15s;

        &:hover {
          background: var(--table-hover);
        }

        &.selected {
          background: var(--info-bg);
          border-left: 3px solid var(--primary);
        }
      }

      .col-seq { width: 40px; text-align: center; }
      .col-barcode { width: 140px; font-family: monospace; font-size: 13px; }
      .col-desc { min-width: 200px; }
      .col-qty { width: 60px; text-align: center; font-weight: 700; }
      .col-price, .col-subtotal { width: 120px; text-align: right; }
      .col-subtotal { font-weight: 700; color: var(--primary-dark); }
      .col-actions { width: 50px; text-align: center; }
    }

    .cart-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 60px 20px;
      color: var(--text-secondary);

      .empty-icon {
        font-size: 64px;
        height: 64px;
        width: 64px;
        opacity: 0.3;
        margin-bottom: 16px;
      }

      p {
        font-size: 18px;
        font-weight: 500;
      }

      .empty-hint {
        font-size: 14px;
        margin-top: 8px;
        opacity: 0.7;
      }
    }
  `]
})
export class CartTableComponent {
  @Input() items: CartItem[] = [];
  @Input() selectedIndex = -1;
  @Output() selectItem = new EventEmitter<number>();
  @Output() removeItem = new EventEmitter<number>();
}

