import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Notification } from '../../../core/models/notification.model';

/**
 * Displays a single notification row.
 *
 * - Req 8.7: links directly to the relevant task when taskId is present.
 * - Req 8.8: emits markRead on click so the parent can call the API.
 * - Unread items are visually distinguished with a bold style + accent dot.
 * - Color is never the sole indicator (icon + text label also used).
 */
@Component({
  selector: 'app-notification-item',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notification-item.component.html',
  styleUrl: './notification-item.component.scss',
  imports: [CommonModule, RouterLink, DatePipe],
})
export class NotificationItemComponent {
  @Input({ required: true }) notification!: Notification;

  /** Emits the notification id when the user clicks the item. */
  @Output() markRead = new EventEmitter<number>();

  protected get typeLabel(): string {
    const labels: Record<string, string> = {
      REMINDER_15MIN: '15-min reminder',
      REMINDER_1HOUR: '1-hour reminder',
      REMINDER_1DAY: '1-day reminder',
      REMINDER_2DAYS: '2-day reminder',
      OVERDUE: 'Overdue',
      INVITATION: 'Invitation',
      SYSTEM: 'System',
    };
    return labels[this.notification.type] ?? this.notification.type;
  }

  /** SVG path for the notification type icon. */
  protected get iconPath(): string {
    switch (this.notification.type) {
      case 'REMINDER_15MIN':
      case 'REMINDER_1HOUR':
      case 'REMINDER_1DAY':
      case 'REMINDER_2DAYS':
        // Clock icon
        return 'M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z M12 6v6l4 2';
      case 'OVERDUE':
        // Alert triangle icon
        return 'M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z M12 9v4 M12 17h.01';
      case 'INVITATION':
        // Users icon
        return 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2 M23 21v-2a4 4 0 0 0-3-3.87 M16 3.13a4 4 0 0 1 0 7.75';
      default:
        // Info icon
        return 'M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z M12 16v-4 M12 8h.01';
    }
  }

  protected onClick(): void {
    if (!this.notification.isRead) {
      this.markRead.emit(this.notification.id);
    }
  }
}
