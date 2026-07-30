import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { AnalyticsResponse, CreateUrlResponse, UrlShortenerService } from './url-shortener.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [FormsModule, CommonModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  destinationUrl = 'https://example.com';
  code = '';
  result: CreateUrlResponse | null = null;
  analytics: AnalyticsResponse | null = null;
  error = '';
  loading = false;

  constructor(private service: UrlShortenerService) {}

  createShortUrl(): void {
    this.loading = true;
    this.error = '';
    this.service.createShortUrl(this.destinationUrl).subscribe({
      next: (response) => {
        this.result = response;
        this.code = response.code;
        this.analytics = null;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to create a short URL.';
        this.loading = false;
      }
    });
  }

  loadAnalytics(): void {
    this.loading = true;
    this.error = '';
    this.service.getAnalytics(this.code).subscribe({
      next: (response) => {
        this.analytics = response;
        this.loading = false;
      },
      error: () => {
        this.error = 'Unable to load analytics.';
        this.loading = false;
      }
    });
  }
}
