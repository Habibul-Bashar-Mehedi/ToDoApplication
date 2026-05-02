import {
  Component,
  Input,
  ChangeDetectionStrategy,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { AnalyticsService } from '../../../core/services/analytics.service';
import { ToastService } from '../../../shared/components/toast/toast.service';

/**
 * ExportButtonsComponent
 *
 * Provides PDF and CSV export buttons for the analytics dashboard.
 * On click, calls AnalyticsService which triggers a browser file download.
 * Shows an error toast if the export fails.
 *
 * Req 10.1: PDF export of all dashboard metrics.
 * Req 10.2: CSV export of raw tabular data.
 * Req 10.3: File download triggered in the browser.
 * Req 10.4: Error toast on failure; no navigation away from dashboard.
 * Req 13.1: WCAG 2.1 AA — accessible button labels, loading states.
 */
@Component({
  selector: 'app-export-buttons',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './export-buttons.component.html',
  styleUrl: './export-buttons.component.scss',
  imports: [CommonModule],
})
export class ExportButtonsComponent {
  /** ISO-8601 start date for the export, e.g. "2026-04-01T00:00:00Z". */
  @Input({ required: true }) from!: string;

  /** ISO-8601 end date for the export, e.g. "2026-04-30T23:59:59Z". */
  @Input({ required: true }) to!: string;

  private readonly analyticsService = inject(AnalyticsService);
  private readonly toastService = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  protected readonly isExportingPdf = signal(false);
  protected readonly isExportingCsv = signal(false);

  exportPdf(): void {
    if (this.isExportingPdf()) return;
    this.isExportingPdf.set(true);

    this.analyticsService
      .exportPdf(this.from, this.to)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isExportingPdf.set(false);
          this.toastService.success('PDF export downloaded successfully');
        },
        error: () => {
          this.isExportingPdf.set(false);
          this.toastService.error(
            'Failed to generate PDF export. Please try again.',
          );
        },
      });
  }

  exportCsv(): void {
    if (this.isExportingCsv()) return;
    this.isExportingCsv.set(true);

    this.analyticsService
      .exportCsv(this.from, this.to)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isExportingCsv.set(false);
          this.toastService.success('CSV export downloaded successfully');
        },
        error: () => {
          this.isExportingCsv.set(false);
          this.toastService.error(
            'Failed to generate CSV export. Please try again.',
          );
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
