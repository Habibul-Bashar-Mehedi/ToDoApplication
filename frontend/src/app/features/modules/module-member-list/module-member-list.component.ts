import {
  Component,
  Input,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { ModuleMember, PermissionLevel } from '../../../core/models/module.model';

/**
 * Displays the list of module members with their permission levels.
 *
 * Visible only to the module owner and ADMIN members (enforced by parent).
 *
 * - Req 7.4–7.6: permission levels VIEW, EDIT, ADMIN displayed.
 * - Req 7.7: revoke member access.
 * - Req 7.8: cannot remove the module owner.
 * - Req 7.10: member list visible to owner and ADMIN members.
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 */
@Component({
  selector: 'app-module-member-list',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './module-member-list.component.html',
  styleUrl: './module-member-list.component.scss',
  imports: [CommonModule, FormsModule],
})
export class ModuleMemberListComponent {
  /** The list of current module members. */
  @Input({ required: true }) members: ModuleMember[] = [];

  /** The user ID of the module owner (cannot be removed). */
  @Input({ required: true }) ownerId!: number;

  /** The current user's ID (used to prevent self-removal). */
  @Input({ required: true }) currentUserId!: number;

  /**
   * Whether the current user can manage members (owner or ADMIN).
   * When false, the permission selector and remove button are hidden.
   */
  @Input() canManage = false;

  /** Emits { userId, permissionLevel } when a member's permission is changed. */
  @Output() permissionChanged = new EventEmitter<{
    userId: number;
    permissionLevel: PermissionLevel;
  }>();

  /** Emits the userId when a member is removed. */
  @Output() memberRemoved = new EventEmitter<number>();

  protected readonly permissionLevels: PermissionLevel[] = ['VIEW', 'EDIT', 'ADMIN'];

  protected isOwner(member: ModuleMember): boolean {
    return member.userId === this.ownerId;
  }

  protected canRemove(member: ModuleMember): boolean {
    // Cannot remove the module owner; cannot remove yourself
    return member.userId !== this.ownerId && member.userId !== this.currentUserId;
  }

  protected onPermissionChange(member: ModuleMember, level: PermissionLevel): void {
    this.permissionChanged.emit({ userId: member.userId, permissionLevel: level });
  }

  protected onRemove(member: ModuleMember): void {
    this.memberRemoved.emit(member.userId);
  }

  protected permissionLabel(level: PermissionLevel): string {
    switch (level) {
      case 'VIEW':  return 'Can view';
      case 'EDIT':  return 'Can edit';
      case 'ADMIN': return 'Admin';
    }
  }

  /** Track function for @for loops. */
  trackByUserId(_index: number, member: ModuleMember): number {
    return member.userId;
  }
}
