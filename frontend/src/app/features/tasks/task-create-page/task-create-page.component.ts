import {
  Component,
  ChangeDetectionStrategy,
  OnInit,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';

import { Task } from '../../../core/models/task.model';
import { Module } from '../../../core/models/module.model';
import { ModuleService } from '../../../core/services/module.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { TaskFormComponent } from '../task-form/task-form.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

/**
 * Standalone page for creating a new task.
 * Wraps TaskFormComponent and handles navigation on save/cancel.
 *
 * Route: /tasks/new
 */
@Component({
  selector: 'app-task-create-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TaskFormComponent, LoadingSpinnerComponent],
  template: `
    <div class="task-create-page">
      <div class="task-create-page__header">
        <button
          type="button"
          class="task-create-page__back"
          (click)="goBack()"
          aria-label="Back to tasks"
        >
          ← Back to tasks
        </button>
      </div>

      @if (isLoading()) {
        <div class="task-create-page__loading">
          <app-loading-spinner label="Loading modules…" />
        </div>
      } @else {
        <div class="task-create-page__form-wrapper">
          <app-task-form
            [modules]="modules()"
            (saved)="onSaved($event)"
            (cancelled)="goBack()"
          />
        </div>
      }
    </div>
  `,
  styles: [`
    .task-create-page {
      max-width: 720px;
      margin: 0 auto;
      padding: 24px 16px;
    }

    .task-create-page__header {
      margin-bottom: 24px;
    }

    .task-create-page__back {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: none;
      border: none;
      cursor: pointer;
      color: #4f46e5;
      font-size: 0.875rem;
      font-weight: 500;
      padding: 0;
      text-decoration: none;

      &:hover {
        text-decoration: underline;
      }

      &:focus-visible {
        outline: 2px solid #4f46e5;
        outline-offset: 2px;
        border-radius: 4px;
      }
    }

    .task-create-page__loading {
      display: flex;
      justify-content: center;
      padding: 48px 0;
    }

    .task-create-page__form-wrapper {
      background: #ffffff;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      padding: 32px;
    }
  `],
})
export class TaskCreatePageComponent implements OnInit {
  private readonly router = inject(Router);
  private readonly moduleService = inject(ModuleService);
  private readonly toastService = inject(ToastService);

  readonly modules = signal<Module[]>([]);
  readonly isLoading = signal(true);

  ngOnInit(): void {
    this.moduleService.getModules().subscribe({
      next: (modules) => {
        this.modules.set(modules);
        this.isLoading.set(false);
      },
      error: () => {
        this.toastService.error('Failed to load modules');
        this.isLoading.set(false);
      },
    });
  }

  onSaved(task: Task): void {
    this.router.navigate(['/tasks', task.id]);
  }

  goBack(): void {
    this.router.navigate(['/tasks']);
  }
}
