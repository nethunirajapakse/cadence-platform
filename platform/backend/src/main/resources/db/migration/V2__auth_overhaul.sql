-- Auth overhaul: ADMIN role, direct-manager link, invitations, password reset
-- tokens, and an audit log.

-- ---- roles: allow ADMIN ----------------------------------------------------

ALTER TABLE public.roles DROP CONSTRAINT IF EXISTS roles_role_name_check;
ALTER TABLE public.roles
    ADD CONSTRAINT roles_role_name_check CHECK (((role_name)::text = ANY ((ARRAY[
        'TEAM_MEMBER'::character varying,
        'MANAGER'::character varying,
        'ADMIN'::character varying
    ])::text[])));

-- ---- users: manager link + password change marker --------------------------

ALTER TABLE public.users ADD COLUMN manager_id uuid;
ALTER TABLE public.users ADD COLUMN password_changed_at timestamp(6) without time zone;

ALTER TABLE public.users
    ADD CONSTRAINT fk_users_manager FOREIGN KEY (manager_id) REFERENCES public.users(user_id);
ALTER TABLE public.users
    ADD CONSTRAINT users_manager_not_self CHECK (manager_id IS NULL OR manager_id <> user_id);

CREATE INDEX idx_users_manager_id ON public.users (manager_id);

-- Emails are compared case-insensitively throughout the app; enforce it here too.
CREATE UNIQUE INDEX uq_users_email_lower ON public.users (lower(email));

-- ---- invitations -----------------------------------------------------------

CREATE TABLE public.invitations (
    invitation_id uuid NOT NULL,
    email character varying(180) NOT NULL,
    role_id uuid NOT NULL,
    manager_id uuid,
    invited_by uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    status character varying(20) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    accepted_at timestamp(6) without time zone,
    CONSTRAINT invitations_pkey PRIMARY KEY (invitation_id),
    CONSTRAINT invitations_token_hash_key UNIQUE (token_hash),
    CONSTRAINT invitations_status_check CHECK (((status)::text = ANY ((ARRAY[
        'PENDING'::character varying,
        'ACCEPTED'::character varying,
        'REVOKED'::character varying
    ])::text[]))),
    CONSTRAINT fk_invitations_role FOREIGN KEY (role_id) REFERENCES public.roles(role_id),
    CONSTRAINT fk_invitations_manager FOREIGN KEY (manager_id) REFERENCES public.users(user_id),
    CONSTRAINT fk_invitations_invited_by FOREIGN KEY (invited_by) REFERENCES public.users(user_id)
);

-- At most one live invitation per email address.
CREATE UNIQUE INDEX uq_invitations_pending_email
    ON public.invitations (lower(email)) WHERE status = 'PENDING';

-- ---- password reset tokens -------------------------------------------------

CREATE TABLE public.password_reset_tokens (
    reset_token_id uuid NOT NULL,
    user_id uuid NOT NULL,
    token_hash character varying(64) NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    used_at timestamp(6) without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (reset_token_id),
    CONSTRAINT password_reset_tokens_token_hash_key UNIQUE (token_hash),
    CONSTRAINT fk_reset_tokens_user FOREIGN KEY (user_id) REFERENCES public.users(user_id)
);

CREATE INDEX idx_reset_tokens_user_id ON public.password_reset_tokens (user_id);

-- ---- audit log -------------------------------------------------------------

-- Deliberately no foreign keys: audit rows must outlive whatever they describe.
CREATE TABLE public.audit_log (
    audit_id uuid NOT NULL,
    actor_user_id uuid,
    actor_email character varying(180),
    action character varying(60) NOT NULL,
    target character varying(255),
    details text,
    ip_address character varying(64),
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT audit_log_pkey PRIMARY KEY (audit_id)
);

CREATE INDEX idx_audit_log_created_at ON public.audit_log (created_at DESC);
CREATE INDEX idx_audit_log_action ON public.audit_log (action);
