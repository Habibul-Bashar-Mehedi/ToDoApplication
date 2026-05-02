import {
  Component,
  ChangeDetectionStrategy,
  inject,
  signal,
  computed,
  OnInit,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';

import { TaskService } from '../../core/services/task.service';
import { ModuleService } from '../../core/services/module.service';
import { AuthService } from '../../core/services/auth.service';
import { Task } from '../../core/models/task.model';
import { Module } from '../../core/models/module.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { TaskSummaryWidgetComponent } from './task-summary-widget/task-summary-widget.component';
import { QuickAddTaskComponent } from './quick-add-task/quick-add-task.component';

/**
 * Dashboard — the primary landing page after login.
 *
 * Displays:
 * - A greeting with the user's display name
 * - TaskSummaryWidget: total / pending / completed counts + completion rate
 * - QuickAddTaskComponent: inline task creation (click 1 of the primary workflow)
 * - Quick-action shortcuts to Tasks and Modules (clicks 2–3 of the primary workflow)
 *
 * Requirement 13.2: the primary workflow (create task + share module) must be
 * reachable in at most 3 clicks from the dashboard.
 *   Click 1 → "Add a task" trigger in QuickAddTaskComponent (expands form)
 *   Click 2 → "Create task" submit button (task created)
 *   Click 3 → "Share" link on a module card (opens module share page)
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
  imports: [
    CommonModule,
    RouterLink,
    LoadingSpinnerComponent,
    TaskSummaryWidgetComponent,
    QuickAddTaskComponent,
  ],
})
export class DashboardComponent implements OnInit {
  private readonly taskService = inject(TaskService);
  private readonly moduleService = inject(ModuleService);
  protected readonly authService = inject(AuthService);

  readonly isLoadingTasks = signal(true);
  readonly isLoadingModules = signal(true);
  readonly tasks = signal<Task[]>([]);
  readonly modules = signal<Module[]>([]);

  /** Summary counts derived from the task list — completion is per-user. */
  readonly summary = computed(() => {
    const all = this.tasks();
    return {
      total: all.length,
      pending: all.filter((t) => t.status !== 'CANCELLED' && !t.completedByMe).length,
      completed: all.filter((t) => t.completedByMe).length,
      cancelled: all.filter((t) => t.status === 'CANCELLED').length,
    };
  });

  /** Modules the user can create tasks in (owned or EDIT/ADMIN member). */
  readonly writableModules = computed(() => this.modules());

  /** The 5 most recently created tasks the user hasn't personally completed yet. */
  readonly upcomingTasks = computed(() =>
    this.tasks()
      .filter((t) => t.status !== 'CANCELLED' && !t.completedByMe)
      .slice(0, 5),
  );

  ngOnInit(): void {
    this.loadTasks();
    this.loadModules();
  }

  private loadTasks(): void {
    this.isLoadingTasks.set(true);
    this.taskService.getTasks().subscribe({
      next: (tasks) => {
        this.tasks.set(tasks);
        this.isLoadingTasks.set(false);
      },
      error: () => {
        this.isLoadingTasks.set(false);
      },
    });
  }

  private loadModules(): void {
    this.isLoadingModules.set(true);
    this.moduleService.getModules().subscribe({
      next: (modules) => {
        this.modules.set(modules);
        this.isLoadingModules.set(false);
      },
      error: () => {
        this.isLoadingModules.set(false);
      },
    });
  }

  /**
   * Called when QuickAddTaskComponent emits a newly created task.
   * Prepends the task to the local list so summary counts update instantly
   * without a full reload.
   */
  onTaskCreated(task: Task): void {
    this.tasks.update((current) => [task, ...current]);
  }
}
