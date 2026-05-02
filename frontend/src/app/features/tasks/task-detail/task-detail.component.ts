import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';

import { Task } from '../../../core/models/task.model';
import { Module } from '../../../core/models/module.model';
import { TaskService } from '../../../core/services/task.service';
import { ModuleService } from '../../../core/services/module.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { TaskFormComponent } from '../task-form/task-form.component';

@Component({
  selector: 'app-task-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-detail.component.html',
  styleUrl: './task-detail.component.scss',
  imports: [
    RouterLink,
    DatePipe,
    BadgeComponent,
    ModalComponent,
    LoadingSpinnerComponent,
    TaskFormComponent,
  ],
})
export class TaskDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly taskService = inject(TaskService);
  private readonly moduleService = inject(ModuleService);
  private readonly toastService = inject(ToastService);

  // ─── Local state ────────────────────────────────────────────────────────────

  readonly task = signal<Task | null>(null);
  readonly modules = signal<Module[]>([]);
  readonly isLoading = signal(true);
  readonly isEditing = signal(false);
  readonly showCancelModal = signal(false);
  readonly cancelReason = signal('');
  readonly showDeleteModal = signal(false);
  readonly isActionLoading = signal(false);
  readonly notFound = signal(false);

  // ─── Computed ───────────────────────────────────────────────────────────────

  readonly isOverdue = computed(() => {
    const t = this.task();
    if (!t || t.status === 'CANCELLED' || t.completedByMe || !t.dueDate) return false;
    return new Date(t.dueDate) < new Date();
  });

  // ─── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);

    // Guard against non-numeric route params (e.g. "new" before route fix)
    if (!idParam || isNaN(id) || id <= 0) {
      this.router.navigate(['/tasks']);
      return;
    }

    forkJoin({
      modules: this.moduleService.getModules(),
      task: this.taskService.getTask(id),
    }).subscribe({
      next: ({ modules, task }) => {
        this.modules.set(modules);
        this.task.set(task);
        this.isLoading.set(false);
      },
      error: (err: unknown) => {
        const status = (err as { status?: number })?.status;
        if (status === 404) {
          this.notFound.set(true);
        } else {
          this.toastService.error('Failed to load task. Please try again.');
        }
        this.isLoading.set(false);
      },
    });
  }

  // ─── Actions ────────────────────────────────────────────────────────────────

  onComplete(): void {
    const t = this.task();
    if (!t) return;

    this.isActionLoading.set(true);
    this.taskService.completeTask(t.id).subscribe({
      next: (updated) => {
        this.task.set(updated);
        this.isActionLoading.set(false);
        const msg = updated.completedByMe
          ? 'Task marked as completed'
          : 'Task reverted to pending';
        this.toastService.success(msg);
      },
      error: () => {
        this.isActionLoading.set(false);
        this.toastService.error('Failed to update task status. Please try again.');
      },
    });
  }

  onCancelClick(): void {
    this.cancelReason.set('');
    this.showCancelModal.set(true);
  }

  onCancelConfirm(): void {
    const t = this.task();
    if (!t) return;

    this.isActionLoading.set(true);
    this.showCancelModal.set(false);

    const reason = this.cancelReason().trim();
    this.taskService.cancelTask(t.id, reason ? { reason } : undefined).subscribe({
      next: (updated) => {
        this.task.set(updated);
        this.isActionLoading.set(false);
        this.toastService.success('Task cancelled');
      },
      error: () => {
        this.isActionLoading.set(false);
        this.toastService.error('Failed to cancel task. Please try again.');
      },
    });
  }

  onCancelModalClose(): void {
    this.showCancelModal.set(false);
  }

  onDeleteClick(): void {
    this.showDeleteModal.set(true);
  }

  onDeleteConfirm(): void {
    const t = this.task();
    if (!t) return;

    this.isActionLoading.set(true);
    this.showDeleteModal.set(false);

    this.taskService.deleteTask(t.id).subscribe({
      next: () => {
        this.toastService.success('Task deleted');
        this.router.navigate(['/tasks']);
      },
      error: () => {
        this.isActionLoading.set(false);
        this.toastService.error('Failed to delete task. Please try again.');
      },
    });
  }

  onDeleteModalClose(): void {
    this.showDeleteModal.set(false);
  }

  onEdit(): void {
    this.isEditing.set(true);
  }

  onSaved(updatedTask: Task): void {
    this.task.set(updatedTask);
    this.isEditing.set(false);
    this.toastService.success('Task updated successfully');
  }

  onEditCancelled(): void {
    this.isEditing.set(false);
  }

  onCancelReasonChange(event: Event): void {
    const textarea = event.target as HTMLTextAreaElement;
    this.cancelReason.set(textarea.value);
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  get enabledReminders(): string[] {
    const t = this.task();
    if (!t) return [];
    const reminders: string[] = [];
    if (t.reminder15min) reminders.push('15 min');
    if (t.reminder1hour) reminders.push('1 hour');
    if (t.reminder1day) reminders.push('1 day');
    if (t.reminder2days) reminders.push('2 days');
    return reminders;
  }
}
