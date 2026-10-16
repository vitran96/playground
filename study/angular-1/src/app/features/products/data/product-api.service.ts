import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';

export type ProductStatus = 'ACTIVE' | 'DRAFT' | 'ARCHIVED';

export type ProductsListRequest = {
  size?: number,
  page?: number,
  sort?: 'id' | 'name' | 'sku' | 'categoryId' | 'price' | 'stock' | 'status' | 'contactEmail' | 'tags' | 'createdAt',
  order?: 'asc' | 'desc',
  categoryId?: number[],
  status?: ProductStatus[],
};

export type ProductResponse = {
  id: number,
  name: string,
  sku: string,
  categoryId: number,
  price: number,
  stock: number,
  status: ProductStatus,
  contactEmail: string,
  tags: string[],
  createdAt: string,
};

export type ProductsListResponse = {
  items: ProductResponse[],
  total: number,
  page: number,
  size: number,
};

@Injectable({
  providedIn: 'root'
})
export class ProductApiService {
  private httpClient = inject(HttpClient);

  getList(params?: ProductsListRequest) {
    return this.httpClient.get<ProductsListResponse>('/api/products', {
      params
    });
  }
}
