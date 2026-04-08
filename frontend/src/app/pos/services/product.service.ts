import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { ProductDTO, CreateProductDTO } from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class ProductService {

  private readonly baseUrl = '/api/products';

  constructor(private http: HttpClient) {}

  listAll(): Observable<ProductDTO[]> {
    console.info('[ProductService] Listando todos os produtos');
    return this.http.get<ProductDTO[]>(this.baseUrl).pipe(
      tap(products => console.info('[ProductService] Produtos carregados:', products.length))
    );
  }

  getById(id: string): Observable<ProductDTO> {
    console.info('[ProductService] Buscando produto por id:', id);
    return this.http.get<ProductDTO>(`${this.baseUrl}/${id}`);
  }

  getByBarcode(barcode: string): Observable<ProductDTO> {
    console.info('[ProductService] Buscando produto por barcode:', barcode);
    return this.http.get<ProductDTO>(`${this.baseUrl}/barcode/${barcode}`);
  }

  create(product: CreateProductDTO): Observable<ProductDTO> {
    console.info('[ProductService] Criando produto:', product.description);
    return this.http.post<ProductDTO>(this.baseUrl, product).pipe(
      tap(created => console.info('[ProductService] Produto criado com sucesso:', created.id))
    );
  }

  update(id: string, product: CreateProductDTO): Observable<ProductDTO> {
    console.info('[ProductService] Atualizando produto:', id);
    return this.http.put<ProductDTO>(`${this.baseUrl}/${id}`, product).pipe(
      tap(() => console.info('[ProductService] Produto atualizado com sucesso:', id))
    );
  }

  delete(id: string): Observable<void> {
    console.info('[ProductService] Removendo produto:', id);
    return this.http.delete<void>(`${this.baseUrl}/${id}`).pipe(
      tap(() => console.info('[ProductService] Produto removido com sucesso:', id))
    );
  }
}
