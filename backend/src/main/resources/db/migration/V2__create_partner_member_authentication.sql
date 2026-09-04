CREATE TABLE partners
(
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    partner_key VARCHAR(64) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
    name        VARCHAR(100) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_partners PRIMARY KEY (id),
    CONSTRAINT uq_partners_partner_key UNIQUE (partner_key),
    CONSTRAINT ck_partners_status CHECK (status IN ('ACTIVE', 'SUSPENDED'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE members
(
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    partner_id          BIGINT UNSIGNED NOT NULL,
    email               VARCHAR(254) NOT NULL,
    password_hash       VARCHAR(255) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    display_name        VARCHAR(100) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    password_changed_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at          DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_members PRIMARY KEY (id),
    CONSTRAINT uq_members_partner_email UNIQUE (partner_id, email),
    CONSTRAINT fk_members_partner FOREIGN KEY (partner_id) REFERENCES partners (id),
    CONSTRAINT ck_members_status CHECK (status IN ('ACTIVE', 'LOCKED', 'WITHDRAWN'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE member_roles
(
    member_id  BIGINT UNSIGNED NOT NULL,
    role_code  VARCHAR(30) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    assigned_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT pk_member_roles PRIMARY KEY (member_id, role_code),
    CONSTRAINT fk_member_roles_member FOREIGN KEY (member_id) REFERENCES members (id) ON DELETE CASCADE,
    CONSTRAINT ck_member_roles_role CHECK (role_code IN ('CUSTOMER', 'PARTNER_ADMIN', 'PLATFORM_ADMIN'))
) ENGINE = InnoDB
  DEFAULT CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci;
