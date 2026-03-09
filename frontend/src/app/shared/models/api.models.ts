export interface ProductDTO {
  id?: string;
  barcode?: string;
  description?: string;
  price?: number;
  stockQuantity?: number;
  active?: boolean;
}

export interface CreateProductDTO {
  barcode: string;
  description: string;
  price: number;
  stockQuantity: number;
}

export interface SaleDTO {
  id?: string;
  items?: SaleItemDTO[];
  payments?: PaymentDTO[];
  totalAmount?: number;
  totalPaid?: number;
  change?: number;
  timestamp?: string;
}

export interface SaleItemDTO {
  productId?: string;
  productDescription?: string;
  quantity?: number;
  unitPrice?: number;
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

