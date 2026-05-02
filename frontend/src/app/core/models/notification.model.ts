export type NotificationType =
  | 'REMINDER_15MIN'
  | 'REMINDER_1HOUR'
  | 'REMINDER_1DAY'
  | 'REMINDER_2DAYS'
  | 'OVERDUE'
  | 'INVITATION'
  | 'SYSTEM';

export interface Notification {
  id: number;
  userId: number;
  taskId: number | null;
  type: NotificationType;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export interface NotificationPreference {
  reminder15minEnabled: boolean;
  reminder1hourEnabled: boolean;
  reminder1dayEnabled: boolean;
  reminder2daysEnabled: boolean;
  overdueEnabled: boolean;
}

export interface NotificationPreferenceRequest {
  reminder15minEnabled: boolean;
  reminder1hourEnabled: boolean;
  reminder1dayEnabled: boolean;
  reminder2daysEnabled: boolean;
  overdueEnabled: boolean;
}

export interface NotificationPage {
  content: Notification[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
