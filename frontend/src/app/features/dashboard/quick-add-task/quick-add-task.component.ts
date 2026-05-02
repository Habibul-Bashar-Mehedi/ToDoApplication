import {
  Component,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  inject,
  signal,
  Input,
} from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { CommonModule } from '@angular/common';

import { TaskService } from '../../../core/services/task.service';
import { Module } from '../../../core/models/module.model';
import { Task, TaskPriority } from '../../../core/models/task.model';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

/**
 * Custom validator: the selected date must be in the future relative to now.
 * Returns `{ pastDate: true }` when the date is in the past.
 */
function futureDateValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null; // optional field — no date is fine
  const selected = new Date(control.value as string);
  if (isNaN(selected.getTime())) return null; // let the browser handle invalid dates
  return selected > new Date() ? null : { pastDate: true };
}

/**
 * Inline task creation form shown on the dashboard.
 *
 * Collects: title (required), module (required), priority (optional, defaults
 * to MEDIUM), and due date (optional, must be future).
 *
 * Emits `taskCreated` with the newly created Task on success so the parent
 * can refresh its summary counts without a full page reload.
 *
 * Requirement 13.2: primary workflow reachable in ≤ 3 clicks from dashboard.
 */
@Component({
  selector: 'app-quick-add-task',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './quick-add-task.component.html',
  styleUrl: './quick-add-task.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FieldErrorComponent,
    LoadingSpinnerComponent,
  ],
})
export class QuickAddTaskComponent {
  private readonly fb = inject(FormBuilder);
  private readonly taskService = inject(TaskService);

  /** List of modules the user can create tasks in (EDIT/ADMIN/owner). */
  @Input({ required: true }) modules: Module[] = [];

  /** Emitted when a task is successfully created. */
  @Output() taskCreated = new EventEmitter<Task>();

  readonly isLoading = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly isExpanded = signal(false);

  readonly priorities: TaskPriority[] = ['HIGH', 'MEDIUM', 'LOW'];

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    moduleId: [null as number | null, [Validators.required]],
    priority: ['MEDIUM' as TaskPriority],
    dueDate: ['', [futureDateValidator]],
  });

  /** Expand the form when the user clicks the trigger button. */
  expand(): void {
    this.isExpanded.set(true);
    // Focus the title input after the DOM updates
    setTimeout(() => {
      const titleInput = document.getElementById('quick-task-title');
      titleInput?.focus();
    }, 0);
  }

  /** Collapse and reset the form. */
  cancel(): void {
    this.isExpanded.set(false);
    this.form.reset({ priority: 'MEDIUM' });
    this.serverError.set(null);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.serverError.set(null);

    const { title, moduleId, priority, dueDate } = this.form.getRawValue();

    this.taskService
      .createTask({
        title: title!,
        moduleId: moduleId!,
        priority: priority ?? 'MEDIUM',
        dueDate: dueDate || undefined,
      })
      .subscribe({
        next: (task) => {
          this.isLoading.set(false);
          this.taskCreated.emit(task);
          this.cancel(); // collapse and reset after success
        },
        error: () => {
          this.isLoading.set(false);
          this.serverError.set('Failed to create task. Please try again.');
        },
      });
  }
}
