import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductDTO, CreateProductDTO } from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly baseUrl = '/api/products';

  constructor(private http: HttpClient) {}

  listAll(): Observable<ProductDTO[]> {
    return this.http.get<ProductDTO[]>(this.baseUrl);
  }

  getById(id: string): Observable<ProductDTO> {
    return this.http.get<ProductDTO>(`${this.baseUrl}/${id}`);
  }

  getByBarcode(barcode: string): Observable<ProductDTO> {
    return this.http.get<ProductDTO>(`${this.baseUrl}/barcode/${barcode}`);
  }

  create(product: CreateProductDTO): Observable<ProductDTO> {
    return this.http.post<ProductDTO>(this.baseUrl, product);
  }

  update(id: string, product: CreateProductDTO): Observable<ProductDTO> {
    return this.http.put<ProductDTO>(`${this.baseUrl}/${id}`, product);
  }

  delete(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}

