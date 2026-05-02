import {
  Component,
  Input,
  ChangeDetectionStrategy,
} from '@angular/core';
import { AbstractControl } from '@angular/forms';

/**
 * Displays validation error messages for a Reactive Form control.
 *
 * The component renders nothing when the control is valid or untouched.
 * The `id` input must match the `aria-describedby` attribute on the
 * associated form input so screen readers announce the error.
 *
 * Usage:
 * ```html
 * <label for="email">Email</label>
 * <input
 *   id="email"
 *   formControlName="email"
 *   aria-describedby="email-error"
 *   [attr.aria-invalid]="emailControl.invalid && emailControl.touched"
 * />
 * <app-field-error
 *   id="email-error"
 *   [control]="emailControl"
 *   [messages]="{
 *     required: 'Email is required',
 *     email: 'Enter a valid email address'
 *   }"
 * />
 * ```
 */
@Component({
  selector: 'app-field-error',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './field-error.component.html',
  styleUrl: './field-error.component.scss',
})
export class FieldErrorComponent {
  /**
   * The form control to inspect for errors.
   * Errors are only shown when the control is both invalid and touched/dirty.
   */
  @Input({ required: true }) control!: AbstractControl | null;

  /**
   * Map of validator key → human-readable error message.
   *
   * Built-in keys: required, email, minlength, maxlength, min, max, pattern
   * Custom keys: any key returned by a custom validator
   *
   * If a key is not present in this map, the raw error value is displayed
   * as a fallback (useful during development).
   */
  @Input() messages: Record<string, string> = {};

  /**
   * The `id` attribute placed on the error container.
   * Must match the `aria-describedby` on the associated input.
   */
  @Input() id = '';

  /** Returns true when errors should be displayed. */
  protected get shouldShow(): boolean {
    return !!(
      this.control &&
      this.control.invalid &&
      (this.control.touched || this.control.dirty)
    );
  }

  /** Returns the first active error message. */
  protected get errorMessage(): string {
    if (!this.control?.errors) return '';

    const errors = this.control.errors;
    const firstKey = Object.keys(errors)[0];

    if (this.messages[firstKey]) {
      return this.messages[firstKey];
    }

    // Built-in Angular validator fallbacks
    switch (firstKey) {
      case 'required':
        return 'This field is required.';
      case 'email':
        return 'Enter a valid email address.';
      case 'minlength': {
        const req = errors['minlength']?.requiredLength as number;
        return `Must be at least ${req} characters.`;
      }
      case 'maxlength': {
        const req = errors['maxlength']?.requiredLength as number;
        return `Must be no more than ${req} characters.`;
      }
      case 'min': {
        const min = errors['min']?.min as number;
        return `Must be at least ${min}.`;
      }
      case 'max': {
        const max = errors['max']?.max as number;
        return `Must be no more than ${max}.`;
      }
      case 'pattern':
        return 'Invalid format.';
      default:
        // Fallback: show the raw error value if it's a string
        return typeof errors[firstKey] === 'string'
          ? (errors[firstKey] as string)
          : 'Invalid value.';
    }
  }
}
