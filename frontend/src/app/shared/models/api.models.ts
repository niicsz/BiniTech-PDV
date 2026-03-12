export interface ProductDTO {
  id?: string;
  barcode?: string;
  description?: string;
  price?: number;
  costPrice?: number;
  stockQuantity?: number;
  active?: boolean;
  userId?: string;
}

export interface CreateProductDTO {
  barcode: string;
  description: string;
  price: number;
  costPrice: number;
  stockQuantity: number;
}

export interface SaleDTO {
  id?: string;
  items?: SaleItemDTO[];
  payments?: PaymentDTO[];
  totalAmount?: number;
  totalCost?: number;
  totalPaid?: number;
  change?: number;
  timestamp?: string;
  userId?: string;
}

export interface SaleItemDTO {
  productId?: string;
  productDescription?: string;
  quantity?: number;
  unitPrice?: number;
  costPrice?: number;
  subtotal?: number;
}

export interface CreateSaleDTO {
  items: CreateSaleItemDTO[];
  payments: PaymentDTO[];
}

export interface CreateSaleItemDTO {
  productId: string;
  quantity: number;
}

export interface PaymentDTO {
  method: PaymentMethodEnum;
  amount: number;
}

export type PaymentMethodEnum = 'CASH' | 'CREDIT_CARD' | 'DEBIT_CARD' | 'PIX';

export interface ErrorDTO {
  code?: string;
  message?: string;
  timestamp?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  role: 'ADMIN' | 'OPERATOR';
}

export interface RefreshTokenRequest {
  refreshToken: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  username: string;
  role: string;
}

