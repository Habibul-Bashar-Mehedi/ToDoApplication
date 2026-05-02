import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  OnInit,
  OnDestroy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';

import { Module } from '../../../core/models/module.model';
import { TaskFilters, TaskStatus, TaskPriority } from '../../../core/models/task.model';

/**
 * Filter bar for the task list.
 *
 * Emits `filtersChanged` whenever any filter value changes (debounced for
 * the tag text input). Emits `showCancelledChanged` when the hide/show
 * cancelled toggle is flipped.
 *
 * Requirement 5.7: filter control to hide/show CANCELLED tasks.
 */
@Component({
  selector: 'app-task-filter-bar',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-filter-bar.component.html',
  styleUrl: './task-filter-bar.component.scss',
  imports: [CommonModule, FormsModule],
})
export class TaskFilterBarComponent implements OnInit, OnDestroy {
  /** Modules available for the module filter dropdown. */
  @Input() modules: Module[] = [];

  /** Current filter values (used to initialise the controls). */
  @Input() filters: TaskFilters = {};

  /** Whether cancelled tasks are currently shown. */
  @Input() showCancelled = false;

  /** Emitted when any filter changes. */
  @Output() filtersChanged = new EventEmitter<TaskFilters>();

  /** Emitted when the show/hide cancelled toggle changes. */
  @Output() showCancelledChanged = new EventEmitter<boolean>();

  protected selectedModuleId: number | '' = '';
  protected selectedStatus: TaskStatus | '' = '';
  protected selectedPriority: TaskPriority | '' = '';
  protected tagInput = '';

  protected readonly statuses: TaskStatus[] = ['PENDING', 'COMPLETED', 'CANCELLED'];
  protected readonly priorities: TaskPriority[] = ['HIGH', 'MEDIUM', 'LOW'];

  private readonly tagInput$ = new Subject<string>();
  private readonly destroy$ = new Subject<void>();

  ngOnInit(): void {
    // Initialise controls from the current filter values
    this.selectedModuleId = this.filters.moduleId ?? '';
    this.selectedStatus = this.filters.status ?? '';
    this.selectedPriority = this.filters.priority ?? '';
    this.tagInput = this.filters.tag ?? '';

    // Debounce tag input to avoid firing on every keystroke
    this.tagInput$
      .pipe(debounceTime(300), distinctUntilChanged(), takeUntil(this.destroy$))
      .subscribe(() => this.emitFilters());
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  protected onModuleChange(): void {
    this.emitFilters();
  }

  protected onStatusChange(): void {
    this.emitFilters();
  }

  protected onPriorityChange(): void {
    this.emitFilters();
  }

  protected onTagInput(): void {
    this.tagInput$.next(this.tagInput);
  }

  protected onShowCancelledChange(): void {
    this.showCancelledChanged.emit(!this.showCancelled);
  }

  protected clearFilters(): void {
    this.selectedModuleId = '';
    this.selectedStatus = '';
    this.selectedPriority = '';
    this.tagInput = '';
    this.emitFilters();
  }

  protected get hasActiveFilters(): boolean {
    return !!(
      this.selectedModuleId ||
      this.selectedStatus ||
      this.selectedPriority ||
      this.tagInput.trim()
    );
  }

  private emitFilters(): void {
    const filters: TaskFilters = {};
    if (this.selectedModuleId) filters.moduleId = Number(this.selectedModuleId);
    if (this.selectedStatus) filters.status = this.selectedStatus;
    if (this.selectedPriority) filters.priority = this.selectedPriority;
    if (this.tagInput.trim()) filters.tag = this.tagInput.trim();
    this.filtersChanged.emit(filters);
  }
}
