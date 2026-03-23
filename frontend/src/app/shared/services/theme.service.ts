import { Injectable, signal } from '@angular/core';

export interface ThemeColors {
  primary?: string;
  headerBg?: string;
}

@Injectable({
  providedIn: 'root'
})
export class ThemeService {
  private readonly THEME_KEY = 'binitech-theme';
  private readonly COLORS_KEY = 'binitech-colors';
  
  public isDarkMode = signal<boolean>(false);
  public customColors = signal<ThemeColors>({});

  constructor() {
    this.loadTheme();
    this.loadColors();
  }

  toggleTheme() {
    const isDark = !this.isDarkMode();
    this.isDarkMode.set(isDark);
    this.applyTheme(isDark);
    localStorage.setItem(this.THEME_KEY, isDark ? 'dark' : 'light');
    
    setTimeout(() => this.applyCustomColors(this.customColors()), 0);
  }

  updateColor(key: keyof ThemeColors, value: string) {
    const currentColors = this.customColors();
    const newColors = { ...currentColors, [key]: value };
    this.customColors.set(newColors);
    localStorage.setItem(this.COLORS_KEY, JSON.stringify(newColors));
    this.applyCustomColors(newColors);
  }

  resetColors() {
    this.customColors.set({});
    localStorage.removeItem(this.COLORS_KEY);
    
    document.body.style.removeProperty('--primary');
    document.body.style.removeProperty('--primary-dark');
    document.body.style.removeProperty('--header-bg');
    
    this.applyTheme(this.isDarkMode());
  }

  private loadTheme() {
    const savedTheme = localStorage.getItem(this.THEME_KEY);
    const prefersDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
    
    const isDark = savedTheme ? savedTheme === 'dark' : prefersDark;
    this.isDarkMode.set(isDark);
    this.applyTheme(isDark);
  }

  private loadColors() {
    const savedColors = localStorage.getItem(this.COLORS_KEY);
    if (savedColors) {
      try {
        const colors = JSON.parse(savedColors) as ThemeColors;
        this.customColors.set(colors);
        this.applyCustomColors(colors);
      } catch (e) {
        console.error('Failed to load custom colors', e);
      }
    }
  }

  private applyTheme(isDark: boolean) {
    if (isDark) {
      document.body.classList.add('dark-theme');
    } else {
      document.body.classList.remove('dark-theme');
    }
  }

  private applyCustomColors(colors: ThemeColors) {
    if (colors.primary) {
      document.body.style.setProperty('--primary', colors.primary);
      document.body.style.setProperty('--primary-dark', this.adjustColorBrightness(colors.primary, -20));
    }
    if (colors.headerBg) {
      document.body.style.setProperty('--header-bg', colors.headerBg);
    }
  }
  
  private adjustColorBrightness(hex: string, percent: number): string {
    hex = hex.replace(/^#/, '');
    if (hex.length === 3) hex = hex[0]+hex[0]+hex[1]+hex[1]+hex[2]+hex[2];
    
    let r = parseInt(hex.substring(0, 2), 16);
    let g = parseInt(hex.substring(2, 4), 16);
    let b = parseInt(hex.substring(4, 6), 16);

    r = Math.min(255, Math.max(0, r + (r * percent / 100)));
    g = Math.min(255, Math.max(0, g + (g * percent / 100)));
    b = Math.min(255, Math.max(0, b + (b * percent / 100)));

    return `#${Math.round(r).toString(16).padStart(2, '0')}${Math.round(g).toString(16).padStart(2, '0')}${Math.round(b).toString(16).padStart(2, '0')}`;
  }
}
