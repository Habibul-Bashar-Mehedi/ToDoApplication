import { Component, Input, ChangeDetectionStrategy } from '@angular/core';

/**
 * Accessible loading spinner.
 *
 * Usage:
 *   <app-loading-spinner />
 *   <app-loading-spinner label="Saving changes…" size="sm" />
 */
@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './loading-spinner.component.html',
  styleUrl: './loading-spinner.component.scss',
})
export class LoadingSpinnerComponent {
  /** Accessible label announced to screen readers. */
  @Input() label = 'Loading…';

  /** Visual size of the spinner. */
  @Input() size: 'sm' | 'md' | 'lg' = 'md';
}
