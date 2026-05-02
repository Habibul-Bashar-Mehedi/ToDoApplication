import {
  Component,
  Input,
  ChangeDetectionStrategy,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { SharingMetrics } from '../../../core/models/analytics.model';

/**
 * SharingMetricsComponent
 *
 * Displays collaboration metrics:
 *  - Number of shared modules
 *  - Total collaboration activity count
 *  - Top collaborators list (name + activity count)
 *
 * Req 9.10: Sharing metrics — shared module count, collaboration activity,
 *           top collaborators.
 * Req 13.1: WCAG 2.1 AA — semantic markup, descriptive labels.
 */
@Component({
  selector: 'app-sharing-metrics',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './sharing-metrics.component.html',
  styleUrl: './sharing-metrics.component.scss',
  imports: [CommonModule],
})
export class SharingMetricsComponent {
  @Input({ required: true }) set metrics(value: SharingMetrics) {
    this._metrics.set(value);
  }

  protected readonly _metrics = signal<SharingMetrics>({
    sharedModuleCount: 0,
    collaborationActivityCount: 0,
    topCollaborators: [],
  });
}
