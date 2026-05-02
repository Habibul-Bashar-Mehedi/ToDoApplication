import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { AnalyticsDashboard } from '../models/analytics.model';

const API_BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private readonly http = inject(HttpClient);

  /**
   * Fetches the full analytics dashboard data for the given date range.
   * The API caches results for up to 5 minutes (Caffeine TTL).
   *
   * @param from ISO-8601 start date (inclusive), e.g. "2026-04-01T00:00:00Z"
   * @param to   ISO-8601 end date (inclusive), e.g. "2026-04-30T23:59:59Z"
   */
  getDashboard(from: string, to: string): Observable<AnalyticsDashboard> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<AnalyticsDashboard>(`${API_BASE}/analytics/dashboard`, {
      params,
    });
  }

  /**
   * Requests a PDF export of the analytics dashboard for the given date range.
   * The response is a binary blob; this method triggers a browser file download
   * using a temporary object URL.
   *
   * @param from ISO-8601 start date
   * @param to   ISO-8601 end date
   */
  exportPdf(from: string, to: string): Observable<Blob> {
    const params = new HttpParams()
      .set('format', 'pdf')
      .set('from', from)
      .set('to', to);

    return this.http
      .get(`${API_BASE}/analytics/export`, {
        params,
        responseType: 'blob',
      })
      .pipe(
        tap((blob) =>
          this.triggerDownload(blob, `analytics-${from}-${to}.pdf`, 'application/pdf'),
        ),
      );
  }

  /**
   * Requests a CSV export of the analytics dashboard for the given date range.
   * The response is a text/csv blob; this method triggers a browser file download.
   *
   * @param from ISO-8601 start date
   * @param to   ISO-8601 end date
   */
  exportCsv(from: string, to: string): Observable<Blob> {
    const params = new HttpParams()
      .set('format', 'csv')
      .set('from', from)
      .set('to', to);

    return this.http
      .get(`${API_BASE}/analytics/export`, {
        params,
        responseType: 'blob',
      })
      .pipe(
        tap((blob) =>
          this.triggerDownload(blob, `analytics-${from}-${to}.csv`, 'text/csv'),
        ),
      );
  }

  /**
   * Creates a temporary anchor element to trigger a browser file download
   * from a Blob, then immediately revokes the object URL to free memory.
   *
   * @param blob     The binary content to download
   * @param filename The suggested filename for the download dialog
   * @param mimeType MIME type used when constructing the Blob URL
   */
  private triggerDownload(blob: Blob, filename: string, mimeType: string): void {
    const typedBlob = new Blob([blob], { type: mimeType });
    const url = URL.createObjectURL(typedBlob);

    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.style.display = 'none';

    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);

    // Revoke the object URL after a short delay to allow the download to start
    setTimeout(() => URL.revokeObjectURL(url), 100);
  }
}
