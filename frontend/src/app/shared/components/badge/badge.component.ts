import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

export type TaskStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';
export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW';
export type BadgeVariant = TaskStatus | TaskPriority;

interface BadgeConfig {
  label: string;
  /** CSS modifier class applied to the badge element. */
  modifier: string;
  /** SVG path data for the icon (Feather-style). */
  iconPath: string;
}

const STATUS_CONFIG: Record<TaskStatus, BadgeConfig> = {
  PENDING: {
    label: 'Pending',
    modifier: 'badge--pending',
    iconPath: 'M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z M12 6v6l4 2',
  },
  COMPLETED: {
    label: 'Completed',
    modifier: 'badge--completed',
    iconPath: 'M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3',
  },
  CANCELLED: {
    label: 'Cancelled',
    modifier: 'badge--cancelled',
    iconPath:
      'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 1 1-18 0 9 9 0 0 1 18 0z',
  },
};

const PRIORITY_CONFIG: Record<TaskPriority, BadgeConfig> = {
  HIGH: {
    label: 'High',
    modifier: 'badge--high',
    iconPath: 'M12 9v4 M12 17h.01 M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z',
  },
  MEDIUM: {
    label: 'Medium',
    modifier: 'badge--medium',
    iconPath: 'M5 12h14',
  },
  LOW: {
    label: 'Low',
    modifier: 'badge--low',
    iconPath: 'M12 5v14 M19 12l-7 7-7-7',
  },
};

/**
 * Badge component for task status and priority indicators.
 *
 * Always uses both color AND an icon to convey meaning (WCAG 1.4.1).
 *
 * Usage:
 *   <app-badge variant="PENDING" />
 *   <app-badge variant="HIGH" />
 */
@Component({
  selector: 'app-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './badge.component.html',
  styleUrl: './badge.component.scss',
})
export class BadgeComponent {
  @Input({ required: true }) variant!: BadgeVariant;

  protected get config(): BadgeConfig {
    return (
      STATUS_CONFIG[this.variant as TaskStatus] ??
      PRIORITY_CONFIG[this.variant as TaskPriority]
    );
  }
}
