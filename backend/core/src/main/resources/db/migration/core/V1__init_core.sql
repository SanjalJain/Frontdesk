-- =====================================================================
-- Schema: core
-- Core platform tables: Tenancy, Identity, Outbox, Messaging, Audit, Sessions
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS core;

-- Config templates: vertical differences live here (data), not in code.
CREATE TABLE core.vertical_templates (
  key         text PRIMARY KEY,                 -- 'dental', 'salon', 'generic'
  label       text NOT NULL,
  config      jsonb NOT NULL                    -- labels, intake fields, default services, message wording
);

CREATE TABLE core.platform_admins (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  email         text NOT NULL,
  password_hash text NOT NULL,
  created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX platform_admins_email_uq ON core.platform_admins (lower(email));

CREATE TABLE core.tenants (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  slug          text NOT NULL UNIQUE CHECK (slug ~ '^[a-z0-9-]{3,40}$'),  -- public form URL
  name          text NOT NULL,
  vertical      text NOT NULL DEFAULT 'dental' REFERENCES core.vertical_templates (key),
  timezone      text NOT NULL DEFAULT 'Asia/Kolkata',
  currency      char(3) NOT NULL DEFAULT 'INR',
  slot_minutes  smallint NOT NULL DEFAULT 15 CHECK (slot_minutes BETWEEN 5 AND 120),
  settings      jsonb NOT NULL DEFAULT '{}'::jsonb,   -- branding, labels override, booking rules
  logo_path     text,
  is_active     boolean NOT NULL DEFAULT true,
  created_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE core.users (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id     bigint NOT NULL REFERENCES core.tenants (id),
  email         text NOT NULL,
  password_hash text NOT NULL,
  full_name     text NOT NULL,
  role          text NOT NULL DEFAULT 'owner'
                CHECK (role IN ('owner', 'manager', 'receptionist')),
  is_active     boolean NOT NULL DEFAULT true,
  last_login_at timestamptz,
  created_at    timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, id)
);
CREATE UNIQUE INDEX users_email_uq ON core.users (lower(email));

-- Transactional Outbox for async inter-module event publishing
CREATE TABLE core.outbox_events (
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id      bigint NOT NULL REFERENCES core.tenants (id),
  event_type     text NOT NULL,
  aggregate_type text NOT NULL,
  aggregate_id   text NOT NULL,
  payload        jsonb NOT NULL,
  created_at     timestamptz NOT NULL DEFAULT now(),
  published_at   timestamptz
);
CREATE INDEX outbox_unpublished_idx ON core.outbox_events (created_at) WHERE published_at IS NULL;

-- Messaging: templates + outbound queue
CREATE TABLE core.message_templates (
  id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL REFERENCES core.tenants (id),
  kind      text NOT NULL CHECK (kind IN ('confirmation','reminder_24h','reminder_2h','custom')),
  channel   text NOT NULL DEFAULT 'whatsapp',
  language  text NOT NULL DEFAULT 'en',
  body      text NOT NULL,
  is_active boolean NOT NULL DEFAULT true,
  UNIQUE (tenant_id, kind, channel, language)
);

CREATE TABLE core.notifications (
  id                  bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id           bigint NOT NULL REFERENCES core.tenants (id),
  ref_type            text NOT NULL DEFAULT 'appointment', -- polymorphic ref
  ref_id              bigint,                              -- loose id reference (no cross-schema FK)
  customer_phone      text NOT NULL,
  customer_name       text,
  kind                text NOT NULL CHECK (kind IN ('confirmation','reminder_24h','reminder_2h','custom')),
  channel             text NOT NULL DEFAULT 'whatsapp',
  provider            text NOT NULL DEFAULT 'mock',
  body                text NOT NULL,
  scheduled_for       timestamptz NOT NULL,
  status              text NOT NULL DEFAULT 'queued'
                      CHECK (status IN ('queued','sending','sent','failed','cancelled')),
  attempts            smallint NOT NULL DEFAULT 0,
  next_attempt_at     timestamptz,
  last_error          text,
  provider_message_id text,
  sent_at             timestamptz,
  dedupe_key          text NOT NULL,
  created_at          timestamptz NOT NULL DEFAULT now(),
  UNIQUE (tenant_id, dedupe_key)
);
CREATE INDEX notifications_due_idx ON core.notifications (scheduled_for) WHERE status = 'queued';
CREATE INDEX notifications_ref_idx ON core.notifications (tenant_id, ref_type, ref_id);

CREATE TABLE core.audit_log (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id  bigint REFERENCES core.tenants (id),
  user_id    bigint,
  entity     text NOT NULL,
  entity_id  bigint,
  action     text NOT NULL,
  changes    jsonb,
  ip         inet,
  created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX audit_entity_idx ON core.audit_log (tenant_id, entity, entity_id);
CREATE INDEX audit_time_idx   ON core.audit_log (tenant_id, created_at DESC);

-- Spring Session JDBC tables in core schema
CREATE TABLE core.spring_session (
  primary_id            char(36) NOT NULL PRIMARY KEY,
  session_id            char(36) NOT NULL,
  creation_time         bigint NOT NULL,
  last_access_time      bigint NOT NULL,
  max_inactive_interval integer NOT NULL,
  expiry_time           bigint NOT NULL,
  principal_name        text
);
CREATE UNIQUE INDEX spring_session_ix1 ON core.spring_session (session_id);
CREATE INDEX spring_session_ix2 ON core.spring_session (expiry_time);
CREATE INDEX spring_session_ix3 ON core.spring_session (principal_name);

CREATE TABLE core.spring_session_attributes (
  session_primary_id char(36) NOT NULL REFERENCES core.spring_session(primary_id) ON DELETE CASCADE,
  attribute_name     varchar(200) NOT NULL,
  attribute_bytes    bytea NOT NULL,
  PRIMARY KEY (session_primary_id, attribute_name)
);
