import {
  Component,
  OnInit,
  OnDestroy,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  ViewChild,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { Subject, takeUntil } from 'rxjs';

import { Module, Invitation, InvitationRequest, PermissionLevel } from '../../../core/models/module.model';
import { ModuleService } from '../../../core/services/module.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { InvitationFormComponent } from '../invitation-form/invitation-form.component';

/**
 * Module share page component.
 *
 * Allows module owners and ADMIN members to:
 * - Invite new users by email at a specified permission level.
 * - View and manage pending invitations for this module.
 *
 * - Req 7.1: invite by email, create pending Invitation, send in-app notification.
 * - Req 7.9: only verified users can be invited (enforced by API; UI shows error).
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 */
@Component({
  selector: 'app-module-share',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './module-share.component.html',
  styleUrl: './module-share.component.scss',
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    InvitationFormComponent,
  ],
})
export class ModuleShareComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly moduleService = inject(ModuleService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly destroy$ = new Subject<void>();

  @ViewChild(InvitationFormComponent) private invitationForm?: InvitationFormComponent;

  // ─── Local state ────────────────────────────────────────────────────────────

  readonly module = signal<Module | null>(null);
  readonly invitations = signal<Invitation[]>([]);
  readonly isLoading = signal(true);
  readonly isInvitationsLoading = signal(false);

  // ─── Computed ────────────────────────────────────────────────────────────────

  readonly moduleId = computed(() => {
    const id = this.route.snapshot.paramMap.get('id');
    return id ? Number(id) : null;
  });

  /** Pending invitations only — filter from the full list. */
  readonly pendingInvitations = computed(() =>
    this.invitations().filter((inv) => inv.status === 'PENDING'),
  );

  // ─── Lifecycle ───────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const id = this.moduleId();
    if (id == null) {
      this.router.navigate(['/modules']);
      return;
    }

    this.moduleService
      .getModule(id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (mod) => {
          this.module.set(mod);
          this.isLoading.set(false);
          this.loadInvitations(id);
        },
        error: () => {
          this.toastService.error('Failed to load module');
          this.isLoading.set(false);
          this.router.navigate(['/modules']);
        },
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ─── Invitation actions ──────────────────────────────────────────────────────

  /**
   * Handles the invite event emitted by InvitationFormComponent.
   * Calls ModuleService.inviteMember() and updates the pending list on success.
   */
  onInvite(request: InvitationRequest): void {
    const id = this.moduleId();
    if (id == null) return;

    this.moduleService
      .inviteMember(id, request)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (invitation) => {
          // Prepend the new invitation to the list
          this.invitations.update((list) => [invitation, ...list]);
          this.toastService.success(`Invitation sent to ${request.inviteeEmail}`);
          this.invitationForm?.reset();
        },
        error: (err) => {
          this.invitationForm?.setSubmitting(false);
          const status = err?.status;
          if (status === 404) {
            this.invitationForm?.setEmailError('notFound');
          } else if (status === 409) {
            // Could be already a member or already invited
            const message: string = err?.error?.message ?? '';
            if (message.toLowerCase().includes('already a member')) {
              this.invitationForm?.setEmailError('alreadyMember');
            } else {
              this.invitationForm?.setEmailError('alreadyInvited');
            }
          } else {
            this.toastService.error('Failed to send invitation. Please try again.');
          }
        },
      });
  }

  /**
   * Returns a human-readable label for a permission level.
   */
  permissionLabel(level: PermissionLevel): string {
    switch (level) {
      case 'VIEW':  return 'Can view';
      case 'EDIT':  return 'Can edit';
      case 'ADMIN': return 'Admin';
    }
  }

  /**
   * Returns the CSS modifier class for a permission level badge.
   */
  permissionClass(level: PermissionLevel): string {
    switch (level) {
      case 'VIEW':  return 'permission-badge--view';
      case 'EDIT':  return 'permission-badge--edit';
      case 'ADMIN': return 'permission-badge--admin';
    }
  }

  /**
   * Formats an ISO date string to a human-readable relative or absolute date.
   */
  formatDate(isoDate: string): string {
    const date = new Date(isoDate);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Yesterday';
    if (diffDays < 7) return `${diffDays} days ago`;

    return date.toLocaleDateString(undefined, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  }

  /** Track function for @for loops. */
  trackById(_index: number, invitation: Invitation): number {
    return invitation.id;
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private loadInvitations(moduleId: number): void {
    this.isInvitationsLoading.set(true);
    // The API returns invitations for the current user; we filter by moduleId
    // to show only invitations for this specific module.
    this.moduleService
      .getInvitations()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (invitations) => {
          // Filter to invitations for this module only
          this.invitations.set(
            invitations.filter((inv) => inv.moduleId === moduleId),
          );
          this.isInvitationsLoading.set(false);
        },
        error: () => {
          // Non-critical — pending list just won't show
          this.isInvitationsLoading.set(false);
        },
      });
  }
}
