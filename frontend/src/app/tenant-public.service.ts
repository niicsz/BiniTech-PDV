import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateTenantRequest {
  name: string;
  planId: string;
  billingEmail: string;
}

export interface TenantDTO {
  id: string;
  name: string;
  slug: string;
  status: string;
  planId: string;
  billingEmail: string;
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class TenantPublicService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/public/tenants';

  createTenant(payload: CreateTenantRequest): Observable<TenantDTO> {
    return this.http.post<TenantDTO>(this.baseUrl, payload);
  }
}
