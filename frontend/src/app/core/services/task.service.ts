import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import {
  Task,
  TaskCreateRequest,
  TaskUpdateRequest,
  TaskCancelRequest,
  TaskFilters,
} from '../models/task.model';

const API_BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class TaskService {
  private readonly http = inject(HttpClient);

  /** BehaviorSubject holding the current list of tasks. */
  private readonly tasksSubject = new BehaviorSubject<Task[]>([]);

  /** Observable stream of the task list for components to subscribe to. */
  readonly tasks$ = this.tasksSubject.asObservable();

  /**
   * Fetches tasks from the API with optional filters and updates the local
   * BehaviorSubject. Filters map to query params: moduleId, status, priority, tag.
   */
  getTasks(filters?: TaskFilters): Observable<Task[]> {
    let params = new HttpParams();
    if (filters?.moduleId != null) {
      params = params.set('moduleId', filters.moduleId.toString());
    }
    if (filters?.status) {
      params = params.set('status', filters.status);
    }
    if (filters?.priority) {
      params = params.set('priority', filters.priority);
    }
    if (filters?.tag) {
      params = params.set('tag', filters.tag);
    }

    return this.http
      .get<Task[]>(`${API_BASE}/tasks`, { params })
      .pipe(tap((tasks) => this.tasksSubject.next(tasks)));
  }

  /** Fetches a single task by ID. Does not update the BehaviorSubject. */
  getTask(id: number): Observable<Task> {
    return this.http.get<Task>(`${API_BASE}/tasks/${id}`);
  }

  /**
   * Creates a new task and prepends it to the local task list on success.
   */
  createTask(request: TaskCreateRequest): Observable<Task> {
    return this.http.post<Task>(`${API_BASE}/tasks`, request).pipe(
      tap((created) => {
        const current = this.tasksSubject.getValue();
        this.tasksSubject.next([created, ...current]);
      }),
    );
  }

  /**
   * Updates an existing task and replaces it in the local task list on success.
   */
  updateTask(id: number, request: TaskUpdateRequest): Observable<Task> {
    return this.http.put<Task>(`${API_BASE}/tasks/${id}`, request).pipe(
      tap((updated) => this.replaceTask(updated)),
    );
  }

  /**
   * Toggles a task between COMPLETED and PENDING.
   * PATCH /tasks/{id}/complete — the API handles the toggle logic.
   */
  completeTask(id: number): Observable<Task> {
    return this.http
      .patch<Task>(`${API_BASE}/tasks/${id}/complete`, {})
      .pipe(tap((updated) => this.replaceTask(updated)));
  }

  /**
   * Cancels a task with an optional reason.
   * PATCH /tasks/{id}/cancel
   */
  cancelTask(id: number, request?: TaskCancelRequest): Observable<Task> {
    return this.http
      .patch<Task>(`${API_BASE}/tasks/${id}/cancel`, request ?? {})
      .pipe(tap((updated) => this.replaceTask(updated)));
  }

  /**
   * Soft-deletes a task (owner/admin only) and removes it from the local list.
   */
  deleteTask(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/tasks/${id}`).pipe(
      tap(() => {
        const current = this.tasksSubject.getValue();
        this.tasksSubject.next(current.filter((t) => t.id !== id));
      }),
    );
  }

  /** Replaces a task in the BehaviorSubject by matching ID. */
  private replaceTask(updated: Task): void {
    const current = this.tasksSubject.getValue();
    this.tasksSubject.next(
      current.map((t) => (t.id === updated.id ? updated : t)),
    );
  }
}
