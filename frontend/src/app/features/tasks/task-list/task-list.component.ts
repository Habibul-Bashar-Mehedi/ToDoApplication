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
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { Task, TaskFilters } from '../../../core/models/task.model';
import { Module } from '../../../core/models/module.model';
import { TaskService } from '../../../core/services/task.service';
import { ModuleService } from '../../../core/services/module.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { TaskFilterBarComponent } from '../task-filter-bar/task-filter-bar.component';
import { TaskCardComponent } from '../task-card/task-card.component';

/**
 * Task list page component.
 *
 * - Req 5.6: delegates strikethrough/greyed-out rendering to TaskCardComponent.
 * - Req 5.7: hide/show CANCELLED tasks via showCancelled signal.
 * - Req 4.1–4.7: task fields displayed via TaskCardComponent.
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 */
@Component({
  selector: 'app-task-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-list.component.html',
  styleUrl: './task-list.component.scss',
  imports: [
    CommonModule,
    RouterLink,
    FormsModule,
    LoadingSpinnerComponent,
    ModalComponent,
    TaskFilterBarComponent,
    TaskCardComponent,
  ],
})
export class TaskListComponent implements OnInit, OnDestroy {
  private readonly taskService = inject(TaskService);
  private readonly moduleService = inject(ModuleService);
  private readonly toastService = inject(ToastService);
  private readonly route = inject(ActivatedRoute);
  private readonly destroy$ = new Subject<void>();

  // ─── Local state ────────────────────────────────────────────────────────────

  readonly tasks = signal<Task[]>([]);
  readonly modules = signal<Module[]>([]);
  readonly filters = signal<TaskFilters>({});
  readonly showCancelled = signal(false);
  readonly isLoading = signal(true);

  // ─── Cancel modal state ──────────────────────────────────────────────────────

  readonly showCancelModal = signal(false);
  readonly cancelTaskId = signal<number | null>(null);
  readonly cancelReason = signal('');

  // ─── Delete modal state ──────────────────────────────────────────────────────

  readonly showDeleteModal = signal(false);
  readonly deleteTaskId = signal<number | null>(null);

  // ─── Computed ────────────────────────────────────────────────────────────────

  /**
   * Client-side filter: hide CANCELLED tasks when showCancelled is false.
   * Server-side filters (module, status, priority, tag) are applied via API.
   */
  readonly filteredTasks = computed(() => {
    const all = this.tasks();
    if (this.showCancelled()) return all;
    return all.filter((t) => t.status !== 'CANCELLED');
  });

  // ─── Lifecycle ───────────────────────────────────────────────────────────────

  ngOnInit(): void {
    // Read query params to pre-populate filters (e.g. from dashboard card clicks)
    const qp = this.route.snapshot.queryParamMap;
    const initialFilters: TaskFilters = {};
    const statusParam = qp.get('status');
    const moduleParam = qp.get('moduleId');
    const priorityParam = qp.get('priority');
    const tagParam = qp.get('tag');
    if (statusParam) initialFilters.status = statusParam as TaskFilters['status'];
    if (moduleParam) initialFilters.moduleId = Number(moduleParam);
    if (priorityParam) initialFilters.priority = priorityParam as TaskFilters['priority'];
    if (tagParam) initialFilters.tag = tagParam;
    this.filters.set(initialFilters);

    // Load modules for the filter bar
    this.moduleService
      .getModules()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (modules) => this.modules.set(modules),
        error: () => this.toastService.error('Failed to load modules'),
      });

    // Subscribe to the tasks stream to keep local state in sync
    this.taskService.tasks$
      .pipe(takeUntil(this.destroy$))
      .subscribe((tasks) => this.tasks.set(tasks));

    // Initial task load with any pre-populated filters
    this.loadTasks(initialFilters);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Filter events ───────────────────────────────────────────────────────────

  onFiltersChanged(newFilters: TaskFilters): void {
    this.filters.set(newFilters);
    this.loadTasks(newFilters);
  }

  onShowCancelledChanged(show: boolean): void {
    this.showCancelled.set(show);
  }

  // ─── Task action events ──────────────────────────────────────────────────────

  onComplete(taskId: number): void {
    this.taskService
      .completeTask(taskId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (task) => {
          const msg = task.completedByMe
            ? 'Task marked as complete'
            : 'Task reverted to pending';
          this.toastService.success(msg);
        },
        error: () => this.toastService.error('Failed to update task'),
      });
  }

  onCancel(taskId: number): void {
    this.cancelTaskId.set(taskId);
    this.cancelReason.set('');
    this.showCancelModal.set(true);
  }

  onDelete(taskId: number): void {
    this.deleteTaskId.set(taskId);
    this.showDeleteModal.set(true);
  }

  // ─── Cancel modal actions ────────────────────────────────────────────────────

  confirmCancel(): void {
    const id = this.cancelTaskId();
    if (id == null) return;

    const reason = this.cancelReason().trim();
    this.taskService
      .cancelTask(id, reason ? { reason } : undefined)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toastService.success('Task cancelled');
          this.closeCancelModal();
        },
        error: () => {
          this.toastService.error('Failed to cancel task');
          this.closeCancelModal();
        },
      });
  }

  closeCancelModal(): void {
    this.showCancelModal.set(false);
    this.cancelTaskId.set(null);
    this.cancelReason.set('');
  }

  // ─── Delete modal actions ────────────────────────────────────────────────────

  confirmDelete(): void {
    const id = this.deleteTaskId();
    if (id == null) return;

    this.taskService
      .deleteTask(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toastService.success('Task deleted');
          this.closeDeleteModal();
        },
        error: () => {
          this.toastService.error('Failed to delete task');
          this.closeDeleteModal();
        },
      });
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deleteTaskId.set(null);
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private loadTasks(filters: TaskFilters): void {
    this.isLoading.set(true);
    this.taskService
      .getTasks(filters)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => this.isLoading.set(false),
        error: () => {
          this.toastService.error('Failed to load tasks');
          this.isLoading.set(false);
        },
      });
  }

  /** Track function for @for loops. */
  trackById(_index: number, task: Task): number {
    return task.id;
  }
}
