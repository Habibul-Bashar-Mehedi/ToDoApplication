import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
} from '@angular/core';
import {
  FormBuilder,
  Validators,
  ReactiveFormsModule,
  AbstractControl,
  ValidationErrors,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { AuthService } from '../../../core/services/auth.service';
import { FieldErrorComponent } from '../../../shared/components/field-error/field-error.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

/**
 * Custom validator: password must be ≥8 chars, contain ≥1 letter, ≥1 digit.
 * Returns `{ passwordStrength: true }` when the password fails the rules.
 */
export function passwordStrengthValidator(
  control: AbstractControl,
): ValidationErrors | null {
  const value: string = control.value ?? '';

  if (!value) {
    // Let the `required` validator handle the empty case
    return null;
  }

  const hasMinLength = value.length >= 8;
  const hasLetter = /[a-zA-Z]/.test(value);
  const hasDigit = /\d/.test(value);

  if (hasMinLength && hasLetter && hasDigit) {
    return null;
  }

  return { passwordStrength: true };
}

@Component({
  selector: 'app-register',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    FieldErrorComponent,
    LoadingSpinnerComponent,
  ],
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly isLoading = signal(false);
  readonly submitted = signal(false);
  readonly serverError = signal<string | null>(null);
  readonly showPassword = signal(false);

  readonly form = this.fb.group({
    displayName: ['', [Validators.required, Validators.maxLength(100)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, passwordStrengthValidator]],
  });

  togglePasswordVisibility(): void {
    this.showPassword.update((v) => !v);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      // Mark all controls as touched to reveal field-level errors
      // without clearing any valid field values
      this.form.markAllAsTouched();
      return;
    }

    this.isLoading.set(true);
    this.serverError.set(null);

    const { email, password, displayName } = this.form.getRawValue();

    this.authService
      .register({
        email: email!,
        password: password!,
        displayName: displayName!,
      })
      .subscribe({
        next: () => {
          this.isLoading.set(false);
          this.submitted.set(true);
        },
        error: (err: unknown) => {
          this.isLoading.set(false);
          // Extract a human-readable message from the error response
          const message =
            (err as { error?: { message?: string } })?.error?.message ??
            'Registration failed. Please try again.';
          this.serverError.set(message);
          // Do NOT reset the form — preserve valid field values (req 1.11)
        },
      });
  }
}
