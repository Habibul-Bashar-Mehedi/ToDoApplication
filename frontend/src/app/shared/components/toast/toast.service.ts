import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface Toast {
  id: number;
  type: ToastType;
  message: string;
  /** Auto-dismiss duration in ms. 0 = no auto-dismiss. */
  duration: number;
}

let nextId = 0;

/**
 * Service for displaying non-blocking toast notifications.
 *
 * Usage (inject in any component or service):
 *   toastService.success('Task created successfully');
 *   toastService.error('Failed to save changes');
 *   toastService.warning('You have unsaved changes');
 *   toastService.info('Reminder set for tomorrow');
 */
@Injectable({ providedIn: 'root' })
export class ToastService {
  /** Signal holding the current list of active toasts. */
  readonly toasts = signal<Toast[]>([]);

  /** Show a success toast. */
  success(message: string, duration = 4000): void {
    this.add({ type: 'success', message, duration });
  }

  /** Show an error toast. */
  error(message: string, duration = 6000): void {
    this.add({ type: 'error', message, duration });
  }

  /** Show a warning toast. */
  warning(message: string, duration = 5000): void {
    this.add({ type: 'warning', message, duration });
  }

  /** Show an info toast. */
  info(message: string, duration = 4000): void {
    this.add({ type: 'info', message, duration });
  }

  /** Dismiss a specific toast by ID. */
  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  /** Dismiss all active toasts. */
  dismissAll(): void {
    this.toasts.set([]);
  }

  private add(options: Omit<Toast, 'id'>): void {
    const toast: Toast = { id: ++nextId, ...options };
    this.toasts.update((list) => [...list, toast]);

    if (toast.duration > 0) {
      setTimeout(() => this.dismiss(toast.id), toast.duration);
    }
  }
}
