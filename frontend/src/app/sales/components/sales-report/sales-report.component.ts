import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatChipsModule } from '@angular/material/chips';
import { SaleService } from '../../../pos/services/sale.service';
import { SaleDTO } from '../../../shared/models/api.models';

@Component({
  selector: 'app-sales-report',
  imports: [
    CommonModule, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatChipsModule
  ],
  templateUrl: './sales-report.component.html',
  styleUrls: ['./sales-report.component.scss']
})
export class SalesReportComponent implements OnInit {

  sales: SaleDTO[] = [];
  filterMode: 'today' | 'period' = 'today';
  startDate = '';
  endDate = '';
  totalRevenue = 0;
  totalCost = 0;
  totalProfit = 0;

  constructor(private saleService: SaleService) {}

  ngOnInit(): void {
    this.loadToday();
  }

  loadToday(): void {
    this.filterMode = 'today';
    const today = new Date().toISOString().split('T')[0];
    console.info('[SalesReport] Carregando vendas do dia:', today);
    this.saleService.listByDay(today).subscribe({
      next: (sales) => {
        this.sales = sales;
        this.calculateTotal();
        console.info('[SalesReport] Vendas do dia carregadas:', sales.length, 'receita:', this.totalRevenue);
      },
      error: () => {
        console.error('[SalesReport] Erro ao carregar vendas do dia');
        this.sales = [];
      }
    });
  }

  loadByPeriod(): void {
    if (!this.startDate || !this.endDate) return;
    this.filterMode = 'period';
    console.info('[SalesReport] Carregando vendas por período:', this.startDate, 'a', this.endDate);
    this.saleService.listByPeriod(this.startDate, this.endDate).subscribe({
      next: (sales) => {
        this.sales = sales;
        this.calculateTotal();
        console.info('[SalesReport] Vendas por período carregadas:', sales.length, 'receita:', this.totalRevenue);
      },
      error: () => {
        console.error('[SalesReport] Erro ao carregar vendas por período');
        this.sales = [];
      }
    });
  }

  calculateTotal(): void {
    this.totalRevenue = this.sales.reduce((sum, sale) => sum + (sale.totalAmount || 0), 0);
    this.totalCost = this.sales.reduce((sum, sale) => sum + (sale.totalCost || 0), 0);
    this.totalProfit = this.totalRevenue - this.totalCost;
  }

  getPaymentLabel(method: string | undefined): string {
    const labels: Record<string, string> = {
      'CASH': 'Dinheiro',
      'CREDIT_CARD': 'Crédito',
      'DEBIT_CARD': 'Débito',
      'PIX': 'PIX',
      'CREDIARIO': 'Crediário'
    };
    return method ? (labels[method] || method) : '';
  }

  formatTimestamp(ts: string | undefined): string {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleString('pt-BR');
  }

  getSaleProfit(sale: SaleDTO): number {
    return (sale.totalAmount || 0) - (sale.totalCost || 0);
  }
}
