import { Injectable, OnDestroy, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import {
  Notification,
  NotificationPage,
  NotificationPreference,
  NotificationPreferenceRequest,
} from '../models/notification.model';

const API_BASE = '/api/v1';

/** Polling interval in milliseconds (30 seconds). */
const POLL_INTERVAL_MS = 30_000;

@Injectable({ providedIn: 'root' })
export class NotificationService implements OnDestroy {
  private readonly http = inject(HttpClient);

  /**
   * Signal holding the current unread notification count.
   * Updated by the 30-second polling loop and by mark-read operations.
   */
  readonly unreadCount = signal<number>(0);

  private pollingIntervalId: ReturnType<typeof setInterval> | null = null;

  constructor() {
    this.startPolling();
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  // ─── Polling ────────────────────────────────────────────────────────────────

  /**
   * Starts the 30-second polling loop that keeps `unreadCount` up to date.
   * Calls `GET /notifications/unread-count` (lightweight endpoint) on each tick.
   * An initial fetch is performed immediately on startup.
   */
  startPolling(): void {
    // Fetch immediately, then every 30 seconds
    this.fetchUnreadCount();
    this.pollingIntervalId = setInterval(
      () => this.fetchUnreadCount(),
      POLL_INTERVAL_MS,
    );
  }

  /** Stops the polling loop. Called on service destruction. */
  stopPolling(): void {
    if (this.pollingIntervalId !== null) {
      clearInterval(this.pollingIntervalId);
      this.pollingIntervalId = null;
    }
  }

  /**
   * Fetches the current unread count from the API and updates the signal.
   * Uses a lightweight query: GET /notifications?unread=true&size=0 returns
   * the total count in the page metadata without loading notification bodies.
   */
  private fetchUnreadCount(): void {
    const params = new HttpParams()
      .set('unread', 'true')
      .set('size', '1')
      .set('page', '0');

    this.http
      .get<NotificationPage>(`${API_BASE}/notifications`, { params })
      .subscribe({
        next: (page) => this.unreadCount.set(page.totalElements),
        error: () => {
          // Silently ignore polling errors to avoid flooding the console
        },
      });
  }

  // ─── Notification CRUD ──────────────────────────────────────────────────────

  /**
   * Fetches a paginated list of notifications for the current user,
   * ordered by `created_at` descending (newest first).
   */
  getNotifications(page = 0, size = 20): Observable<NotificationPage> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<NotificationPage>(`${API_BASE}/notifications`, {
      params,
    });
  }

  /**
   * Marks a single notification as read and decrements the unread count signal.
   */
  markRead(id: number): Observable<Notification> {
    return this.http
      .patch<Notification>(`${API_BASE}/notifications/${id}/read`, {})
      .pipe(
        tap((updated) => {
          if (!updated.isRead) return; // guard: only decrement if now read
          const current = this.unreadCount();
          if (current > 0) {
            this.unreadCount.set(current - 1);
          }
        }),
      );
  }

  /**
   * Marks all notifications as read and resets the unread count to 0.
   */
  markAllRead(): Observable<void> {
    return this.http
      .post<void>(`${API_BASE}/notifications/read-all`, {})
      .pipe(tap(() => this.unreadCount.set(0)));
  }

  // ─── Preferences ────────────────────────────────────────────────────────────

  /** Retrieves the current user's notification preferences. */
  getPreferences(): Observable<NotificationPreference> {
    return this.http.get<NotificationPreference>(
      `${API_BASE}/notifications/preferences`,
    );
  }

  /**
   * Updates the current user's notification preferences.
   * Accepts all 5 boolean flags (15min, 1hr, 1day, 2days, overdue).
   */
  updatePreferences(
    request: NotificationPreferenceRequest,
  ): Observable<NotificationPreference> {
    return this.http.put<NotificationPreference>(
      `${API_BASE}/notifications/preferences`,
      request,
    );
  }
}
