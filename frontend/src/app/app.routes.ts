import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

/**
 * Application routes.
 *
 * Public routes (no guard): login, register, verify-email, reset-password.
 * Protected routes (authGuard): everything else, nested under a layout shell.
 *
 * Feature components are lazy-loaded so each phase can be implemented
 * independently without breaking the build.
 */
export const routes: Routes = [
  // Default redirect
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },

  // ── Public routes ──────────────────────────────────────────────────────────
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(
        (m) => m.LoginComponent,
      ),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(
        (m) => m.RegisterComponent,
      ),
  },
  {
    path: 'verify-email',
    loadComponent: () =>
      import('./features/auth/verify-email/verify-email.component').then(
        (m) => m.VerifyEmailComponent,
      ),
  },
  {
    path: 'reset-password',
    loadComponent: () =>
      import(
        './features/auth/reset-password/reset-password.component'
      ).then((m) => m.ResetPasswordComponent),
  },

  // ── Protected routes (require authentication) ──────────────────────────────
  {
    path: '',
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(
            (m) => m.DashboardComponent,
          ),
      },
      {
        // tasks/* routes — "new" must be a child before :id to prevent NaN
        path: 'tasks',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/tasks/task-list/task-list.component').then(
                (m) => m.TaskListComponent,
              ),
          },
          {
            // Declared before :id so "new" is never treated as a numeric ID
            path: 'new',
            loadComponent: () =>
              import('./features/tasks/task-create-page/task-create-page.component').then(
                (m) => m.TaskCreatePageComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import('./features/tasks/task-detail/task-detail.component').then(
                (m) => m.TaskDetailComponent,
              ),
          },
        ],
      },
      {
        // modules/* routes — :id/share must be a child before :id
        path: 'modules',
        children: [
          {
            path: '',
            loadComponent: () =>
              import('./features/modules/module-list/module-list.component').then(
                (m) => m.ModuleListComponent,
              ),
          },
          {
            path: ':id',
            loadComponent: () =>
              import(
                './features/modules/module-detail/module-detail.component'
              ).then((m) => m.ModuleDetailComponent),
          },
          {
            path: ':id/share',
            loadComponent: () =>
              import(
                './features/modules/module-share/module-share.component'
              ).then((m) => m.ModuleShareComponent),
          },
        ],
      },
      {
        path: 'invitations',
        loadComponent: () =>
          import(
            './features/modules/invitations/invitations.component'
          ).then((m) => m.InvitationsComponent),
      },
      {
        path: 'notifications',
        loadComponent: () =>
          import(
            './features/notifications/notification-centre/notification-centre.component'
          ).then((m) => m.NotificationCentreComponent),
      },
      {
        path: 'analytics',
        loadComponent: () =>
          import(
            './features/analytics/analytics-dashboard/analytics-dashboard.component'
          ).then((m) => m.AnalyticsDashboardComponent),
      },
      {
        path: 'settings',
        loadComponent: () =>
          import('./features/settings/settings.component').then(
            (m) => m.SettingsComponent,
          ),
      },
    ],
  },

  // ── Catch-all ──────────────────────────────────────────────────────────────
  {
    path: '**',
    loadComponent: () =>
      import('./shared/components/not-found/not-found.component').then(
        (m) => m.NotFoundComponent,
      ),
  },
];
