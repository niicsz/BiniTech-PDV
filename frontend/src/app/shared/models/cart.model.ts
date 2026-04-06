export interface CartItem {
  productId: string;
  barcode: string;
  description: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  stockQuantity?: number;
}

