import {
  Component,
  ChangeDetectionStrategy,
  Input,
  Output,
  EventEmitter,
  OnInit,
  inject,
  signal,
  computed,
} from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';

import { Task, TaskCreateRequest, TaskUpdateRequest } from '../../../core/models/task.model';
import { Module } from '../../../core/models/module.model';
import { TaskService } from '../../../core/services/task.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

/**
 * Custom validator: due date must be in the future.
 * Returns null if the field is empty (optional field).
 * Returns { futureDate: true } if the parsed date is in the past.
 *
 * Req 4.5
 */
export function futureDateValidator(
  control: AbstractControl,
): ValidationErrors | null {
  const value: string = control.value ?? '';

  if (!value) {
    return null;
  }

  const parsed = new Date(value);
  if (isNaN(parsed.getTime())) {
    return null;
  }

  if (parsed <= new Date()) {
    return { futureDate: true };
  }

  return null;
}

/**
 * Shared form component for creating and editing tasks.
 *
 * - Create mode: `task` input is null; submits via `taskService.createTask()`
 * - Edit mode: `task` input is provided; form is pre-populated; submits via `taskService.updateTask()`
 *
 * Req 4.1–4.7, 8.1, 13.1
 */
@Component({
  selector: 'app-task-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-form.component.html',
  styleUrl: './task-form.component.scss',
  imports: [
    ReactiveFormsModule,
    FieldErrorComponent,
    LoadingSpinnerComponent,
  ],
})
export class TaskFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly taskService = inject(TaskService);
  private readonly toastService = inject(ToastService);

  /** If provided, the form is in edit mode and will be pre-populated. */
  @Input() task: Task | null = null;

  /** List of modules for the module selector. */
  @Input() modules: Module[] = [];

  /** Optionally pre-select a module (used from task list). */
  @Input() preselectedModuleId: number | null = null;

  /** Emits the saved task on success. */
  @Output() saved = new EventEmitter<Task>();

  /** Emits when the user clicks Cancel. */
  @Output() cancelled = new EventEmitter<void>();

  // ─── Local state ────────────────────────────────────────────────────────────

  readonly isLoading = signal(false);

  /** Tags are managed as a signal array, not a form control. */
  readonly tags = signal<string[]>([]);

  /** The current value of the tag text input. */
  readonly tagInput = signal('');

  // ─── Computed labels ────────────────────────────────────────────────────────

  readonly formTitle = computed(() => (this.task ? 'Edit Task' : 'Create Task'));
  readonly submitLabel = computed(() =>
    this.task ? 'Save changes' : 'Create task',
  );

  // ─── Reactive form ──────────────────────────────────────────────────────────

  readonly form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(200)]],
    description: ['', [Validators.maxLength(2000)]],
    moduleId: ['' as string | number, [Validators.required]],
    priority: ['MEDIUM' as 'HIGH' | 'MEDIUM' | 'LOW', [Validators.required]],
    dueDate: ['', [futureDateValidator]],
    reminder15min: [false],
    reminder1hour: [false],
    reminder1day: [false],
    reminder2days: [false],
  });

  // ─── Lifecycle ──────────────────────────────────────────────────────────────

  ngOnInit(): void {
    if (this.task) {
      // Edit mode: populate form with existing task values
      this.form.patchValue({
        title: this.task.title,
        description: this.task.description ?? '',
        moduleId: this.task.moduleId.toString(),
        priority: this.task.priority,
        dueDate: this.task.dueDate
          ? this.toDatetimeLocalString(this.task.dueDate)
          : '',
        reminder15min: this.task.reminder15min,
        reminder1hour: this.task.reminder1hour,
        reminder1day: this.task.reminder1day,
        reminder2days: this.task.reminder2days,
      });
      this.tags.set([...this.task.tags]);
    } else if (this.preselectedModuleId != null) {
      // Create mode with pre-selected module
      this.form.patchValue({ moduleId: this.preselectedModuleId.toString() });
    }
  }

  // ─── Tag management ─────────────────────────────────────────────────────────

  onTagInputChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.tagInput.set(input.value);
  }

  onTagInputKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter') {
      event.preventDefault();
      this.addTag();
    }
  }

  addTag(): void {
    const value = this.tagInput().trim();
    if (!value) return;

    const current = this.tags();
    // Silently ignore duplicates
    if (current.includes(value)) {
      this.tagInput.set('');
      return;
    }

    this.tags.set([...current, value]);
    this.tagInput.set('');
  }

  removeTag(tag: string): void {
    this.tags.update((current) => current.filter((t) => t !== tag));
  }

  // ─── Form submission ─────────────────────────────────────────────────────────

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    const raw = this.form.getRawValue();

    if (this.task) {
      // Edit mode
      const request: TaskUpdateRequest = {
        title: raw.title ?? undefined,
        description: raw.description || undefined,
        moduleId: raw.moduleId ? Number(raw.moduleId) : undefined,
        priority: raw.priority ?? undefined,
        dueDate: raw.dueDate ? this.toIsoString(raw.dueDate) : undefined,
        reminder15min: raw.reminder15min ?? undefined,
        reminder1hour: raw.reminder1hour ?? undefined,
        reminder1day: raw.reminder1day ?? undefined,
        reminder2days: raw.reminder2days ?? undefined,
        tags: this.tags(),
      };

      this.taskService.updateTask(this.task.id, request).subscribe({
        next: (updatedTask) => {
          this.isLoading.set(false);
          this.toastService.success('Task updated successfully');
          this.saved.emit(updatedTask);
        },
        error: (err: unknown) => {
          this.isLoading.set(false);
          const message =
            (err as { error?: { message?: string } })?.error?.message ??
            'Failed to update task. Please try again.';
          this.toastService.error(message);
        },
      });
    } else {
      // Create mode
      const request: TaskCreateRequest = {
        title: raw.title!,
        description: raw.description || undefined,
        moduleId: Number(raw.moduleId!),
        priority: raw.priority ?? undefined,
        dueDate: raw.dueDate ? this.toIsoString(raw.dueDate) : undefined,
        reminder15min: raw.reminder15min ?? undefined,
        reminder1hour: raw.reminder1hour ?? undefined,
        reminder1day: raw.reminder1day ?? undefined,
        reminder2days: raw.reminder2days ?? undefined,
        tags: this.tags().length > 0 ? this.tags() : undefined,
      };

      this.taskService.createTask(request).subscribe({
        next: (createdTask) => {
          this.isLoading.set(false);
          this.toastService.success('Task created successfully');
          this.saved.emit(createdTask);
        },
        error: (err: unknown) => {
          this.isLoading.set(false);
          const message =
            (err as { error?: { message?: string } })?.error?.message ??
            'Failed to create task. Please try again.';
          this.toastService.error(message);
        },
      });
    }
  }

  onCancel(): void {
    this.cancelled.emit();
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  /**
   * Converts an ISO date string to the format required by datetime-local inputs:
   * "YYYY-MM-DDTHH:mm"
   */
  private toDatetimeLocalString(isoString: string): string {
    const date = new Date(isoString);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return (
      `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}` +
      `T${pad(date.getHours())}:${pad(date.getMinutes())}`
    );
  }

  /**
   * Converts a datetime-local string ("YYYY-MM-DDTHH:mm") to a full ISO string.
   */
  private toIsoString(datetimeLocal: string): string {
    return new Date(datetimeLocal).toISOString();
  }
}
