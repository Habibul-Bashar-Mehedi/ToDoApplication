import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Task } from '../../../core/models/task.model';
import { BadgeComponent } from '../../../shared/components/badge/badge.component';

/**
 * Card component for a single task.
 *
 * - Req 5.6: COMPLETED tasks have strikethrough title; CANCELLED tasks are greyed-out.
 * - Req 4.1–4.7: displays title, description, module, priority, due date, tags.
 * - Req 13.1: all interactive elements have accessible labels.
 */
@Component({
  selector: 'app-task-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './task-card.component.html',
  styleUrl: './task-card.component.scss',
  imports: [CommonModule, RouterLink, BadgeComponent, DatePipe],
})
export class TaskCardComponent {
  @Input({ required: true }) task!: Task;

  /** Emits the task id when the user requests completion/revert. */
  @Output() complete = new EventEmitter<number>();

  /** Emits the task id when the user requests cancellation. */
  @Output() cancel = new EventEmitter<number>();

  /** Emits the task id when the user requests deletion. */
  @Output() delete = new EventEmitter<number>();

  protected get isOverdue(): boolean {
    if (!this.task.dueDate || this.task.status === 'CANCELLED' || this.task.completedByMe) return false;
    return new Date(this.task.dueDate) < new Date();
  }

  protected onComplete(): void {
    this.complete.emit(this.task.id);
  }

  protected onCancel(): void {
    this.cancel.emit(this.task.id);
  }

  protected onDelete(): void {
    this.delete.emit(this.task.id);
  }
}
