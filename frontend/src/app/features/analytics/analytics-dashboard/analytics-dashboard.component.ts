import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';

import { AnalyticsService } from '../../../core/services/analytics.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { AnalyticsDashboard } from '../../../core/models/analytics.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

import {
  DateRangeSelectorComponent,
  DateRangeSelection,
} from '../date-range-selector/date-range-selector.component';
import { SummaryCardsComponent } from '../summary-cards/summary-cards.component';
import { StatusDonutChartComponent } from '../status-donut-chart/status-donut-chart.component';
import { CompletionTrendChartComponent } from '../completion-trend-chart/completion-trend-chart.component';
import { ModulePerformanceChartComponent } from '../module-performance-chart/module-performance-chart.component';
import { PriorityStackedBarChartComponent } from '../priority-stacked-bar-chart/priority-stacked-bar-chart.component';
import { ProductivityInsightsComponent } from '../productivity-insights/productivity-insights.component';
import { SharingMetricsComponent } from '../sharing-metrics/sharing-metrics.component';
import { ExportButtonsComponent } from '../export-buttons/export-buttons.component';

/**
 * AnalyticsDashboardComponent
 *
 * The main analytics page. Composes all analytics sub-components:
 *  - DateRangeSelectorComponent  (Req 9.2)
 *  - SummaryCardsComponent       (Req 9.1)
 *  - StatusDonutChartComponent   (Req 9.3)
 *  - CompletionTrendChartComponent (Req 9.4)
 *  - ModulePerformanceChartComponent (Req 9.5)
 *  - PriorityStackedBarChartComponent (Req 9.6)
 *  - ProductivityInsightsComponent (Req 9.7, 9.9)
 *  - SharingMetricsComponent     (Req 9.10)
 *  - ExportButtonsComponent      (Req 10.1–10.4)
 *
 * Req 9.11: Analytics data is refreshed on each dashboard load (API caches for 5 min).
 * Req 9.12: Data is scoped to the authenticated user's accessible modules (enforced by API).
 * Req 13.1: WCAG 2.1 AA — accessible headings, landmark regions, loading states.
 */
@Component({
  selector: 'app-analytics-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './analytics-dashboard.component.html',
  styleUrl: './analytics-dashboard.component.scss',
  imports: [
    CommonModule,
    LoadingSpinnerComponent,
    DateRangeSelectorComponent,
    SummaryCardsComponent,
    StatusDonutChartComponent,
    CompletionTrendChartComponent,
    ModulePerformanceChartComponent,
    PriorityStackedBarChartComponent,
    ProductivityInsightsComponent,
    SharingMetricsComponent,
    ExportButtonsComponent,
  ],
})
export class AnalyticsDashboardComponent implements OnInit, OnDestroy {
  private readonly analyticsService = inject(AnalyticsService);
  private readonly toastService = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  protected readonly isLoading = signal(true);
  protected readonly dashboard = signal<AnalyticsDashboard | null>(null);

  /** Current date range — updated by DateRangeSelectorComponent. */
  protected readonly currentFrom = signal('');
  protected readonly currentTo = signal('');

  ngOnInit(): void {
    // DateRangeSelectorComponent emits the default 30-day range on init,
    // which triggers onRangeChange → loadDashboard. Nothing to do here.
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /** Called when the user selects a new date range. */
  protected onRangeChange(selection: DateRangeSelection): void {
    this.currentFrom.set(selection.from);
    this.currentTo.set(selection.to);
    this.loadDashboard(selection.from, selection.to);
  }

  private loadDashboard(from: string, to: string): void {
    this.isLoading.set(true);

    this.analyticsService
      .getDashboard(from, to)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.dashboard.set(data);
          this.isLoading.set(false);
        },
        error: () => {
          this.toastService.error(
            'Failed to load analytics data. Please try again.',
          );
          this.isLoading.set(false);
        },
      });
  }
}
