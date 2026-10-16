import { Component, inject } from '@angular/core';
import { ProductApiService } from '../data/product-api.service';
import { toSignal } from '@angular/core/rxjs-interop';
import { ProductCardComponent } from '../components/product-card.component';

@Component({
  selector: 'app-products',
  standalone: true,
  imports: [ProductCardComponent],
  templateUrl: './products.component.html',
  styleUrl: './products.component.scss'
})
export class ProductsComponent {
  private productApi = inject(ProductApiService)

  // NOTE: should pipe() the result to different structure to allow handling loading | error
  productsData = toSignal(this.productApi.getList());
}
