import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface TenantDTO {
  id: string;
  name: string;
  slug: string;
  status: string;
  planId: string;
  billingEmail: string;
  trialEndsAt: string | null;
  blockedAt: string | null;
  blockReason: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TenantUserDTO {
  id: string;
  username: string;
  role: string;
}

@Injectable({ providedIn: 'root' })
export class AdminService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin';

  getTenants(): Observable<TenantDTO[]> {
    return this.http.get<TenantDTO[]>(`${this.base}/tenants`);
  }

  approveTenant(id: string): Observable<TenantDTO> {
    return this.http.post<TenantDTO>(`${this.base}/tenants/${id}/approve`, {});
  }

  blockTenant(id: string, reason: string): Observable<TenantDTO> {
    return this.http.post<TenantDTO>(`${this.base}/tenants/${id}/block`, { reason });
  }

  activateTenant(id: string): Observable<TenantDTO> {
    return this.http.post<TenantDTO>(`${this.base}/tenants/${id}/activate`, {});
  }

  getTenantUsers(id: string): Observable<TenantUserDTO[]> {
    return this.http.get<TenantUserDTO[]>(`${this.base}/tenants/${id}/users`);
  }
}
