-- =====================================================================
-- Schema: customer
-- Public-facing operations: Profile Cache, Inbound Requests
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS customer;

CREATE TABLE customer.inbound_requests (
  id                       bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id                bigint NOT NULL,
  type                     text NOT NULL DEFAULT 'appointment' CHECK (type IN ('call_back', 'appointment')),
  customer_name            text NOT NULL,
  phone_e164               text NOT NULL CHECK (phone_e164 ~ '^\+[1-9][0-9]{7,14}$'),
  email                    text,
  service_id               bigint,
  preferred_date           date,
  preferred_time_window    text,                 -- e.g. 'morning', 'afternoon', 'evening'
  concern                  text,
  status                   text NOT NULL DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'declined')),
  converted_appointment_id bigint,                 -- loose id reference to business.appointments
  handled_by               bigint,
  handled_at               timestamptz,
  created_at               timestamptz NOT NULL DEFAULT now(),
  updated_at               timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX inbound_requests_tenant_status_idx ON customer.inbound_requests (tenant_id, status, created_at DESC);

-- Fast read-only cache of public profile to serve public requests with zero business schema coupling
CREATE TABLE customer.public_profile_cache (
  tenant_id      bigint PRIMARY KEY,
  slug           text NOT NULL UNIQUE,
  name           text NOT NULL,
  vertical       text NOT NULL,
  timezone       text NOT NULL,
  currency       char(3) NOT NULL,
  slot_minutes   smallint NOT NULL DEFAULT 15,
  logo_path      text,
  services_json  jsonb NOT NULL DEFAULT '[]'::jsonb,
  hours_json     jsonb NOT NULL DEFAULT '[]'::jsonb,
  branding_json  jsonb NOT NULL DEFAULT '{}'::jsonb,
  updated_at     timestamptz NOT NULL DEFAULT now()
);
