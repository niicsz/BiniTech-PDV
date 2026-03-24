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

  login(username: string, password: string): Observable<AuthResponse> {
    const body: LoginRequest = { username, password };
    return this.http.post<AuthResponse>(`${this.authUrl}/login`, body).pipe(
      tap(res => this.storeAuth(res))
    );
  }

  register(username: string, password: string, role: 'ADMIN' | 'OPERATOR'): Observable<AuthResponse> {
    const body: RegisterRequest = { username, password, role };
    return this.http.post<AuthResponse>(`${this.authUrl}/register`, body);
  }

  refreshToken(): Observable<AuthResponse> {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) {
      this.logout();
      throw new Error('No refresh token');
    }
    const body: RefreshTokenRequest = { refreshToken };
    return this.http.post<AuthResponse>(`${this.authUrl}/refresh`, body).pipe(
      tap(res => this.storeAuth(res))
    );
  }

  logout(): void {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('username');
    localStorage.removeItem('role');
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

  private storeAuth(res: AuthResponse): void {
    localStorage.setItem('accessToken', res.accessToken);
    localStorage.setItem('refreshToken', res.refreshToken);
    localStorage.setItem('username', res.username);
    localStorage.setItem('role', res.role);
    this.loggedIn$.next(true);
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('accessToken');
  }
}

