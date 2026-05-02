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
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';

import { Module, ModuleMember, PermissionLevel } from '../../../core/models/module.model';
import { ModuleService } from '../../../core/services/module.service';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../shared/components/toast/toast.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ModalComponent } from '../../../shared/components/modal/modal.component';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { TaskListComponent } from '../../tasks/task-list/task-list.component';
import { ModuleMemberListComponent } from '../module-member-list/module-member-list.component';

/**
 * Module detail page component.
 *
 * Shows:
 * - Module metadata (name, description, visibility) with inline edit for owner.
 * - Scoped task list filtered to this module.
 * - Member list (visible only to owner and ADMIN members) — Req 7.10.
 *
 * - Req 6.1–6.8: module CRUD, name uniqueness, visibility.
 * - Req 7.4–7.8: permission enforcement, member management.
 * - Req 7.10: member list visible to owner and ADMIN members only.
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all interactive elements.
 */
@Component({
  selector: 'app-module-detail',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './module-detail.component.html',
  styleUrl: './module-detail.component.scss',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    LoadingSpinnerComponent,
    ModalComponent,
    FieldErrorComponent,
    TaskListComponent,
    ModuleMemberListComponent,
  ],
})
export class ModuleDetailComponent implements OnInit, OnDestroy {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly moduleService = inject(ModuleService);
  private readonly authService = inject(AuthService);
  private readonly toastService = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly destroy$ = new Subject<void>();

  // ─── Local state ────────────────────────────────────────────────────────────

  readonly module = signal<Module | null>(null);
  readonly members = signal<ModuleMember[]>([]);
  readonly isLoading = signal(true);
  readonly isMembersLoading = signal(false);
  readonly activeTab = signal<'tasks' | 'members'>('tasks');

  // ─── Edit module modal ───────────────────────────────────────────────────────

  readonly showEditModal = signal(false);
  readonly isEditSubmitting = signal(false);

  readonly editForm = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    visibility: ['PRIVATE' as 'PRIVATE' | 'SHARED'],
  });

  // ─── Delete module modal ─────────────────────────────────────────────────────

  readonly showDeleteModal = signal(false);

  // ─── Computed ────────────────────────────────────────────────────────────────

  readonly currentUserId = computed(() => this.authService.currentUser()?.id);

  readonly isOwner = computed(() => {
    const mod = this.module();
    return mod != null && mod.ownerId === this.currentUserId();
  });

  /**
   * True when the current user is the owner OR has ADMIN permission.
   * Controls whether the user can change permissions or remove members.
   */
  readonly canManageMembers = computed(() => {
    if (this.isOwner()) return true;
    const uid = this.currentUserId();
    return this.members().some(
      (m) => m.userId === uid && m.permissionLevel === 'ADMIN',
    );
  });

  /**
   * True when the current user is any member (VIEW, EDIT, ADMIN) or the owner.
   * Controls visibility of the Members tab — all members can see who else is in the module.
   */
  readonly canViewMembers = computed(() => {
    if (this.isOwner()) return true;
    const uid = this.currentUserId();
    return this.members().some((m) => m.userId === uid);
  });

  readonly moduleId = computed(() => {
    const id = this.route.snapshot.paramMap.get('id');
    return id ? Number(id) : null;
  });

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
          // Load members after module is loaded
          this.loadMembers(id);
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

  // ─── Tab navigation ──────────────────────────────────────────────────────────

  setTab(tab: 'tasks' | 'members'): void {
    this.activeTab.set(tab);
  }

  // ─── Edit module ─────────────────────────────────────────────────────────────

  openEditModal(): void {
    const mod = this.module();
    if (!mod) return;
    this.editForm.reset({
      name: mod.name,
      description: mod.description ?? '',
      visibility: mod.visibility,
    });
    this.showEditModal.set(true);
  }

  closeEditModal(): void {
    this.showEditModal.set(false);
    this.isEditSubmitting.set(false);
  }

  submitEdit(): void {
    if (this.editForm.invalid || this.isEditSubmitting()) return;
    const mod = this.module();
    if (!mod) return;

    const { name, description, visibility } = this.editForm.getRawValue();
    if (!name) return;

    this.isEditSubmitting.set(true);
    this.moduleService
      .updateModule(mod.id, {
        name: name.trim(),
        description: description?.trim() || undefined,
      })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.module.set(updated);
          this.toastService.success('Module updated');
          this.closeEditModal();
        },
        error: (err) => {
          this.isEditSubmitting.set(false);
          if (err?.status === 409) {
            this.editForm.get('name')?.setErrors({ conflict: true });
          } else {
            this.toastService.error('Failed to update module');
          }
        },
      });
  }

  // ─── Delete module ───────────────────────────────────────────────────────────

  openDeleteModal(): void {
    this.showDeleteModal.set(true);
  }

  closeDeleteModal(): void {
    this.showDeleteModal.set(false);
  }

  confirmDelete(): void {
    const mod = this.module();
    if (!mod) return;

    this.moduleService
      .deleteModule(mod.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.toastService.success('Module deleted');
          this.router.navigate(['/modules']);
        },
        error: () => {
          this.toastService.error('Failed to delete module');
          this.closeDeleteModal();
        },
      });
  }

  // ─── Member management ───────────────────────────────────────────────────────

  onPermissionChanged(event: { userId: number; permissionLevel: PermissionLevel }): void {
    const mod = this.module();
    if (!mod) return;

    this.moduleService
      .updateMember(mod.id, event.userId, event.permissionLevel)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updated) => {
          this.members.update((list) =>
            list.map((m) => (m.userId === updated.userId ? updated : m)),
          );
          this.toastService.success('Permission updated');
        },
        error: () => this.toastService.error('Failed to update permission'),
      });
  }

  onMemberRemoved(userId: number): void {
    const mod = this.module();
    if (!mod) return;

    this.moduleService
      .removeMember(mod.id, userId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.members.update((list) => list.filter((m) => m.userId !== userId));
          this.toastService.success('Member removed');
        },
        error: () => this.toastService.error('Failed to remove member'),
      });
  }

  // ─── Helpers ─────────────────────────────────────────────────────────────────

  private loadMembers(moduleId: number): void {
    this.isMembersLoading.set(true);
    this.moduleService
      .getMembers(moduleId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (members) => {
          this.members.set(members);
          this.isMembersLoading.set(false);
        },
        error: () => {
          this.isMembersLoading.set(false);
          // Non-critical — members list just won't show
        },
      });
  }
}
