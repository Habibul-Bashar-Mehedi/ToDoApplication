export type TaskStatus = 'PENDING' | 'COMPLETED' | 'CANCELLED';
export type TaskPriority = 'HIGH' | 'MEDIUM' | 'LOW';

export interface Task {
  id: number;
  title: string;
  description: string | null;
  moduleId: number;
  moduleName: string;
  createdBy: number;
  createdByDisplayName: string;
  lastEditedBy: number | null;
  lastEditedByDisplayName: string | null;
  status: TaskStatus;
  priority: TaskPriority;
  dueDate: string | null;
  reminder15min: boolean;
  reminder1hour: boolean;
  reminder1day: boolean;
  reminder2days: boolean;
  /** True when the requesting user has personally marked this task as done. */
  completedByMe: boolean;
  /** When the requesting user completed this task (null if not completed by them). */
  completedByMeAt: string | null;
  completedAt: string | null;
  cancelledAt: string | null;
  cancellationReason: string | null;
  tags: string[];
  createdAt: string;
  updatedAt: string;
}

export interface TaskCreateRequest {
  title: string;
  description?: string;
  moduleId: number;
  priority?: TaskPriority;
  dueDate?: string;
  reminder15min?: boolean;
  reminder1hour?: boolean;
  reminder1day?: boolean;
  reminder2days?: boolean;
  tags?: string[];
}

export interface TaskUpdateRequest {
  title?: string;
  description?: string;
  moduleId?: number;
  priority?: TaskPriority;
  dueDate?: string;
  reminder15min?: boolean;
  reminder1hour?: boolean;
  reminder1day?: boolean;
  reminder2days?: boolean;
  tags?: string[];
}

export interface TaskCancelRequest {
  reason?: string;
}

export interface TaskFilters {
  moduleId?: number;
  status?: TaskStatus;
  priority?: TaskPriority;
  tag?: string;
}
