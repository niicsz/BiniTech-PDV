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
            [class.even]="i % 2 === 0"
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
        <div class="empty-visual">
          <mat-icon class="empty-icon">shopping_cart</mat-icon>
        </div>
        <p class="empty-title">Carrinho vazio</p>
        <p class="empty-hint">Leia um código de barras ou pesquise um produto para começar</p>
        <div class="empty-shortcuts">
          <span class="empty-shortcut"><kbd>Enter</kbd> para adicionar</span>
          <span class="empty-shortcut"><kbd>F4-F9</kbd> para pagar</span>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .cart-wrapper {
      flex: 1;
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: var(--radius-md);
      overflow: auto;
    }

    .cart-table {
      width: 100%;
      border-collapse: collapse;

      th, td {
        padding: 10px 14px;
        text-align: left;
      }

      thead th {
        position: sticky;
        top: 0;
        background: var(--table-header-bg);
        color: var(--table-header-text);
        font-size: 11px;
        font-weight: 700;
        text-transform: uppercase;
        letter-spacing: 0.5px;
        border-bottom: 2px solid var(--border);
        z-index: 1;
      }

      tbody tr {
        border-bottom: 1px solid var(--border-light);
        cursor: pointer;
        transition: background 0.15s;

        &:hover {
          background: var(--surface-hover);
        }

        &.selected {
          background: var(--primary-bg);
          border-left: 3px solid var(--primary);
        }
      }

      .col-seq {
        width: 40px;
        text-align: center;
        color: var(--text-tertiary);
        font-size: 12px;
      }
      .col-barcode {
        width: 140px;
        font-family: var(--font-mono);
        font-size: 12px;
        color: var(--text-secondary);
      }
      .col-desc {
        min-width: 200px;
        font-weight: 500;
      }
      .col-qty {
        width: 60px;
        text-align: center;
        font-weight: 700;
        color: var(--primary);
      }
      .col-price {
        width: 120px;
        text-align: right;
        font-family: var(--font-mono);
        font-size: 13px;
      }
      .col-subtotal {
        width: 130px;
        text-align: right;
        font-weight: 700;
        font-family: var(--font-mono);
        color: var(--text);
        font-size: 14px;
      }
      .col-actions {
        width: 48px;
        text-align: center;
      }
    }

    .cart-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px 24px;
      color: var(--text-secondary);
      min-height: 300px;
    }

    .empty-visual {
      width: 80px;
      height: 80px;
      border-radius: 50%;
      background: var(--surface-alt);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 20px;
      border: 2px dashed var(--border);
    }

    .empty-icon {
      font-size: 36px;
      height: 36px;
      width: 36px;
      opacity: 0.4;
      color: var(--text-tertiary);
    }

    .empty-title {
      font-size: 18px;
      font-weight: 600;
      color: var(--text);
      margin-bottom: 6px;
    }

    .empty-hint {
      font-size: 14px;
      color: var(--text-secondary);
      margin-bottom: 20px;
    }

    .empty-shortcuts {
      display: flex;
      gap: 16px;
    }

    .empty-shortcut {
      font-size: 12px;
      color: var(--text-tertiary);

      kbd {
        display: inline-block;
        background: var(--kbd-bg);
        color: var(--kbd-color);
        padding: 2px 6px;
        border-radius: 3px;
        font-size: 10px;
        font-weight: 700;
        font-family: var(--font-mono);
        margin-right: 4px;
      }
    }

    @media (max-width: 1366px) {
      .cart-table {
        th, td {
          padding: 8px 10px;
        }

        .col-barcode {
          width: 120px;
        }
        .col-price {
          width: 100px;
        }
        .col-subtotal {
          width: 110px;
        }
      }
    }

    @media (max-width: 1280px) {
      .cart-table {
        th, td {
          padding: 7px 8px;
          font-size: 12px;
        }

        thead th {
          font-size: 10px;
        }

        .col-seq {
          width: 32px;
        }
        .col-barcode {
          width: 100px;
          font-size: 11px;
        }
        .col-desc {
          min-width: 140px;
        }
        .col-qty {
          width: 48px;
        }
        .col-price {
          width: 90px;
          font-size: 12px;
        }
        .col-subtotal {
          width: 100px;
          font-size: 13px;
        }
        .col-actions {
          width: 40px;
        }
      }

      .cart-empty {
        padding: 32px 16px;
        min-height: 200px;
      }

      .empty-visual {
        width: 60px;
        height: 60px;
        margin-bottom: 14px;
      }

      .empty-icon {
        font-size: 28px;
        height: 28px;
        width: 28px;
      }

      .empty-title {
        font-size: 15px;
      }

      .empty-hint {
        font-size: 13px;
      }
    }

    @media (max-height: 768px) {
      .cart-table {
        th, td {
          padding: 6px 8px;
        }
      }

      .cart-empty {
        padding: 24px 16px;
        min-height: 150px;
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

