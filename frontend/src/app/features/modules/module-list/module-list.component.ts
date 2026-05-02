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
import { RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { Module } from '../../../core/models/module.model';
import { Task } from '../../../core/models/task.model';
import { ModuleService } from '../../../core/services/module.service';
import { TaskService } from '../../../core/services/task.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { ModuleCardComponent } from '../module-card/module-card.component';

/**
 * Module list page component.
 *
 * Displays all modules the current user owns or is a member of.
 * Allows creating new modules and deleting owned modules.
 *
 * - Req 6.1–6.8: module CRUD, name uniqueness, visibility, soft-delete.
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 * - Req 13.2: primary workflow (create task + share module) reachable in ≤3 clicks.
 */
@Component({
  selector: 'app-module-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './module-list.component.html',
  styleUrl: './module-list.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    ModalComponent,
    FieldErrorComponent,
    ModuleCardComponent
],
})
export class ModuleListComponent implements OnInit, OnDestroy {
  private readonly moduleService = inject(ModuleService);
  private readonly taskService = inject(TaskService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  // ─── Local state ────────────────────────────────────────────────────────────

  readonly modules = signal<Module[]>([]);
  readonly taskCounts = signal<Map<number, number>>(new Map());
  readonly isLoading = signal(true);

  // ─── Create module modal ─────────────────────────────────────────────────────

  readonly showCreateModal = signal(false);
  readonly isSubmitting = signal(false);

  readonly createForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    visibility: ['PRIVATE' as 'PRIVATE' | 'SHARED'],
  });

  // ─── Delete module modal ─────────────────────────────────────────────────────

  readonly showDeleteModal = signal(false);
  readonly deleteModuleId = signal<number | null>(null);
  readonly deleteModuleName = signal('');

  // ─── Computed ────────────────────────────────────────────────────────────────

  readonly currentUserId = computed(() => this.authService.currentUser()?.id);

  // ─── Lifecycle ───────────────────────────────────────────────────────────────

  ngOnInit(): void {
    // Subscribe to the modules stream
    this.moduleService.modules$
      .pipe(takeUntil(this.destroy$))
      .subscribe((modules) => this.modules.set(modules));

    // Load modules
    this.moduleService
      .getModules()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (modules) => {
          this.isLoading.set(false);
          this.loadTaskCounts(modules);
        },
        error: () => {
          this.toastService.error('Failed to load modules');
          this.isLoading.set(false);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Create module ───────────────────────────────────────────────────────────

  openCreateModal(): void {
    this.createForm.reset({ name: '', description: '', visibility: 'PRIVATE' });
    this.showCreateModal.set(true);
  }

  closeCreateModal(): void {
    this.showCreateModal.set(false);
    this.isSubmitting.set(false);
  }

  submitCreate(): void {
    if (this.createForm.invalid || this.isSubmitting()) return;

    const { name, description, visibility } = this.createForm.getRawValue();
    if (!name) return;

    this.isSubmitting.set(true);
    this.moduleService
      .createModule({
        name: name.trim(),
        description: description?.trim() || undefined,
        visibility: visibility ?? 'PRIVATE',
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (created) => {
          this.toastService.success(`Module "${created.name}" created`);
          this.closeCreateModal();
          // Refresh task counts for the new module (starts at 0)
          this.taskCounts.update((map) => {
            const next = new Map(map);
            next.set(created.id, 0);
            return next;
          });
        },
        error: (err) => {
          this.isSubmitting.set(false);
          if (err?.status === 409) {
            this.createForm.get('name')?.setErrors({ conflict: true });
          } else {
            this.toastService.error('Failed to create module');
          }
        },
      });
  }

  // ─── Delete module ───────────────────────────────────────────────────────────

  onDeleteModule(moduleId: number): void {
    const mod = this.modules().find((m) => m.id === moduleId);
    if (!mod) return;
    this.deleteModuleId.set(moduleId);
    this.deleteModuleName.set(mod.name);
    this.showDeleteModal.set(true);
  }

  confirmDelete(): void {
    const id = this.deleteModuleId();
    if (id == null) return;

    this.moduleService
      .deleteModule(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toastService.success('Module deleted');
          this.closeDeleteModal();
        },
        error: () => {
          this.toastService.error('Failed to delete module');
          this.closeDeleteModal();
        },
      });
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
    this.deleteModuleId.set(null);
    this.deleteModuleName.set('');
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  isOwner(module: Module): boolean {
    return module.ownerId === this.currentUserId();
  }

  getTaskCount(moduleId: number): number {
    return this.taskCounts().get(moduleId) ?? 0;
  }

  /** Track function for @for loops. */
  trackById(_index: number, module: Module): number {
    return module.id;
  }

  /**
   * Loads task counts per module by fetching all tasks and grouping by moduleId.
   * This avoids N+1 requests by using the existing task list endpoint.
   */
  private loadTaskCounts(modules: Module[]): void {
    if (modules.length === 0) return;

    this.taskService
      .getTasks()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (tasks: Task[]) => {
          const counts = new Map<number, number>();
          // Initialise all modules to 0
          modules.forEach((m) => counts.set(m.id, 0));
          // Count tasks per module
          tasks.forEach((t) => {
            const current = counts.get(t.moduleId) ?? 0;
            counts.set(t.moduleId, current + 1);
          });
          this.taskCounts.set(counts);
        },
        error: () => {
          // Non-critical — task counts just won't show
        },
      });
  }
}
