import { Component, input } from '@angular/core';
import { ProductResponse } from '../data/product-api.service';

@Component({
  selector: 'app-product-card',
  standalone: true,
  imports: [],
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss'
})
export class ProductCardComponent {
  product = input.required<ProductResponse>()
}
