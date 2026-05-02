import { Injectable, computed, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { User } from '../models/user.model';

const JWT_KEY = 'todo_jwt';
const API_BASE = '/api/v1';

export interface AuthResponse {
  token: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface RegisterRequest {
  email: string;
  password: string;
  displayName: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);

  /** Signal holding the currently authenticated user, or null if not logged in. */
  readonly currentUser = signal<User | null>(null);

  /** Derived signal: true when a user is authenticated. */
  readonly isAuthenticated = computed(() => !!this.currentUser());

  constructor() {
    this.restoreSession();
  }

  /**
   * On app init, reads the JWT from localStorage, checks its expiry,
   * and restores the currentUser signal if the token is still valid.
   *
   * The JWT payload contains `userId`, `email`, `exp`, and `iat` claims.
   * Since there is no `/users/me` endpoint, the user object is reconstructed
   * from the JWT claims. Fields not present in the token (`displayName`,
   * `emailVerified`, `createdAt`, `updatedAt`) are set to safe defaults and
   * will be populated with accurate values on the next full login.
   */
  private restoreSession(): void {
    const token = localStorage.getItem(JWT_KEY);
    if (!token) return;

    try {
      const payload = this.decodePayload(token);
      const nowSeconds = Math.floor(Date.now() / 1000);
      const exp = payload['exp'] as number | undefined;

      if (exp && exp > nowSeconds) {
        // Token is still valid — reconstruct user from JWT claims
        const userId = payload['userId'] as number | undefined;
        const email = payload['email'] as string | undefined;

        if (userId && email) {
          const restoredUser: User = {
            id: userId,
            email,
            displayName: email, // best available approximation until next login
            emailVerified: true, // a valid JWT implies the account was verified at login
            createdAt: '',
            updatedAt: '',
          };
          this.currentUser.set(restoredUser);
        } else {
          // Malformed payload — clear the token
          localStorage.removeItem(JWT_KEY);
        }
      } else {
        // Token is expired or has no exp claim — clear it
        localStorage.removeItem(JWT_KEY);
      }
    } catch {
      // Token is malformed — clear it
      localStorage.removeItem(JWT_KEY);
    }
  }

  /** Decodes the payload section of a JWT without verifying the signature. */
  private decodePayload(token: string): Record<string, unknown> {
    const parts = token.split('.');
    if (parts.length !== 3) throw new Error('Invalid JWT');
    const payload = parts[1];
    // Base64url → Base64 → JSON
    const base64 = payload.replace(/-/g, '+').replace(/_/g, '/');
    const json = atob(base64);
    return JSON.parse(json) as Record<string, unknown>;
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${API_BASE}/auth/login`, request)
      .pipe(
        tap((response) => {
          localStorage.setItem(JWT_KEY, response.token);
          this.currentUser.set(response.user);
        }),
      );
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/register`, request);
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/logout`, {}).pipe(
      tap({
        next: () => this.clearSession(),
        error: () => this.clearSession(), // clear locally even if API call fails
      }),
    );
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/verify-email`, { token });
  }

  resendVerification(email: string): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/resend-verification`, {
      email,
    });
  }

  forgotPassword(request: ForgotPasswordRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/forgot-password`, request);
  }

  resetPassword(request: ResetPasswordRequest): Observable<void> {
    return this.http.post<void>(`${API_BASE}/auth/reset-password`, request);
  }

  private clearSession(): void {
    localStorage.removeItem(JWT_KEY);
    this.currentUser.set(null);
  }
}
