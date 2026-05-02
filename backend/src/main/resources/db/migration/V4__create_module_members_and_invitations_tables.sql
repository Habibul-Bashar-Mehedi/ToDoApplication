-- V4: Create module_members and invitations tables
-- module_members: tracks which users have access to which modules and at what permission level.
-- invitations: pending/accepted/rejected sharing invitations sent to users.

CREATE TABLE module_members (
    id               BIGINT UNSIGNED                  NOT NULL AUTO_INCREMENT,
    module_id        BIGINT UNSIGNED                  NOT NULL,
    user_id          BIGINT UNSIGNED                  NOT NULL,
    permission_level ENUM('VIEW', 'EDIT', 'ADMIN')    NOT NULL,
    created_at       DATETIME                         NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_module_members             PRIMARY KEY (id),
    CONSTRAINT fk_module_members_module      FOREIGN KEY (module_id) REFERENCES modules (id),
    CONSTRAINT fk_module_members_user        FOREIGN KEY (user_id)   REFERENCES users   (id),
    -- A user can only have one membership record per module (Property 21)
    CONSTRAINT uq_module_members_module_user UNIQUE (module_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------

CREATE TABLE invitations (
    id               BIGINT UNSIGNED                          NOT NULL AUTO_INCREMENT,
    module_id        BIGINT UNSIGNED                          NOT NULL,
    inviter_id       BIGINT UNSIGNED                          NOT NULL,
    invitee_id       BIGINT UNSIGNED                          NOT NULL,
    permission_level ENUM('VIEW', 'EDIT', 'ADMIN')            NOT NULL,
    status           ENUM('PENDING', 'ACCEPTED', 'REJECTED')  NOT NULL DEFAULT 'PENDING',
    invited_at       DATETIME                                 NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at     DATETIME                                 NULL,

    CONSTRAINT pk_invitations         PRIMARY KEY (id),
    CONSTRAINT fk_invitations_module  FOREIGN KEY (module_id)  REFERENCES modules (id),
    CONSTRAINT fk_invitations_inviter FOREIGN KEY (inviter_id) REFERENCES users   (id),
    CONSTRAINT fk_invitations_invitee FOREIGN KEY (invitee_id) REFERENCES users   (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for fetching all pending invitations for a given invitee (inbox view)
CREATE INDEX idx_invitations_invitee_id ON invitations (invitee_id);

-- Index for fetching all invitations associated with a module
CREATE INDEX idx_invitations_module_id  ON invitations (module_id);
