import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../../shared/components/toast/toast.service';

/**
 * Handles HTTP error responses globally:
 * - 401: clears stored JWT and redirects to /login
 * - 403: shows a "permission denied" toast
 * - 400: passes through so form components can map field errors
 * - 5xx: shows a generic error toast
 *
 * All errors are re-thrown so calling code can still react if needed.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const router = inject(Router);
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      switch (true) {
        case error.status === 401:
          // Clear the stored token and redirect to login
          localStorage.removeItem('todo_jwt');
          router.navigate(['/login'], {
            queryParams:
              router.url && router.url !== '/login'
                ? { returnUrl: router.url }
                : {},
          });
          break;

        case error.status === 403:
          toastService.error(
            "You don't have permission to perform this action.",
          );
          break;

        case error.status >= 500:
          toastService.error(
            'An unexpected error occurred. Please try again later.',
          );
          break;

        // 400 field errors are passed through to form components — no toast
      }

      // Re-throw so components can inspect the error (e.g., map 400 field errors)
      return throwError(() => error);
    }),
  );
};
