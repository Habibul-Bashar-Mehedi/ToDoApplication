import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import {
  Notification,
  NotificationPage,
  NotificationPreference,
} from '../../../core/models/notification.model';
import { NotificationService } from '../../../core/services/notification.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { NotificationItemComponent } from '../notification-item/notification-item.component';

/**
 * Notification Centre page.
 *
 * - Req 8.7: paginated list in reverse-chronological order, each item links to task.
 * - Req 8.8: mark individual notification as read on item click.
 * - Req 8.9: mark-all-read button clears the unread badge.
 * - Req 8.11: 5-toggle preferences form (15min, 1hr, 1day, 2days, overdue).
 * - Req 13.1: WCAG 2.1 AA — accessible labels, aria-live regions.
 */
@Component({
  selector: 'app-notification-centre',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './notification-centre.component.html',
  styleUrl: './notification-centre.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    NotificationItemComponent,
  ],
})
export class NotificationCentreComponent implements OnInit, OnDestroy {
  private readonly notificationService = inject(NotificationService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  // ─── Notification list state ─────────────────────────────────────────────

  readonly notifications = signal<Notification[]>([]);
  readonly isLoading = signal(true);
  readonly isMarkingAllRead = signal(false);

  // ─── Pagination state ────────────────────────────────────────────────────

  readonly currentPage = signal(0);
  readonly totalPages = signal(0);
  readonly totalElements = signal(0);
  readonly pageSize = 20;

  readonly hasPreviousPage = computed(() => this.currentPage() > 0);
  readonly hasNextPage = computed(
    () => this.currentPage() < this.totalPages() - 1,
  );

  // ─── Preferences state ───────────────────────────────────────────────────

  readonly preferencesForm: FormGroup = this.fb.group({
    reminder15minEnabled: [true],
    reminder1hourEnabled: [true],
    reminder1dayEnabled: [true],
    reminder2daysEnabled: [true],
    overdueEnabled: [true],
  });

  readonly isLoadingPreferences = signal(true);
  readonly isSavingPreferences = signal(false);
  readonly preferencesSaved = signal(false);

  // ─── Lifecycle ───────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadPage(0);
    this.loadPreferences();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Notification list ───────────────────────────────────────────────────

  private loadPage(page: number): void {
    this.isLoading.set(true);
    this.notificationService
      .getNotifications(page, this.pageSize)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (result: NotificationPage) => {
          this.notifications.set(result.content);
          this.currentPage.set(result.number);
          this.totalPages.set(result.totalPages);
          this.totalElements.set(result.totalElements);
          this.isLoading.set(false);
        },
        error: () => {
          this.toastService.error('Failed to load notifications');
          this.isLoading.set(false);
        },
      });
  }

  onMarkRead(notificationId: number): void {
    this.notificationService
      .markRead(notificationId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          // Update the item in-place so the UI reflects the read state immediately
          this.notifications.update((list) =>
            list.map((n) => (n.id === updated.id ? updated : n)),
          );
        },
        error: () => this.toastService.error('Failed to mark notification as read'),
      });
  }

  onMarkAllRead(): void {
    this.isMarkingAllRead.set(true);
    this.notificationService
      .markAllRead()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // Mark all currently displayed notifications as read
          this.notifications.update((list) =>
            list.map((n) => ({ ...n, isRead: true })),
          );
          this.isMarkingAllRead.set(false);
          this.toastService.success('All notifications marked as read');
        },
        error: () => {
          this.toastService.error('Failed to mark all notifications as read');
          this.isMarkingAllRead.set(false);
        },
      });
  }

  // ─── Pagination ──────────────────────────────────────────────────────────

  goToPreviousPage(): void {
    if (this.hasPreviousPage()) {
      this.loadPage(this.currentPage() - 1);
    }
  }

  goToNextPage(): void {
    if (this.hasNextPage()) {
      this.loadPage(this.currentPage() + 1);
    }
  }

  // ─── Preferences ─────────────────────────────────────────────────────────

  private loadPreferences(): void {
    this.isLoadingPreferences.set(true);
    this.notificationService
      .getPreferences()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (prefs: NotificationPreference) => {
          this.preferencesForm.patchValue(prefs, { emitEvent: false });
          this.isLoadingPreferences.set(false);
        },
        error: () => {
          this.toastService.error('Failed to load notification preferences');
          this.isLoadingPreferences.set(false);
        },
      });
  }

  onSavePreferences(): void {
    if (this.preferencesForm.invalid || this.isSavingPreferences()) return;

    this.isSavingPreferences.set(true);
    this.preferencesSaved.set(false);

    this.notificationService
      .updatePreferences(this.preferencesForm.value)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isSavingPreferences.set(false);
          this.preferencesSaved.set(true);
          this.toastService.success('Notification preferences saved');
          // Clear the saved confirmation after 3 seconds
          setTimeout(() => this.preferencesSaved.set(false), 3000);
        },
        error: () => {
          this.toastService.error('Failed to save notification preferences');
          this.isSavingPreferences.set(false);
        },
      });
  }

  /** Track function for @for loops. */
  trackById(_index: number, notification: Notification): number {
    return notification.id;
  }
}
