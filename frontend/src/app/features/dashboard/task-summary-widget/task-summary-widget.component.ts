import {
  Component,
  Input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { RouterLink } from '@angular/router';

export interface TaskSummary {
  total: number;
  pending: number;
  completed: number;
  cancelled: number;
}

/**
 * Displays a row of clickable summary stat cards: total, pending, completed,
 * and completion rate. Each card navigates to the relevant filtered task list
 * or analytics page.
 */
@Component({
  selector: 'app-task-summary-widget',
  standalone: true,
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-summary-widget.component.html',
  styleUrl: './task-summary-widget.component.scss',
})
export class TaskSummaryWidgetComponent {
  @Input({ required: true }) summary!: TaskSummary;

  /** Completion rate as a percentage string, e.g. "72%". */
  protected get completionRate(): string {
    const denominator = this.summary.total - this.summary.cancelled;
    if (denominator <= 0) return '0%';
    const rate = Math.round((this.summary.completed / denominator) * 100);
    return `${rate}%`;
  }
}
