/**
 * Barrel export for all shared components.
 * Import from this file to keep imports concise:
 *   import { BadgeComponent, ModalComponent } from '../shared/components';
 */
export { HeaderComponent } from './header/header.component';
export { SidebarComponent } from './sidebar/sidebar.component';
export { LoadingSpinnerComponent } from './loading-spinner/loading-spinner.component';
export { ModalComponent } from './modal/modal.component';
export { BadgeComponent } from './badge/badge.component';
export type { TaskStatus, TaskPriority, BadgeVariant } from './badge/badge.component';
export { ToastService } from './toast/toast.service';
export { ToastComponent } from './toast/toast.component';
export type { Toast, ToastType } from './toast/toast.service';
export { FieldErrorComponent } from './field-error/field-error.component';
