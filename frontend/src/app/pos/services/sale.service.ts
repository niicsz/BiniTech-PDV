import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { SaleDTO, CreateSaleDTO } from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class SaleService {

  private readonly baseUrl = '/api/sales';

  constructor(private http: HttpClient) {}

  create(sale: CreateSaleDTO): Observable<SaleDTO> {
    return this.http.post<SaleDTO>(this.baseUrl, sale);
  }

  getById(id: string): Observable<SaleDTO> {
    return this.http.get<SaleDTO>(`${this.baseUrl}/${id}`);
  }

  listByDay(date: string): Observable<SaleDTO[]> {
    const params = new HttpParams().set('date', date);
    return this.http.get<SaleDTO[]>(this.baseUrl, { params });
  }

  listByPeriod(startDate: string, endDate: string): Observable<SaleDTO[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);
    return this.http.get<SaleDTO[]>(this.baseUrl, { params });
  }

  listAll(): Observable<SaleDTO[]> {
    return this.http.get<SaleDTO[]>(this.baseUrl);
  }

  listDebtors(): Observable<SaleDTO[]> {
    return this.http.get<SaleDTO[]>(`${this.baseUrl}/debtors`);
  }

  markAsPaid(id: string): Observable<SaleDTO> {
    return this.http.patch<SaleDTO>(`${this.baseUrl}/${id}/mark-paid`, {});
  }
}
