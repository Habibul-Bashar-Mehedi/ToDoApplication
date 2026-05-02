import {
  Component,
  Input,
  ChangeDetectionStrategy,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ProductivityInsights } from '../../../core/models/analytics.model';

/**
 * ProductivityInsightsComponent
 *
 * Displays three productivity metrics:
 *  - Most productive day of the week
 *  - Peak completion hour of the day
 *  - Completion velocity (tasks completed per week)
 *
 * Also shows average completion time (days) passed in as a separate input.
 *
 * Req 9.7: Average completion time in days.
 * Req 9.9: Most productive day, peak hour, velocity.
 * Req 13.1: WCAG 2.1 AA — semantic markup, descriptive labels.
 */
@Component({
  selector: 'app-productivity-insights',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './productivity-insights.component.html',
  styleUrl: './productivity-insights.component.scss',
  imports: [CommonModule],
})
export class ProductivityInsightsComponent {
  @Input({ required: true }) set insights(value: ProductivityInsights) {
    this._insights.set(value);
  }

  @Input() set avgCompletionDays(value: number) {
    this._avgCompletionDays.set(value);
  }

  protected readonly _insights = signal<ProductivityInsights>({
    mostProductiveDayOfWeek: '—',
    peakCompletionHour: 0,
    completionVelocityPerWeek: 0,
  });

  protected readonly _avgCompletionDays = signal<number>(0);

  /** Format peak hour as a human-readable time range, e.g. "2 PM – 3 PM". */
  protected readonly peakHourDisplay = computed(() => {
    const hour = this._insights().peakCompletionHour;
    const start = this.formatHour(hour);
    const end = this.formatHour((hour + 1) % 24);
    return `${start} – ${end}`;
  });

  /** Velocity rounded to one decimal place. */
  protected readonly velocityDisplay = computed(() => {
    const v = this._insights().completionVelocityPerWeek;
    return v.toFixed(1);
  });

  /** Average completion days rounded to one decimal place. */
  protected readonly avgDaysDisplay = computed(() => {
    const d = this._avgCompletionDays();
    return d > 0 ? d.toFixed(1) : '—';
  });

  private formatHour(hour: number): string {
    if (hour === 0)  return '12 AM';
    if (hour === 12) return '12 PM';
    return hour < 12 ? `${hour} AM` : `${hour - 12} PM`;
  }
}
