import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CreateUrlResponse {
  code: string;
  shortUrl: string;
  destinationUrl: string;
  createdAt: string;
}

export interface AnalyticsResponse {
  code: string;
  totalClicks: number;
  from: string;
  to: string;
}

@Injectable({ providedIn: 'root' })
export class UrlShortenerService {
  constructor(private http: HttpClient) {}

  createShortUrl(destinationUrl: string): Observable<CreateUrlResponse> {
    return this.http.post<CreateUrlResponse>('/api/v1/urls', { destinationUrl });
  }

  getAnalytics(code: string): Observable<AnalyticsResponse> {
    return this.http.get<AnalyticsResponse>(`/api/v1/urls/${code}/analytics`);
  }
}
