export type ModuleVisibility = 'PRIVATE' | 'SHARED';
export type PermissionLevel = 'VIEW' | 'EDIT' | 'ADMIN';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

export interface Module {
  id: number;
  ownerId: number;
  name: string;
  description: string | null;
  visibility: ModuleVisibility;
  createdAt: string;
  updatedAt: string;
}

export interface ModuleMember {
  id: number;
  moduleId: number;
  userId: number;
  userDisplayName: string;
  userEmail: string;
  permissionLevel: PermissionLevel;
  createdAt: string;
}

export interface Invitation {
  id: number;
  moduleId: number;
  moduleName: string;
  inviterId: number;
  inviterDisplayName: string;
  inviteeId: number;
  inviteeEmail: string;
  permissionLevel: PermissionLevel;
  status: InvitationStatus;
  invitedAt: string;
  respondedAt: string | null;
}

export interface ModuleCreateRequest {
  name: string;
  description?: string;
  visibility?: ModuleVisibility;
}

export interface ModuleUpdateRequest {
  name?: string;
  description?: string;
}

export interface InvitationRequest {
  inviteeEmail: string;
  permissionLevel: PermissionLevel;
}
