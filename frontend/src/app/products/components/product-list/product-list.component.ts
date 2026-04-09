import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ProductService } from '../../../pos/services/product.service';
import { ProductDTO, CreateProductDTO } from '../../../shared/models/api.models';

@Component({
  selector: 'app-product-list',
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatTooltipModule
  ],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit {

  products: ProductDTO[] = [];
  showForm = false;
  editingId: string | null = null;

  selectedCategory = '';
  searchTerm = '';

  form: CreateProductDTO = {
    barcode: '',
    description: '',
    price: 0,
    costPrice: 0,
    stockQuantity: 0,
    category: '',
    active: true
  };

  statusMessage = '';
  statusType: 'success' | 'error' = 'success';

  constructor(private productService: ProductService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  get categories(): string[] {
    return this.products
      .map(p => p.category || 'Sem Categoria')
      .filter((v, i, a) => a.indexOf(v) === i)
      .sort();
  }

  get activeCount(): number {
    return this.products.filter(p => p.active).length;
  }

  get lowStockCount(): number {
    return this.products.filter(p => (p.stockQuantity ?? 0) <= 5 && p.active).length;
  }

  get totalSaleValue(): number {
    return this.filteredProducts.reduce((sum, p) => sum + (p.price || 0) * (p.stockQuantity || 0), 0);
  }

  get totalCostValue(): number {
    return this.filteredProducts.reduce((sum, p) => sum + (p.costPrice || 0) * (p.stockQuantity || 0), 0);
  }

  get filteredProducts(): ProductDTO[] {
    let result = this.products;

    if (this.selectedCategory) {
      result = result.filter(p =>
        (p.category || 'Sem Categoria') === this.selectedCategory
      );
    }

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.trim().toLowerCase();
      result = result.filter(p =>
        (p.description && p.description.toLowerCase().includes(term)) ||
        (p.barcode && p.barcode.toLowerCase().includes(term))
      );
    }

    return result;
  }

  loadProducts(): void {
    this.productService.listAll().subscribe({
      next: (products) => {
        this.products = products;
        console.info('[ProductList] Produtos carregados:', products.length);
      },
      error: () => {
        console.error('[ProductList] Erro ao carregar produtos');
        this.showStatus('Erro ao carregar produtos.', 'error');
      }
    });
  }

  openCreateForm(): void {
    this.editingId = null;
    this.form = { barcode: '', description: '', price: 0, costPrice: 0, stockQuantity: 0, category: '', active: true };
    this.showForm = true;
  }

  openEditForm(product: ProductDTO): void {
    this.editingId = product.id!;
    this.form = {
      barcode: product.barcode || '',
      description: product.description || '',
      price: product.price || 0,
      costPrice: product.costPrice || 0,
      stockQuantity: product.stockQuantity || 0,
      category: product.category || '',
      active: product.active ?? true
    };
    this.showForm = true;
  }

  saveProduct(): void {
    if (this.editingId) {
      console.info('[ProductList] Atualizando produto:', this.editingId);
      this.productService.update(this.editingId, this.form).subscribe({
        next: () => {
          console.info('[ProductList] Produto atualizado com sucesso:', this.editingId);
          this.showStatus('Produto atualizado com sucesso!', 'success');
          this.showForm = false;
          this.loadProducts();
        },
        error: (err) => {
          console.error('[ProductList] Erro ao atualizar produto:', err.error?.message);
          this.showStatus(err.error?.message || 'Erro ao atualizar.', 'error');
        }
      });
    } else {
      console.info('[ProductList] Criando novo produto:', this.form.description);
      this.productService.create(this.form).subscribe({
        next: () => {
          console.info('[ProductList] Produto criado com sucesso:', this.form.description);
          this.showStatus('Produto cadastrado com sucesso!', 'success');
          this.showForm = false;
          this.loadProducts();
        },
        error: (err) => {
          console.error('[ProductList] Erro ao cadastrar produto:', err.error?.message);
          this.showStatus(err.error?.message || 'Erro ao cadastrar.', 'error');
        }
      });
    }
  }

  deleteProduct(id: string): void {
    if (confirm('Deseja realmente remover este produto?')) {
      console.info('[ProductList] Removendo produto:', id);
      this.productService.delete(id).subscribe({
        next: () => {
          console.info('[ProductList] Produto removido com sucesso:', id);
          this.showStatus('Produto removido.', 'success');
          this.loadProducts();
        },
        error: () => {
          console.error('[ProductList] Erro ao remover produto:', id);
          this.showStatus('Erro ao remover produto.', 'error');
        }
      });
    }
  }

  cancelForm(): void {
    this.showForm = false;
  }

  showStatus(message: string, type: 'success' | 'error'): void {
    this.statusMessage = message;
    this.statusType = type;
    setTimeout(() => this.statusMessage = '', 4000);
  }
}
