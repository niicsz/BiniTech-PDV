import { Component, EventEmitter, Output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ThemeService, ThemeColors } from '../services/theme.service';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-settings-modal',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="modal-overlay" (click)="close.emit()">
      <div class="modal-content" (click)="$event.stopPropagation()">
        <div class="modal-header">
          <h2>Configurações de Aparência</h2>
          <button class="close-btn" (click)="close.emit()">
            <span class="material-icons">close</span>
          </button>
        </div>
        
        <div class="modal-body">
          <p class="description">Personalize as cores do sistema ao seu gosto.</p>
          
          <div class="form-group">
            <label for="primaryColor">Cor Primária (Links, Botões)</label>
            <div class="color-picker-wrapper">
              <input type="color" id="primaryColor" [(ngModel)]="colors.primary" (change)="updateColor('primary', colors.primary!)">
              <span class="color-value">{{ colors.primary || '#1565c0' }}</span>
            </div>
          </div>
          
          <div class="form-group">
            <label for="headerBg">Cor do Cabeçalho</label>
            <div class="color-picker-wrapper">
              <input type="color" id="headerBg" [(ngModel)]="colors.headerBg" (change)="updateColor('headerBg', colors.headerBg!)">
              <span class="color-value">{{ colors.headerBg || '#0d47a1' }}</span>
            </div>
          </div>
        </div>

        <div class="modal-actions">
          <button class="btn btn-secondary" (click)="resetColors()">
            <span class="material-icons">restart_alt</span> Restaurar Padrões
          </button>
          <button class="btn btn-primary" (click)="close.emit()">
            <span class="material-icons">check</span> Concluir
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .modal-overlay {
      position: fixed;
      top: 0; left: 0; right: 0; bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }
    
    .modal-content {
      background: var(--surface);
      border-radius: var(--radius);
      width: 100%;
      max-width: 400px;
      box-shadow: var(--shadow);
      display: flex;
      flex-direction: column;
      overflow: hidden;
    }
    
    .modal-header {
      padding: 16px 24px;
      border-bottom: 1px solid var(--border);
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .modal-header h2 {
      font-size: 1.25rem;
      margin: 0;
      color: var(--text);
    }
    
    .close-btn {
      background: none;
      border: none;
      color: var(--text-secondary);
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 4px;
      border-radius: 50%;
    }
    
    .close-btn:hover {
      background: var(--bg);
      color: var(--text);
    }
    
    .modal-body {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    
    .description {
      color: var(--text-secondary);
      margin-bottom: 8px;
    }
    
    .form-group {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }
    
    .form-group label {
      font-weight: 500;
      color: var(--text);
    }
    
    .color-picker-wrapper {
      display: flex;
      align-items: center;
      gap: 12px;
      background: var(--bg);
      padding: 8px 12px;
      border-radius: var(--radius);
      border: 1px solid var(--border);
    }
    
    input[type="color"] {
      -webkit-appearance: none;
      border: none;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      padding: 0;
      cursor: pointer;
    }
    
    input[type="color"]::-webkit-color-swatch-wrapper {
      padding: 0;
    }
    
    input[type="color"]::-webkit-color-swatch {
      border: none;
      border-radius: 50%;
      box-shadow: 0 0 0 1px var(--border);
    }
    
    .color-value {
      font-family: monospace;
      color: var(--text-secondary);
      text-transform: uppercase;
    }
    
    .modal-actions {
      padding: 16px 24px;
      border-top: 1px solid var(--border);
      display: flex;
      justify-content: space-between;
      gap: 12px;
      background: var(--bg);
    }
    
    .btn {
      padding: 8px 16px;
      border-radius: var(--radius);
      font-weight: 500;
      cursor: pointer;
      display: flex;
      align-items: center;
      gap: 8px;
      border: none;
      font-size: 14px;
    }
    
    .btn-secondary {
      background: var(--surface);
      color: var(--text);
      border: 1px solid var(--border);
    }
    
    .btn-secondary:hover {
      background: var(--border);
    }
    
    .btn-primary {
      background: var(--primary);
      color: white;
    }
    
    .btn-primary:hover {
      background: var(--primary-dark);
    }
  `]
})
export class SettingsModalComponent {
  @Output() close = new EventEmitter<void>();
  
  themeService = inject(ThemeService);
  colors: ThemeColors = {};

  constructor() {
    this.colors = { ...this.themeService.customColors() };
    if (!this.colors.primary) this.colors.primary = '#1565c0';
    if (!this.colors.headerBg) this.colors.headerBg = '#0d47a1';
  }

  updateColor(key: keyof ThemeColors, value: string) {
    this.themeService.updateColor(key, value);
  }

  resetColors() {
    this.themeService.resetColors();
    this.colors = { primary: '#1565c0', headerBg: '#0d47a1' };
  }
}