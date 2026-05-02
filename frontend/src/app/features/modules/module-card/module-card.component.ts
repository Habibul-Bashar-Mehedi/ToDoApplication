import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';

import { Module } from '../../../core/models/module.model';

/**
 * Card component for a single module.
 *
 * Displays module name, description, task count, visibility badge,
 * and action buttons (view, share, delete).
 *
 * - Req 6.1–6.8: module name, description, visibility displayed.
 * - Req 7.10: visibility badge shows PRIVATE/SHARED.
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 */
@Component({
  selector: 'app-module-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './module-card.component.html',
  styleUrl: './module-card.component.scss',
  imports: [CommonModule, RouterLink],
})
export class ModuleCardComponent {
  @Input({ required: true }) module!: Module;

  /** Task count for this module (passed in from parent). */
  @Input() taskCount = 0;

  /** Whether the current user is the owner of this module. */
  @Input() isOwner = false;

  /** Emits the module id when the user requests deletion. */
  @Output() delete = new EventEmitter<number>();

  protected onDelete(): void {
    this.delete.emit(this.module.id);
  }
}
