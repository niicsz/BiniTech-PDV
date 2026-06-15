import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { AdminService, TenantDTO, TenantUserDTO } from './admin.service';

const PLAN_LABELS: Record<string, string> = {
  starter: 'Starter',
  pro: 'Pro',
  enterprise: 'Enterprise',
  free: 'Cortesia',
};

const PLAN_MRR: Record<string, number> = {
  starter: 99,
  pro: 199,
  enterprise: 349,
  free: 0,
};

const PLAN_OPERATOR_LIMIT: Record<string, number> = {
  starter: 1,
  pro: 3,
  enterprise: 10,
  free: Number.POSITIVE_INFINITY,
};

const STATUS_LABEL: Record<string, string> = {
  PENDING_APPROVAL: 'Aguardando',
  ACTIVE: 'Ativo',
  BLOCKED: 'Bloqueado',
  CANCELLED: 'Cancelado',
};

const USER_ROLE_LABEL: Record<string, string> = {
  SUPER_ADMIN: 'Super Admin',
  TENANT_ADMIN: 'Admin da loja',
  ADMIN: 'Administrador',
  OPERATOR: 'Operador',
};

@Component({
  selector: 'app-admin-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterLink,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule,
  ],
  template: `
    <div class="admin">
      <header class="admin-head">
        <div>
          <a routerLink="/pdv" class="back">← Voltar ao PDV</a>
          <h1>Painel do Super Admin</h1>
        </div>
        <button mat-stroked-button (click)="load()"><mat-icon>refresh</mat-icon> Atualizar</button>
      </header>

      <section class="stats">
        <div class="stat"><span class="num">{{ tenants().length }}</span><span class="lbl">Tenants</span></div>
        <div class="stat"><span class="num">{{ countByStatus('ACTIVE') }}</span><span class="lbl">Ativos</span></div>
        <div class="stat"><span class="num">{{ countByStatus('PENDING_APPROVAL') }}</span><span class="lbl">Aguardando</span></div>
        <div class="stat highlight"><span class="num">R$ {{ mrr() }}</span><span class="lbl">MRR estimado</span></div>
      </section>

      <div *ngIf="loading()" class="loading"><mat-spinner diameter="32"></mat-spinner></div>

      <table *ngIf="!loading()" class="tenant-table">
        <thead>
          <tr><th>Empresa</th><th>Slug</th><th>Plano</th><th>Status</th><th>E-mail</th><th>Ações</th></tr>
        </thead>
        <tbody>
          <ng-container *ngFor="let t of tenants()">
            <tr>
              <td>{{ t.name }}</td>
              <td><code>{{ t.slug }}</code></td>
              <td>{{ planLabel(t.planId) }}</td>
              <td><span class="badge" [class]="'st-' + t.status">{{ statusLabel(t.status) }}</span></td>
              <td>{{ t.billingEmail }}</td>
              <td class="actions">
                <button mat-button (click)="toggleUsers(t)">
                  <mat-icon>{{ expandedTenantId() === t.id ? 'expand_less' : 'group' }}</mat-icon>
                  Usuários
                </button>
                <button mat-flat-button color="primary" *ngIf="t.status === 'PENDING_APPROVAL'"
                        (click)="approve(t)">Aprovar</button>
                <button mat-flat-button color="primary" *ngIf="t.status === 'BLOCKED' || t.status === 'CANCELLED'"
                        (click)="activate(t)">Ativar pagamento</button>
                <button mat-stroked-button color="warn" *ngIf="t.status === 'ACTIVE'"
                        (click)="block(t)">Bloquear</button>
              </td>
            </tr>
            <tr *ngIf="expandedTenantId() === t.id" class="users-row">
              <td colspan="6">
                <div class="users-panel">
                  <div class="users-head">
                    <strong>Usuários do tenant</strong>
                    <span class="limit-chip" [class.over]="isOverLimit(t.planId)">
                      {{ tenantUsers().length }} / {{ operatorLimit(t.planId) }} usuários do plano
                    </span>
                  </div>
                  <div *ngIf="usersLoading()" class="users-loading"><mat-spinner diameter="22"></mat-spinner></div>
                  <table *ngIf="!usersLoading()" class="users-table">
                    <thead><tr><th>Usuário</th><th>Perfil</th></tr></thead>
                    <tbody>
                      <tr *ngFor="let u of tenantUsers()">
                        <td>{{ u.username }}</td>
                        <td><span class="role-badge">{{ roleLabel(u.role) }}</span></td>
                      </tr>
                      <tr *ngIf="tenantUsers().length === 0"><td colspan="2" class="empty">Nenhum usuário.</td></tr>
                    </tbody>
                  </table>
                </div>
              </td>
            </tr>
          </ng-container>
          <tr *ngIf="tenants().length === 0"><td colspan="6" class="empty">Nenhum tenant cadastrado.</td></tr>
        </tbody>
      </table>
    </div>
  `,
  styles: [`
    .admin { max-width:1100px; margin:0 auto; padding:32px 24px; }
    .admin-head { display:flex; justify-content:space-between; align-items:flex-end; margin-bottom:24px; }
    .back { color:#7c3aed; text-decoration:none; font-size:13px; }
    h1 { margin:6px 0 0; font-size:26px; }
    .stats { display:flex; gap:16px; margin-bottom:28px; flex-wrap:wrap; }
    .stat { background:#fff; border-radius:12px; padding:18px 24px; min-width:140px; display:flex; flex-direction:column; box-shadow:0 2px 8px rgba(15,23,42,0.05); }
    .stat .num { font-size:26px; font-weight:700; }
    .stat .lbl { font-size:13px; color:#64748b; }
    .stat.highlight { background:linear-gradient(135deg,#7c3aed,#4f46e5); color:#fff; }
    .stat.highlight .lbl { color:#e0e7ff; }
    .loading { display:flex; justify-content:center; padding:40px; }
    .tenant-table { width:100%; border-collapse:collapse; background:#fff; border-radius:12px; overflow:hidden; box-shadow:0 2px 8px rgba(15,23,42,0.05); }
    th, td { text-align:left; padding:12px 16px; border-bottom:1px solid #f1f5f9; font-size:14px; }
    th { background:#f8fafc; color:#475569; font-weight:600; }
    code { background:#f1f5f9; padding:2px 6px; border-radius:4px; font-size:12px; }
    .badge { padding:3px 10px; border-radius:100px; font-size:12px; font-weight:600; }
    .st-ACTIVE { background:#dcfce7; color:#15803d; }
    .st-PENDING_APPROVAL { background:#fef9c3; color:#a16207; }
    .st-BLOCKED { background:#fee2e2; color:#b91c1c; }
    .st-CANCELLED { background:#e2e8f0; color:#475569; }
    .actions { display:flex; gap:8px; align-items:center; }
    .empty { text-align:center; color:#94a3b8; padding:32px; }
    .users-row td { background:#f8fafc; padding:0; }
    .users-panel { padding:16px 20px; }
    .users-head { display:flex; align-items:center; justify-content:space-between; margin-bottom:12px; }
    .limit-chip { background:#e0e7ff; color:#4338ca; padding:4px 12px; border-radius:100px; font-size:12px; font-weight:600; }
    .limit-chip.over { background:#fee2e2; color:#b91c1c; }
    .users-loading { display:flex; justify-content:center; padding:16px; }
    .users-table { width:100%; border-collapse:collapse; background:#fff; border-radius:8px; overflow:hidden; }
    .users-table th, .users-table td { text-align:left; padding:8px 14px; border-bottom:1px solid #f1f5f9; font-size:13px; }
    .users-table th { background:#eef2f7; color:#475569; font-weight:600; }
    .role-badge { background:#f1f5f9; color:#334155; padding:2px 10px; border-radius:100px; font-size:12px; font-weight:600; }
  `]
})
export class AdminDashboardComponent implements OnInit {
  private adminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);

  tenants = signal<TenantDTO[]>([]);
  loading = signal(false);
  expandedTenantId = signal<string | null>(null);
  tenantUsers = signal<TenantUserDTO[]>([]);
  usersLoading = signal(false);

  mrr = computed(() =>
    this.tenants()
      .filter(t => t.status === 'ACTIVE')
      .reduce((sum, t) => sum + (PLAN_MRR[t.planId] ?? 0), 0)
  );

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.expandedTenantId.set(null);
    this.tenantUsers.set([]);
    this.adminService.getTenants().subscribe({
      next: (data) => {
        this.tenants.set(data);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Erro ao carregar tenants.', 'OK', { duration: 4000 });
      }
    });
  }

  approve(t: TenantDTO): void {
    this.adminService.approveTenant(t.id).subscribe({
      next: () => {
        this.snackBar.open(`Tenant "${t.name}" aprovado. Credenciais enviadas por e-mail.`, 'OK', { duration: 5000 });
        this.load();
      },
      error: () => this.snackBar.open('Erro ao aprovar tenant.', 'OK', { duration: 4000 })
    });
  }

  block(t: TenantDTO): void {
    const reason = window.prompt(`Motivo do bloqueio de "${t.name}":`, 'Inadimplência');
    if (!reason) {
      return;
    }
    this.adminService.blockTenant(t.id, reason).subscribe({
      next: () => {
        this.snackBar.open(`Tenant "${t.name}" bloqueado.`, 'OK', { duration: 4000 });
        this.load();
      },
      error: () => this.snackBar.open('Erro ao bloquear tenant.', 'OK', { duration: 4000 })
    });
  }

  activate(t: TenantDTO): void {
    if (!window.confirm(`Confirmar pagamento e ativar a assinatura de "${t.name}"?`)) {
      return;
    }
    this.adminService.activateTenant(t.id).subscribe({
      next: () => {
        this.snackBar.open(`Assinatura de "${t.name}" ativada.`, 'OK', { duration: 4000 });
        this.load();
      },
      error: () => this.snackBar.open('Erro ao ativar assinatura.', 'OK', { duration: 4000 })
    });
  }

  countByStatus(status: string): number {
    return this.tenants().filter(t => t.status === status).length;
  }

  planLabel(planId: string): string {
    return PLAN_LABELS[planId] ?? planId;
  }

  statusLabel(status: string): string {
    return STATUS_LABEL[status] ?? status;
  }

  roleLabel(role: string): string {
    return USER_ROLE_LABEL[role] ?? role;
  }

  operatorLimit(planId: string): number {
    return PLAN_OPERATOR_LIMIT[planId] ?? 0;
  }

  isOverLimit(planId: string): boolean {
    return this.tenantUsers().length > this.operatorLimit(planId);
  }

  toggleUsers(t: TenantDTO): void {
    if (this.expandedTenantId() === t.id) {
      this.expandedTenantId.set(null);
      this.tenantUsers.set([]);
      return;
    }
    this.expandedTenantId.set(t.id);
    this.tenantUsers.set([]);
    this.usersLoading.set(true);
    this.adminService.getTenantUsers(t.id).subscribe({
      next: (users) => {
        this.tenantUsers.set(users);
        this.usersLoading.set(false);
      },
      error: () => {
        this.usersLoading.set(false);
        this.snackBar.open('Erro ao carregar usuários do tenant.', 'OK', { duration: 4000 });
      }
    });
  }
}
