import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatChipsModule } from '@angular/material/chips';
import { ProductService } from '../../../pos/services/product.service';
import { ProductDTO, CreateProductDTO } from '../../../shared/models/api.models';

@Component({
  selector: 'app-product-list',
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatTooltipModule, MatChipsModule
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
      next: (products) => this.products = products,
      error: () => this.showStatus('Erro ao carregar produtos.', 'error')
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
      this.productService.update(this.editingId, this.form).subscribe({
        next: () => {
          this.showStatus('Produto atualizado com sucesso!', 'success');
          this.showForm = false;
          this.loadProducts();
        },
        error: (err) => this.showStatus(err.error?.message || 'Erro ao atualizar.', 'error')
      });
    } else {
      this.productService.create(this.form).subscribe({
        next: () => {
          this.showStatus('Produto cadastrado com sucesso!', 'success');
          this.showForm = false;
          this.loadProducts();
        },
        error: (err) => this.showStatus(err.error?.message || 'Erro ao cadastrar.', 'error')
      });
    }
  }

  deleteProduct(id: string): void {
    if (confirm('Deseja realmente remover este produto?')) {
      this.productService.delete(id).subscribe({
        next: () => {
          this.showStatus('Produto removido.', 'success');
          this.loadProducts();
        },
        error: () => this.showStatus('Erro ao remover produto.', 'error')
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
