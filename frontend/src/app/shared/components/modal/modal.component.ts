import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  ElementRef,
  ChangeDetectionStrategy,
  HostListener,
  inject,
} from '@angular/core';
import { A11yModule } from '@angular/cdk/a11y';

/**
 * Accessible modal dialog component.
 *
 * Features:
 * - Focus trap via Angular CDK `cdkTrapFocus`
 * - Restores focus to the trigger element on close
 * - Keyboard dismiss on Escape
 * - ARIA role="dialog" with aria-modal, aria-labelledby, aria-describedby
 * - Backdrop click to close (optional)
 *
 * Usage:
 * ```html
 * <app-modal
 *   [isOpen]="showModal"
 *   title="Confirm deletion"
 *   (closed)="showModal = false"
 * >
 *   <p>Are you sure you want to delete this item?</p>
 *   <ng-container modal-footer>
 *     <button (click)="showModal = false">Cancel</button>
 *     <button (click)="confirm()">Delete</button>
 *   </ng-container>
 * </app-modal>
 * ```
 */
@Component({
  selector: 'app-modal',
  standalone: true,
  imports: [A11yModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './modal.component.html',
  styleUrl: './modal.component.scss',
})
export class ModalComponent implements OnInit, OnDestroy {
  private readonly elementRef = inject(ElementRef);

  /** Controls whether the modal is visible. */
  @Input() isOpen = false;

  /** Modal heading text (used for aria-labelledby). */
  @Input() title = '';

  /** Whether clicking the backdrop closes the modal. */
  @Input() closeOnBackdrop = true;

  /** Emitted when the modal requests to be closed. */
  @Output() closed = new EventEmitter<void>();

  /** The element that had focus before the modal opened. */
  private previouslyFocusedElement: HTMLElement | null = null;

  ngOnInit(): void {
    // Capture the currently focused element so we can restore it on close
    this.previouslyFocusedElement = document.activeElement as HTMLElement;
  }

  ngOnDestroy(): void {
    this.restoreFocus();
  }

  /** Dismiss on Escape key. */
  @HostListener('keydown.escape')
  protected onEscape(): void {
    this.close();
  }

  protected onBackdropClick(event: MouseEvent): void {
    if (this.closeOnBackdrop && event.target === event.currentTarget) {
      this.close();
    }
  }

  protected close(): void {
    this.closed.emit();
    this.restoreFocus();
  }

  private restoreFocus(): void {
    if (
      this.previouslyFocusedElement &&
      typeof this.previouslyFocusedElement.focus === 'function'
    ) {
      // Use setTimeout to ensure the DOM has settled before restoring focus
      setTimeout(() => this.previouslyFocusedElement?.focus(), 0);
    }
  }
}
