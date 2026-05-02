import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { ToastService, Toast } from './toast.service';

@Component({
  selector: 'app-toast',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './toast.component.html',
  styleUrl: './toast.component.scss',
})
export class ToastComponent {
  protected readonly toastService = inject(ToastService);

  protected trackById(_index: number, toast: Toast): number {
    return toast.id;
  }

  protected getIconPath(type: Toast['type']): string {
    switch (type) {
      case 'success':
        return 'M22 11.08V12a10 10 0 1 1-5.93-9.14 M22 4L12 14.01l-3-3';
      case 'error':
        return 'M10 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2m7-2a9 9 0 1 1-18 0 9 9 0 0 1 18 0z';
      case 'warning':
        return 'M12 9v4 M12 17h.01 M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z';
      case 'info':
      default:
        return 'M12 16v-4 M12 8h.01 M12 2a10 10 0 1 0 0 20A10 10 0 0 0 12 2z';
    }
  }

  protected getAriaLabel(type: Toast['type']): string {
    switch (type) {
      case 'success': return 'Success';
      case 'error':   return 'Error';
      case 'warning': return 'Warning';
      case 'info':    return 'Information';
    }
  }
}
