import {
  Component,
  Input,
  ChangeDetectionStrategy,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SummaryMetrics } from '../../../core/models/analytics.model';

/**
 * SummaryCardsComponent
 *
 * Displays four KPI cards:
 *  - Total tasks
 *  - Completion rate (as a percentage)
 *  - Overdue task count
 *  - Active module count
 *
 * Req 9.1: Default view shows these metrics for the last 30 days.
 * Req 13.1: WCAG 2.1 AA — each card has a descriptive aria-label.
 */
@Component({
  selector: 'app-summary-cards',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './summary-cards.component.html',
  styleUrl: './summary-cards.component.scss',
  imports: [CommonModule],
})
export class SummaryCardsComponent {
  @Input({ required: true }) set summary(value: SummaryMetrics) {
    this._summary.set(value);
  }

  protected readonly _summary = signal<SummaryMetrics>({
    totalTasks: 0,
    completionRate: 0,
    overdueCount: 0,
    activeModuleCount: 0,
  });

  /** Completion rate formatted as a percentage string, e.g. "72%" */
  protected readonly completionRateDisplay = computed(() => {
    const rate = this._summary().completionRate;
    return `${Math.round(rate * 100)}%`;
  });

  /** Colour class for the completion rate card based on the rate value. */
  protected readonly completionRateClass = computed(() => {
    const rate = this._summary().completionRate;
    if (rate >= 0.75) return 'card--success';
    if (rate >= 0.4)  return 'card--warning';
    return 'card--neutral';
  });

  /** Colour class for the overdue count card. */
  protected readonly overdueClass = computed(() => {
    const count = this._summary().overdueCount;
    return count > 0 ? 'card--danger' : 'card--neutral';
  });
}
