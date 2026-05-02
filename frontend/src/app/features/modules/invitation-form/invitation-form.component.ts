import {
  Component,
  Output,
  EventEmitter,
  ChangeDetectionStrategy,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';

import { InvitationRequest, PermissionLevel } from '../../../core/models/module.model';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';

/**
 * Form component for inviting a user to a module.
 *
 * Collects email address and permission level, then emits the request
 * to the parent component for submission.
 *
 * - Req 7.1: invite by email with specified permission level.
 * - Req 7.9: only verified users can be invited (enforced by API).
 * - Req 13.1: WCAG 2.1 AA — accessible labels on all form elements.
 */
@Component({
  selector: 'app-invitation-form',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './invitation-form.component.html',
  styleUrl: './invitation-form.component.scss',
  imports: [CommonModule, ReactiveFormsModule, FieldErrorComponent],
})
export class InvitationFormComponent {
  private readonly fb = inject(FormBuilder);

  /** Emits the invitation request when the form is submitted. */
  @Output() invite = new EventEmitter<InvitationRequest>();

  readonly isSubmitting = signal(false);

  readonly form = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    permissionLevel: ['VIEW' as PermissionLevel, Validators.required],
  });

  protected readonly permissionOptions: { value: PermissionLevel; label: string; description: string }[] = [
    {
      value: 'VIEW',
      label: 'Can view',
      description: 'Can read tasks but cannot create, edit, or cancel them',
    },
    {
      value: 'EDIT',
      label: 'Can edit',
      description: 'Can create, edit, complete, and cancel tasks',
    },
    {
      value: 'ADMIN',
      label: 'Admin',
      description: 'Full edit access plus can invite members and delete the module',
    },
  ];

  /** Called by the parent after a successful invite to reset the form. */
  reset(): void {
    this.form.reset({ email: '', permissionLevel: 'VIEW' });
    this.isSubmitting.set(false);
  }

  /** Called by the parent when the API call fails. */
  setSubmitting(value: boolean): void {
    this.isSubmitting.set(value);
  }

  /** Sets a field-level error (e.g. user not found, already a member). */
  setEmailError(key: string): void {
    this.form.get('email')?.setErrors({ [key]: true });
    this.isSubmitting.set(false);
  }

  protected onSubmit(): void {
    if (this.form.invalid || this.isSubmitting()) return;

    const { email, permissionLevel } = this.form.getRawValue();
    if (!email || !permissionLevel) return;

    this.isSubmitting.set(true);
    this.invite.emit({ inviteeEmail: email.trim(), permissionLevel });
  }
}
