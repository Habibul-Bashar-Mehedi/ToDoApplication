import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { passwordStrengthValidator } from '../register/register.component';

/**
 * ResetPasswordComponent handles two distinct flows on the same route:
 *
 * 1. **Forgot-password flow** (`/reset-password` with no `token` query param):
 *    Shows an email input. On submit, calls `AuthService.forgotPassword()` and
 *    displays a confirmation message.
 *
 * 2. **New-password flow** (`/reset-password?token=<uuid>`):
 *    Shows a new-password form. On submit, calls `AuthService.resetPassword()`
 *    with the token from the query param and displays a success state with a
 *    link to sign in.
 */
@Component({
  selector: 'app-reset-password',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './reset-password.component.html',
  styleUrl: './reset-password.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    FieldErrorComponent,
    LoadingSpinnerComponent,
  ],
})
export class ResetPasswordComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  // ── Mode ──────────────────────────────────────────────────────────────────

  /** The reset token from the URL query param, or null when in forgot-password mode. */
  readonly token = signal<string | null>(null);

  // ── Shared state ──────────────────────────────────────────────────────────

  readonly isLoading = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly showPassword = signal(false);

  // ── Forgot-password form ──────────────────────────────────────────────────

  readonly forgotForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
  });

  /** True after a successful forgot-password submission. */
  readonly forgotSubmitted = signal(false);

  // ── New-password form ─────────────────────────────────────────────────────

  readonly newPasswordForm = this.fb.group({
    password: ['', [Validators.required, passwordStrengthValidator]],
  });

  /** True after a successful password reset. */
  readonly resetSuccess = signal(false);

  // ── Lifecycle ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const token: string | undefined =
      this.route.snapshot.queryParams['token'];
    this.token.set(token ?? null);
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  // ── Forgot-password submit ────────────────────────────────────────────────

  onForgotSubmit(): void {
    if (this.forgotForm.invalid) {
      this.forgotForm.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.serverError.set(null);

    const { email } = this.forgotForm.getRawValue();

    this.authService.forgotPassword({ email: email! }).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.forgotSubmitted.set(true);
      },
      error: () => {
        this.isLoading.set(false);
        // Always show the same confirmation message to avoid revealing
        // whether the email exists in the system (req 2.11).
        this.forgotSubmitted.set(true);
      },
    });
  }

  // ── New-password submit ───────────────────────────────────────────────────

  onNewPasswordSubmit(): void {
    if (this.newPasswordForm.invalid) {
      this.newPasswordForm.markAllAsTouched();
      return;
    }

    const token = this.token();
    if (!token) return;

    this.isLoading.set(true);
    this.serverError.set(null);

    const { password } = this.newPasswordForm.getRawValue();

    this.authService
      .resetPassword({ token, newPassword: password! })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.resetSuccess.set(true);
        },
        error: (err: unknown) => {
          this.isLoading.set(false);
          const message =
            (err as { error?: { message?: string } })?.error?.message ??
            'This reset link is invalid or has expired. Please request a new one.';
          this.serverError.set(message);
        },
      });
  }
}
