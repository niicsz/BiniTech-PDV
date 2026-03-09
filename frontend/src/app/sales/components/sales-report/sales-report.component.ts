import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SaleService } from '../../../pos/services/sale.service';
import { SaleDTO } from '../../../shared/models/api.models';

@Component({
  selector: 'app-sales-report',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './sales-report.component.html',
  styleUrls: ['./sales-report.component.scss']
})
export class SalesReportComponent implements OnInit {

  sales: SaleDTO[] = [];
  filterMode: 'today' | 'period' = 'today';
  startDate = '';
  endDate = '';
  totalRevenue = 0;

  constructor(private saleService: SaleService) {}

  ngOnInit(): void {
    this.loadToday();
  }

  loadToday(): void {
    this.filterMode = 'today';
    const today = new Date().toISOString().split('T')[0];
    this.saleService.listByDay(today).subscribe({
      next: (sales) => {
        this.sales = sales;
        this.calculateTotal();
      },
      error: () => this.sales = []
    });
  }

  loadByPeriod(): void {
    if (!this.startDate || !this.endDate) return;
    this.filterMode = 'period';
    this.saleService.listByPeriod(this.startDate, this.endDate).subscribe({
      next: (sales) => {
        this.sales = sales;
        this.calculateTotal();
      },
      error: () => this.sales = []
    });
  }

  calculateTotal(): void {
    this.totalRevenue = this.sales.reduce((sum, sale) => sum + (sale.totalAmount || 0), 0);
  }

  getPaymentLabel(method: string | undefined): string {
    const labels: Record<string, string> = {
      'CASH': 'Dinheiro',
      'CREDIT_CARD': 'Crédito',
      'DEBIT_CARD': 'Débito',
      'PIX': 'PIX'
    };
    return method ? (labels[method] || method) : '';
  }

  formatTimestamp(ts: string | undefined): string {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleString('pt-BR');
  }
}

