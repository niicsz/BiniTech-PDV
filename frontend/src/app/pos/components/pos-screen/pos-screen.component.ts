import { Component, OnInit, OnDestroy, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ProductService } from '../../services/product.service';
import { SaleService } from '../../services/sale.service';
import { AuthService } from '../../../auth/services/auth.service';
import { CartItem } from '../../../shared/models/cart.model';
import {
  ProductDTO, CreateSaleDTO, CreateSaleItemDTO,
  PaymentDTO, PaymentMethodEnum, SaleDTO
} from '../../../shared/models/api.models';
import { CartTableComponent } from '../cart-table/cart-table.component';
import { PaymentModalComponent } from '../payment-modal/payment-modal.component';

@Component({
  selector: 'app-pos-screen',
  standalone: true,
  imports: [CommonModule, FormsModule, CartTableComponent, PaymentModalComponent],
  templateUrl: './pos-screen.component.html',
  styleUrls: ['./pos-screen.component.scss']
})
export class PosScreenComponent implements OnInit, OnDestroy {

  @ViewChild('barcodeInput') barcodeInput!: ElementRef<HTMLInputElement>;

  cart: CartItem[] = [];
  barcodeValue = '';
  total = 0;

  showPaymentModal = false;
  initialPaymentMethod: PaymentMethodEnum | null = null;

  showQuantityModal = false;
  selectedCartIndex = -1;
  newQuantity = 1;

  showReceiptModal = false;
  lastSale: SaleDTO | null = null;

  statusMessage = '';
  statusType: 'success' | 'error' | 'info' = 'info';
  private statusTimeout: any;

  loggedInUsername = '';

  showLowStockAlert = false;
  lowStockProducts: { description: string; stockQuantity: number }[] = [];
  private pendingLowStockProducts: { description: string; stockQuantity: number }[] = [];

  allProducts: ProductDTO[] = [];
  filteredProducts: ProductDTO[] = [];
  showDropdown = false;
  dropdownSelectedIndex = -1;

  constructor(
    private productService: ProductService,
    private saleService: SaleService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.loggedInUsername = this.authService.getUsername() || 'Operador';
    this.focusBarcode();
    this.loadProducts();
  }

  loadProducts(): void {
    this.productService.listAll().subscribe({
      next: (products) => {
        this.allProducts = products;
      },
      error: () => {
        console.warn('Não foi possível carregar os produtos para busca offline.');
      }
    });
  }

  ngOnDestroy(): void {
    if (this.statusTimeout) {
      clearTimeout(this.statusTimeout);
    }
  }

  @HostListener('window:keydown', ['$event'])
  handleKeyboard(event: KeyboardEvent): void {
    if (this.showLowStockAlert) {
      if (event.key === 'Escape' || event.key === 'Enter') {
        this.closeLowStockAlert();
        event.preventDefault();
      }
      return;
    }

    if (this.showPaymentModal || this.showReceiptModal) {
      if (event.key === 'Escape') {
        this.closePaymentModal();
        this.closeReceiptModal();
        event.preventDefault();
      }
      return;
    }

    // Dropdown navigation
    if (this.showDropdown) {
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        this.dropdownSelectedIndex = Math.min(this.dropdownSelectedIndex + 1, this.filteredProducts.length - 1);
        return;
      }
      if (event.key === 'ArrowUp') {
        event.preventDefault();
        this.dropdownSelectedIndex = Math.max(this.dropdownSelectedIndex - 1, 0);
        return;
      }
      if (event.key === 'Escape') {
        event.preventDefault();
        this.hideDropdown();
        return;
      }
    }

    switch (event.key) {
      case 'F2':
        event.preventDefault();
        this.openQuantityModal();
        break;
      case 'F4':
        event.preventDefault();
        this.startPayment('CASH');
        break;
      case 'F7':
        event.preventDefault();
        this.startPayment('CREDIT_CARD');
        break;
      case 'F8':
        event.preventDefault();
        this.startPayment('DEBIT_CARD');
        break;
      case 'F9':
        event.preventDefault();
        this.startPayment('PIX');
        break;
      case 'Delete':
        event.preventDefault();
        this.removeSelectedItem();
        break;
      case 'Escape':
        event.preventDefault();
        this.closeQuantityModal();
        this.focusBarcode();
        break;
    }
  }

  hideDropdown(): void {
    setTimeout(() => {
      this.showDropdown = false;
      this.filteredProducts = [];
      this.dropdownSelectedIndex = -1;
    }, 150);
  }

  onSearchChange(): void {
    const val = this.barcodeValue.trim().toLowerCase();
    
    // Only search if not purely numbers (like an active barcode scan in progress)
    // or if the user typed at least 3 chars
    if (val.length < 2) {
      this.hideDropdown();
      return;
    }

    this.filteredProducts = this.allProducts
      .filter(p => 
        (p.description && p.description.toLowerCase().includes(val)) ||
        (p.barcode && p.barcode.includes(val))
      )
      .slice(0, 8); // show up to 8 items

    this.showDropdown = this.filteredProducts.length > 0;
    this.dropdownSelectedIndex = this.showDropdown ? 0 : -1;
  }

  onBarcodeSubmit(): void {
    const barcode = this.barcodeValue.trim();
    if (!barcode) return;

    // Se tiver dropdown aberto e um item selecionado, usa o item
    if (this.showDropdown && this.dropdownSelectedIndex >= 0) {
      const selected = this.filteredProducts[this.dropdownSelectedIndex];
      this.addToCart(selected);
      this.barcodeValue = '';
      this.hideDropdown();
      this.focusBarcode();
      return;
    }

    // Try exact barcode match locally first for faster response
    const exactMatch = this.allProducts.find(p => p.barcode === barcode);
    if (exactMatch) {
      this.addToCart(exactMatch);
      this.barcodeValue = '';
      this.hideDropdown();
      this.focusBarcode();
      return;
    }

    // Fallback to API
    this.productService.getByBarcode(barcode).subscribe({
      next: (product: ProductDTO) => {
        this.addToCart(product);
        this.barcodeValue = '';
        this.hideDropdown();
        this.focusBarcode();
      },
      error: () => {
        this.showStatus(`Produto não encontrado: ${barcode}`, 'error');
        this.barcodeValue = '';
        this.hideDropdown();
        this.focusBarcode();
      }
    });
  }

  selectFromDropdown(product: ProductDTO): void {
    this.addToCart(product);
    this.barcodeValue = '';
    this.hideDropdown();
    this.focusBarcode();
  }

  addToCart(product: ProductDTO): void {
    const existing = this.cart.find(item => item.productId === product.id);
    if (existing) {
      existing.quantity += 1;
      existing.subtotal = existing.quantity * existing.unitPrice;
    } else {
      this.cart.push({
        productId: product.id!,
        barcode: product.barcode || '',
        description: product.description || '',
        quantity: 1,
        unitPrice: product.price || 0,
        subtotal: product.price || 0
      });
    }
    this.recalculateTotal();
    this.showStatus(`${product.description} adicionado ao carrinho`, 'success');
  }

  removeItem(index: number): void {
    const item = this.cart[index];
    this.cart.splice(index, 1);
    if (this.selectedCartIndex === index) {
      this.selectedCartIndex = -1;
    } else if (this.selectedCartIndex > index) {
      this.selectedCartIndex--;
    }
    this.recalculateTotal();
    this.showStatus(`${item.description} removido`, 'info');
    this.focusBarcode();
  }

  removeSelectedItem(): void {
    if (this.selectedCartIndex >= 0 && this.selectedCartIndex < this.cart.length) {
      this.removeItem(this.selectedCartIndex);
    }
  }

  selectItem(index: number): void {
    this.selectedCartIndex = index;
  }

  recalculateTotal(): void {
    this.total = this.cart.reduce((sum, item) => sum + item.subtotal, 0);
  }

  clearCart(): void {
    this.cart = [];
    this.selectedCartIndex = -1;
    this.total = 0;
    this.focusBarcode();
  }

  openQuantityModal(): void {
    if (this.cart.length === 0) {
      this.showStatus('Carrinho vazio. Adicione um produto primeiro.', 'error');
      return;
    }
    if (this.selectedCartIndex < 0) {
      this.selectedCartIndex = this.cart.length - 1;
    }
    this.newQuantity = this.cart[this.selectedCartIndex].quantity;
    this.showQuantityModal = true;
  }

  confirmQuantity(): void {
    if (this.newQuantity < 1) {
      this.showStatus('Quantidade deve ser pelo menos 1.', 'error');
      return;
    }
    const item = this.cart[this.selectedCartIndex];
    item.quantity = this.newQuantity;
    item.subtotal = item.quantity * item.unitPrice;
    this.recalculateTotal();
    this.showQuantityModal = false;
    this.showStatus(`Quantidade de "${item.description}" alterada para ${item.quantity}`, 'success');
    this.focusBarcode();
  }

  closeQuantityModal(): void {
    this.showQuantityModal = false;
    this.focusBarcode();
  }

  startPayment(method: PaymentMethodEnum): void {
    if (this.cart.length === 0) {
      this.showStatus('Carrinho vazio. Adicione produtos antes de finalizar.', 'error');
      return;
    }
    this.initialPaymentMethod = method;
    this.showPaymentModal = true;
  }

  onPaymentsConfirmed(payments: PaymentDTO[]): void {
    const items: CreateSaleItemDTO[] = this.cart.map(item => ({
      productId: item.productId,
      quantity: item.quantity
    }));

    const soldProductIds = this.cart.map(item => item.productId);
    const sale: CreateSaleDTO = { items, payments };

    this.saleService.create(sale).subscribe({
      next: (result: SaleDTO) => {
        this.lastSale = result;
        this.showPaymentModal = false;
        this.showReceiptModal = true;
        this.clearCart();
        this.showStatus('Venda finalizada com sucesso!', 'success');
        this.checkLowStock(soldProductIds);
      },
      error: (err) => {
        const msg = err.error?.message || 'Erro ao finalizar a venda.';
        this.showStatus(msg, 'error');
      }
    });
  }

  private checkLowStock(productIds: string[]): void {
    const uniqueIds = [...new Set(productIds)];
    const requests = uniqueIds.map(id => this.productService.getById(id));

    forkJoin(requests).subscribe({
      next: (products: ProductDTO[]) => {
        const lowStock = products
          .filter(p => (p.stockQuantity ?? 0) < 5)
          .map(p => ({
            description: p.description || 'Sem descrição',
            stockQuantity: p.stockQuantity ?? 0
          }));

        if (lowStock.length > 0) {
          this.pendingLowStockProducts = lowStock;
        }
      },
      error: () => {
        console.warn('Não foi possível verificar estoque dos produtos.');
      }
    });
  }

  private showPendingLowStockAlert(): void {
    if (this.pendingLowStockProducts.length > 0) {
      this.lowStockProducts = this.pendingLowStockProducts;
      this.pendingLowStockProducts = [];
      this.showLowStockAlert = true;
    }
  }

  closeLowStockAlert(): void {
    this.showLowStockAlert = false;
    this.lowStockProducts = [];
    this.focusBarcode();
  }

  closePaymentModal(): void {
    this.showPaymentModal = false;
    this.focusBarcode();
  }

  closeReceiptModal(): void {
    this.showReceiptModal = false;
    this.lastSale = null;
    this.showPendingLowStockAlert();
    if (!this.showLowStockAlert) {
      this.focusBarcode();
    }
  }

  printReceipt(): void {
    window.print();
  }

  focusBarcode(): void {
    setTimeout(() => {
      this.barcodeInput?.nativeElement?.focus();
    }, 50);
  }

  showStatus(message: string, type: 'success' | 'error' | 'info'): void {
    this.statusMessage = message;
    this.statusType = type;
    if (this.statusTimeout) {
      clearTimeout(this.statusTimeout);
    }
    this.statusTimeout = setTimeout(() => {
      this.statusMessage = '';
    }, 4000);
  }

  getPaymentLabel(method: string): string {
    const labels: Record<string, string> = {
      'CASH': 'Dinheiro',
      'CREDIT_CARD': 'Cartão de Crédito',
      'DEBIT_CARD': 'Cartão de Débito',
      'PIX': 'PIX'
    };
    return labels[method] || method;
  }

  formatTimestamp(ts?: string): string {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleString('pt-BR');
  }

  getTotalQty(): number {
    return this.cart.reduce((sum, item) => sum + item.quantity, 0);
  }
}
