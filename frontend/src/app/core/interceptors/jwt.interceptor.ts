import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';

/** localStorage key where the JWT is stored */
const JWT_KEY = 'todo_jwt';

/** Auth endpoints that should NOT receive the Authorization header */
const AUTH_PATHS = [
  '/auth/login',
  '/auth/register',
  '/auth/verify-email',
  '/auth/resend-verification',
  '/auth/forgot-password',
  '/auth/reset-password',
];

function isAuthEndpoint(req: HttpRequest<unknown>): boolean {
  return AUTH_PATHS.some((path) => req.url.includes(path));
}

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  if (isAuthEndpoint(req)) {
    return next(req);
  }

  const token = localStorage.getItem(JWT_KEY);
  if (!token) {
    return next(req);
  }

  const authReq = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` },
  });

  return next(authReq);
};
