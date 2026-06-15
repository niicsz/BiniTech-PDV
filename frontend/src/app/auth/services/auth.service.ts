import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap, BehaviorSubject } from 'rxjs';
import { AuthResponse, LoginRequest, RefreshTokenRequest, RegisterRequest } from '../../shared/models/api.models';

@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly authUrl = '/api/auth';
  private loggedIn$ = new BehaviorSubject<boolean>(this.hasToken());

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string, tenantId?: string): Observable<AuthResponse> {
    console.info('[AuthService] Realizando login para o usuário:', username);
    const body: LoginRequest = { username, password, ...(tenantId ? { tenantId } : {}) };
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, body).pipe(
      tap(res => {
        this.storeAuth(res);
        console.info('[AuthService] Login realizado com sucesso:', res.username, 'role:', res.role);
      })
    );
  }

  register(username: string, password: string, role: 'ADMIN' | 'OPERATOR'): Observable<AuthResponse> {
    console.info('[AuthService] Registrando usuário:', username, 'role:', role);
    const body: RegisterRequest = { username, password, role };
    return this.http.post<AuthResponse>(`${this.authUrl}/register`, body).pipe(
      tap(res => console.info('[AuthService] Usuário registrado com sucesso:', res.username))
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    console.info('[AuthService] Alterando senha do usuário:', this.getUsername());
    return this.http.post<void>(`${this.authUrl}/change-password`, { currentPassword, newPassword });
  }

  forgotPassword(tenantSlug: string, username: string): Observable<void> {
    return this.http.post<void>(`${this.authUrl}/forgot-password`, { tenantSlug, username });
  }

  resetPassword(token: string, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.authUrl}/reset-password`, { token, newPassword });
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      console.warn('[AuthService] Refresh token não encontrado, realizando logout');
      this.logout();
      throw new Error('No refresh token');
    }
    console.info('[AuthService] Renovando token de acesso');
    const body: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.authUrl}/refresh`, body).pipe(
      tap(res => {
        this.storeAuth(res);
        console.info('[AuthService] Token renovado com sucesso para:', res.username);
      })
    );
  }

  logout(): void {
    console.info('[AuthService] Realizando logout do usuário:', this.getUsername());
    const token = this.getToken();
    if (token) {
      this.http.post(`${this.authUrl}/logout`, {}).subscribe({
        error: () => console.warn('[AuthService] Erro ao invalidar token no servidor')
      });
    }
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
    localStorage.removeItem('tenantId');
    this.loggedIn$.next(false);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem('accessToken');
  }

  getRole(): string | null {
    return localStorage.getItem('role');
  }

  getUsername(): string | null {
    return localStorage.getItem('username');
  }

  isLoggedIn(): boolean {
    return this.hasToken();
  }

  get isAuthenticated$(): Observable<boolean> {
    return this.loggedIn$.asObservable();
  }

  isAdmin(): boolean {
    return this.getRole() === 'ADMIN';
  }

  isSuperAdmin(): boolean {
    return this.getRole() === 'SUPER_ADMIN';
  }

  isTenantAdmin(): boolean {
    return this.getRole() === 'TENANT_ADMIN';
  }

  canManageUsers(): boolean {
    const role = this.getRole();
    return role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'TENANT_ADMIN';
  }

  getTenantId(): string | null {
    return localStorage.getItem('tenantId');
  }

  private storeAuth(res: AuthResponse): void {
    localStorage.setItem('accessToken', res.accessToken);
    localStorage.setItem('refreshToken', res.refreshToken);
    localStorage.setItem('username', res.username);
    localStorage.setItem('role', res.role);
    if (res.tenantId) {
      localStorage.setItem('tenantId', res.tenantId);
    }
    this.loggedIn$.next(true);
    console.debug('[AuthService] Dados de autenticação armazenados para:', res.username);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('accessToken');
  }
}
