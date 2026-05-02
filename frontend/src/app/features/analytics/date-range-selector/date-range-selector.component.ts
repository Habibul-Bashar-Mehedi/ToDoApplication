import {
  Component,
  ChangeDetectionStrategy,
  Output,
  EventEmitter,
  OnInit,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';

export type DatePreset = '7d' | '30d' | '90d' | 'custom';

export interface DateRangeSelection {
  from: string;
  to: string;
  preset: DatePreset;
}

/**
 * DateRangeSelectorComponent
 *
 * Provides preset date range buttons (last 7, 30, 90 days) and a custom
 * date range picker. Emits a DateRangeSelection whenever the selection changes.
 *
 * Req 9.2: User can select last 7 days, last 30 days, last 90 days, or custom range.
 * Req 9.1: Default is last 30 days.
 */
@Component({
  selector: 'app-date-range-selector',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './date-range-selector.component.html',
  styleUrl: './date-range-selector.component.scss',
  imports: [CommonModule, ReactiveFormsModule],
})
export class DateRangeSelectorComponent implements OnInit {
  /** Emits whenever the date range changes (preset or custom). */
  @Output() readonly rangeChange = new EventEmitter<DateRangeSelection>();

  protected readonly activePreset = signal<DatePreset>('30d');
  protected readonly showCustomPicker = computed(
    () => this.activePreset() === 'custom',
  );

  protected readonly customForm: FormGroup;

  /** Today's date in YYYY-MM-DD format for the max attribute on date inputs. */
  protected readonly today = this.toDateString(new Date());

  constructor(private readonly fb: FormBuilder) {
    const { from, to } = this.buildPresetRange('30d');
    this.customForm = this.fb.group({
      from: [from, [Validators.required]],
      to: [to, [Validators.required]],
    });
  }

  ngOnInit(): void {
    // Emit the default 30-day range on init
    this.emitPreset('30d');
  }

  // ─── Preset selection ─────────────────────────────────────────────────────

  protected selectPreset(preset: Exclude<DatePreset, 'custom'>): void {
    this.activePreset.set(preset);
    this.emitPreset(preset);
  }

  protected selectCustom(): void {
    this.activePreset.set('custom');
    // Populate the custom form with the current preset range as a starting point
    const { from, to } = this.buildPresetRange('30d');
    this.customForm.patchValue({ from, to }, { emitEvent: false });
  }

  protected applyCustomRange(): void {
    if (this.customForm.invalid) return;

    const { from, to } = this.customForm.value as { from: string; to: string };
    if (from > to) {
      this.customForm.get('to')?.setErrors({ beforeFrom: true });
      return;
    }

    this.rangeChange.emit({
      from: `${from}T00:00:00Z`,
      to: `${to}T23:59:59Z`,
      preset: 'custom',
    });
  }

  // ─── Helpers ──────────────────────────────────────────────────────────────

  private emitPreset(preset: Exclude<DatePreset, 'custom'>): void {
    const { from, to } = this.buildPresetRange(preset);
    this.rangeChange.emit({
      from: `${from}T00:00:00Z`,
      to: `${to}T23:59:59Z`,
      preset,
    });
  }

  private buildPresetRange(preset: Exclude<DatePreset, 'custom'>): {
    from: string;
    to: string;
  } {
    const to = new Date();
    const from = new Date();

    switch (preset) {
      case '7d':
        from.setDate(from.getDate() - 6);
        break;
      case '30d':
        from.setDate(from.getDate() - 29);
        break;
      case '90d':
        from.setDate(from.getDate() - 89);
        break;
    }

    return { from: this.toDateString(from), to: this.toDateString(to) };
  }

  private toDateString(date: Date): string {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
  }

  /** Accessible label for each preset button. */
  protected presetLabel(preset: DatePreset): string {
    switch (preset) {
      case '7d':  return 'Last 7 days';
      case '30d': return 'Last 30 days';
      case '90d': return 'Last 90 days';
      case 'custom': return 'Custom range';
    }
  }
}
