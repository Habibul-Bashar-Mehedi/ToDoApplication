import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';
import {
  Module,
  ModuleMember,
  Invitation,
  ModuleCreateRequest,
  ModuleUpdateRequest,
  InvitationRequest,
  PermissionLevel,
} from '../models/module.model';

const API_BASE = '/api/v1';

@Injectable({ providedIn: 'root' })
export class ModuleService {
  private readonly http = inject(HttpClient);

  /** BehaviorSubject holding the current list of accessible modules. */
  private readonly modulesSubject = new BehaviorSubject<Module[]>([]);

  /** Observable stream of the module list for components to subscribe to. */
  readonly modules$ = this.modulesSubject.asObservable();

  // ─── Module CRUD ────────────────────────────────────────────────────────────

  /**
   * Fetches all modules accessible to the current user (owned + member)
   * and updates the local BehaviorSubject.
   */
  getModules(): Observable<Module[]> {
    return this.http
      .get<Module[]>(`${API_BASE}/modules`)
      .pipe(tap((modules) => this.modulesSubject.next(modules)));
  }

  /** Fetches a single module by ID. Does not update the BehaviorSubject. */
  getModule(id: number): Observable<Module> {
    return this.http.get<Module>(`${API_BASE}/modules/${id}`);
  }

  /**
   * Creates a new module and prepends it to the local module list on success.
   */
  createModule(request: ModuleCreateRequest): Observable<Module> {
    return this.http.post<Module>(`${API_BASE}/modules`, request).pipe(
      tap((created) => {
        const current = this.modulesSubject.getValue();
        this.modulesSubject.next([created, ...current]);
      }),
    );
  }

  /**
   * Updates a module's name or description and replaces it in the local list.
   */
  updateModule(id: number, request: ModuleUpdateRequest): Observable<Module> {
    return this.http.put<Module>(`${API_BASE}/modules/${id}`, request).pipe(
      tap((updated) => this.replaceModule(updated)),
    );
  }

  /**
   * Soft-deletes a module (and all its tasks) and removes it from the local list.
   */
  deleteModule(id: number): Observable<void> {
    return this.http.delete<void>(`${API_BASE}/modules/${id}`).pipe(
      tap(() => {
        const current = this.modulesSubject.getValue();
        this.modulesSubject.next(current.filter((m) => m.id !== id));
      }),
    );
  }

  // ─── Members ────────────────────────────────────────────────────────────────

  /** Lists all members and their permission levels for a module. */
  getMembers(moduleId: number): Observable<ModuleMember[]> {
    return this.http.get<ModuleMember[]>(
      `${API_BASE}/modules/${moduleId}/members`,
    );
  }

  /**
   * Invites a user by email to a module at the specified permission level.
   * Creates a pending Invitation and sends an in-app notification to the invitee.
   */
  inviteMember(
    moduleId: number,
    request: InvitationRequest,
  ): Observable<Invitation> {
    return this.http.post<Invitation>(
      `${API_BASE}/modules/${moduleId}/invitations`,
      request,
    );
  }

  /**
   * Updates the permission level of an existing module member.
   */
  updateMember(
    moduleId: number,
    userId: number,
    permissionLevel: PermissionLevel,
  ): Observable<ModuleMember> {
    return this.http.put<ModuleMember>(
      `${API_BASE}/modules/${moduleId}/members/${userId}`,
      { permissionLevel },
    );
  }

  /**
   * Revokes a member's access to a module.
   * The module owner cannot be removed.
   */
  removeMember(moduleId: number, userId: number): Observable<void> {
    return this.http.delete<void>(
      `${API_BASE}/modules/${moduleId}/members/${userId}`,
    );
  }

  // ─── Invitations ────────────────────────────────────────────────────────────

  /** Lists all pending invitations for the current user. */
  getInvitations(): Observable<Invitation[]> {
    return this.http.get<Invitation[]>(`${API_BASE}/invitations`);
  }

  /**
   * Accepts a pending invitation, creating a ModuleMember record.
   * The accepted module is added to the local modules list.
   */
  acceptInvitation(invitationId: number): Observable<Invitation> {
    return this.http.patch<Invitation>(
      `${API_BASE}/invitations/${invitationId}/accept`,
      {},
    );
  }

  /**
   * Rejects a pending invitation, marking it as REJECTED.
   */
  rejectInvitation(invitationId: number): Observable<Invitation> {
    return this.http.patch<Invitation>(
      `${API_BASE}/invitations/${invitationId}/reject`,
      {},
    );
  }

  // ─── Helpers ────────────────────────────────────────────────────────────────

  /** Replaces a module in the BehaviorSubject by matching ID. */
  private replaceModule(updated: Module): void {
    const current = this.modulesSubject.getValue();
    this.modulesSubject.next(
      current.map((m) => (m.id === updated.id ? updated : m)),
    );
  }
}
