import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  OnInit,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { AuthService } from '../../../core/services/auth.service';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './verify-email.component.html',
  styleUrl: './verify-email.component.scss',
  imports: [RouterLink, LoadingSpinnerComponent],
})
export class VerifyEmailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  readonly status = signal<'loading' | 'success' | 'error' | 'no-token'>(
    'loading',
  );

  /** Email address to resend verification to (from query param, or entered by user). */
  readonly resendEmail = signal('');

  readonly resendStatus = signal<'idle' | 'sending' | 'sent' | 'error'>(
    'idle',
  );

  ngOnInit(): void {
    const token: string | undefined =
      this.route.snapshot.queryParams['token'];
    const email: string | undefined =
      this.route.snapshot.queryParams['email'];

    if (email) {
      this.resendEmail.set(email);
    }

    if (!token) {
      this.status.set('no-token');
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: () => this.status.set('success'),
      error: () => this.status.set('error'),
    });
  }

  resend(): void {
    if (!this.resendEmail()) {
      return;
    }

    this.resendStatus.set('sending');

    this.authService.resendVerification(this.resendEmail()).subscribe({
      next: () => this.resendStatus.set('sent'),
      error: () => this.resendStatus.set('error'),
    });
  }

  onEmailInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.resendEmail.set(input.value);
  }
}
