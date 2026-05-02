import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { Invitation } from '../../../core/models/module.model';
import { ModuleService } from '../../../core/services/module.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

/**
 * Invitations page — lists all pending invitations for the current user
 * and allows them to accept or reject each one.
 *
 * After accepting, the module becomes accessible in the user's module list.
 *
 * - Req 7.2: accept invitation → creates ModuleMember record.
 * - Req 7.3: reject invitation → marks REJECTED.
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 */
@Component({
  selector: 'app-invitations',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './invitations.component.html',
  styleUrl: './invitations.component.scss',
  imports: [CommonModule, RouterLink, LoadingSpinnerComponent],
})
export class InvitationsComponent implements OnInit, OnDestroy {
  private readonly moduleService = inject(ModuleService);
  private readonly toastService = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  readonly invitations = signal<Invitation[]>([]);
  readonly isLoading = signal(true);
  /** Tracks which invitation IDs are currently being processed. */
  readonly processingIds = signal<Set<number>>(new Set());

  ngOnInit(): void {
    this.loadInvitations();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Actions ─────────────────────────────────────────────────────────────

  accept(invitation: Invitation): void {
    this.setProcessing(invitation.id, true);

    this.moduleService
      .acceptInvitation(invitation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          // Remove from the pending list
          this.invitations.update((list) =>
            list.filter((inv) => inv.id !== invitation.id),
          );
          this.setProcessing(invitation.id, false);
          this.toastService.success(
            `You joined "${invitation.moduleName}" — it's now in your modules list.`,
          );
          // Refresh the module list so the newly joined module appears
          this.moduleService.getModules().pipe(takeUntil(this.destroy$)).subscribe();
        },
        error: () => {
          this.setProcessing(invitation.id, false);
          this.toastService.error('Failed to accept invitation. Please try again.');
        },
      });
  }

  reject(invitation: Invitation): void {
    this.setProcessing(invitation.id, true);

    this.moduleService
      .rejectInvitation(invitation.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.invitations.update((list) =>
            list.filter((inv) => inv.id !== invitation.id),
          );
          this.setProcessing(invitation.id, false);
          this.toastService.success('Invitation declined.');
        },
        error: () => {
          this.setProcessing(invitation.id, false);
          this.toastService.error('Failed to decline invitation. Please try again.');
        },
      });
  }

  isProcessing(id: number): boolean {
    return this.processingIds().has(id);
  }

  permissionLabel(level: string): string {
    switch (level) {
      case 'VIEW':  return 'Can view';
      case 'EDIT':  return 'Can edit';
      case 'ADMIN': return 'Admin';
      default:      return level;
    }
  }

  trackById(_index: number, invitation: Invitation): number {
    return invitation.id;
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────

  private loadInvitations(): void {
    this.moduleService
      .getInvitations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (invitations) => {
          this.invitations.set(invitations);
          this.isLoading.set(false);
        },
        error: () => {
          this.toastService.error('Failed to load invitations');
          this.isLoading.set(false);
        },
      });
  }

  private setProcessing(id: number, processing: boolean): void {
    this.processingIds.update((set) => {
      const next = new Set(set);
      if (processing) {
        next.add(id);
      } else {
        next.delete(id);
      }
      return next;
    });
  }
}
