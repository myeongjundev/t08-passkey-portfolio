CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_handle BYTEA NOT NULL UNIQUE,
    display_name VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_accounts_user_handle_length CHECK (OCTET_LENGTH(user_handle) = 32),
    CONSTRAINT ck_accounts_display_name_length CHECK (CHAR_LENGTH(display_name) BETWEEN 1 AND 40)
);

CREATE TABLE passkey_credentials (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    credential_id BYTEA NOT NULL UNIQUE,
    public_key_cose BYTEA NOT NULL,
    credential_record TEXT NOT NULL,
    sign_count BIGINT NOT NULL DEFAULT 0,
    transports VARCHAR(255),
    backup_eligible BOOLEAN NOT NULL DEFAULT FALSE,
    backup_state BOOLEAN NOT NULL DEFAULT FALSE,
    nickname VARCHAR(40) NOT NULL,
    registered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uq_passkey_account_nickname UNIQUE (account_id, nickname),
    CONSTRAINT ck_passkey_credential_id_present CHECK (OCTET_LENGTH(credential_id) > 0),
    CONSTRAINT ck_passkey_public_key_present CHECK (OCTET_LENGTH(public_key_cose) > 0),
    CONSTRAINT ck_passkey_nickname_length CHECK (CHAR_LENGTH(nickname) BETWEEN 1 AND 40),
    CONSTRAINT ck_passkey_sign_count_nonnegative CHECK (sign_count >= 0)
);

CREATE TABLE webauthn_ceremonies (
    id UUID PRIMARY KEY,
    kind VARCHAR(20) NOT NULL,
    challenge BYTEA NOT NULL UNIQUE,
    owner_key BYTEA NOT NULL,
    active_slot VARCHAR(8),
    account_id UUID REFERENCES accounts(id) ON DELETE CASCADE,
    pending_user_handle BYTEA,
    pending_display_name VARCHAR(40),
    pending_nickname VARCHAR(40),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_ceremony_owner_active UNIQUE (owner_key, kind, active_slot),
    CONSTRAINT ck_ceremony_kind CHECK (kind IN ('CREATE_ACCOUNT', 'ADD_PASSKEY', 'AUTHENTICATE')),
    CONSTRAINT ck_ceremony_challenge_length CHECK (OCTET_LENGTH(challenge) = 32),
    CONSTRAINT ck_ceremony_owner_key_length CHECK (OCTET_LENGTH(owner_key) = 32),
    CONSTRAINT ck_ceremony_active_slot CHECK (active_slot IS NULL OR active_slot = 'ACTIVE'),
    CONSTRAINT ck_ceremony_time_order CHECK (expires_at > created_at),
    CONSTRAINT ck_ceremony_create_fields CHECK (
        kind <> 'CREATE_ACCOUNT' OR
        (account_id IS NULL AND OCTET_LENGTH(pending_user_handle) = 32
         AND pending_display_name IS NOT NULL AND pending_nickname IS NOT NULL)
    ),
    CONSTRAINT ck_ceremony_add_account CHECK (kind <> 'ADD_PASSKEY' OR account_id IS NOT NULL)
);

CREATE INDEX ix_ceremony_expiry ON webauthn_ceremonies(expires_at);

CREATE TABLE private_items (
    id UUID PRIMARY KEY,
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(1000) NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_private_item_order UNIQUE (account_id, sort_order),
    CONSTRAINT ck_private_item_category CHECK (category IN ('PROJECT', 'TARGET', 'RETROSPECTIVE')),
    CONSTRAINT ck_private_item_title_length CHECK (CHAR_LENGTH(title) BETWEEN 1 AND 120),
    CONSTRAINT ck_private_item_body_length CHECK (CHAR_LENGTH(body) BETWEEN 1 AND 1000),
    CONSTRAINT ck_private_item_sort_order CHECK (sort_order >= 0)
);

CREATE TABLE security_events (
    id UUID PRIMARY KEY,
    event_type VARCHAR(40) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    account_id UUID REFERENCES accounts(id) ON DELETE SET NULL,
    ceremony_id UUID,
    credential_fingerprint VARCHAR(64),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_security_event_outcome CHECK (outcome IN ('SUCCESS', 'REJECTED'))
);

CREATE INDEX ix_security_event_occurred ON security_events(occurred_at);
CREATE INDEX ix_security_event_account ON security_events(account_id);
