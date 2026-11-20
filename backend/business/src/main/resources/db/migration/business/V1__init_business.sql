-- =====================================================================
-- Schema: business
-- Staff-facing operations: Setup, Customers, Scheduling, Appointments, Payments
-- =====================================================================

CREATE SCHEMA IF NOT EXISTS business;

-- A staff row is a bookable person (dentist, stylist). It may or may not log in.
CREATE TABLE business.staff (
  id          bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id   bigint NOT NULL,
  user_id     bigint,
  name        text NOT NULL,
  title       text,                              -- 'Dentist', 'Senior stylist'
  color       text,                              -- calendar colour
  is_bookable boolean NOT NULL DEFAULT true,
  is_active   boolean NOT NULL DEFAULT true,
  sort_order  smallint NOT NULL DEFAULT 0,
  UNIQUE (tenant_id, id)
);
CREATE INDEX staff_tenant_idx ON business.staff (tenant_id);

CREATE TABLE business.services (
  id               bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id        bigint NOT NULL,
  name             text NOT NULL,
  duration_min     smallint NOT NULL CHECK (duration_min > 0),
  buffer_after_min smallint NOT NULL DEFAULT 0 CHECK (buffer_after_min >= 0),
  price_paise      integer NOT NULL DEFAULT 0 CHECK (price_paise >= 0),
  is_active        boolean NOT NULL DEFAULT true,
  UNIQUE (tenant_id, id)
);
CREATE INDEX services_tenant_idx ON business.services (tenant_id);

CREATE TABLE business.staff_services (
  tenant_id  bigint NOT NULL,
  staff_id   bigint NOT NULL,
  service_id bigint NOT NULL,
  PRIMARY KEY (staff_id, service_id),
  FOREIGN KEY (tenant_id, staff_id)   REFERENCES business.staff (tenant_id, id)    ON DELETE CASCADE,
  FOREIGN KEY (tenant_id, service_id) REFERENCES business.services (tenant_id, id) ON DELETE CASCADE
);

-- Local wall-clock hours in the tenant timezone. weekday: 1 = Monday ... 7 = Sunday.
CREATE TABLE business.working_hours (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id  bigint NOT NULL,
  staff_id   bigint NOT NULL,
  weekday    smallint NOT NULL CHECK (weekday BETWEEN 1 AND 7),
  start_time time NOT NULL,
  end_time   time NOT NULL,
  CHECK (end_time > start_time),
  UNIQUE (staff_id, weekday, start_time),
  FOREIGN KEY (tenant_id, staff_id) REFERENCES business.staff (tenant_id, id) ON DELETE CASCADE
);

-- Leave, holidays, blocked time. staff_id NULL means the whole business is closed.
CREATE TABLE business.time_off (
  id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id bigint NOT NULL,
  staff_id  bigint,
  starts_at timestamptz NOT NULL,
  ends_at   timestamptz NOT NULL,
  reason    text,
  CHECK (ends_at > starts_at),
  FOREIGN KEY (tenant_id, staff_id) REFERENCES business.staff (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX time_off_tenant_idx ON business.time_off (tenant_id, starts_at);

-- Customers (patients, salon clients)
CREATE TABLE business.customers (
  id         bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id  bigint NOT NULL,
  full_name  text NOT NULL,
  phone_e164 text NOT NULL CHECK (phone_e164 ~ '^\+[1-9][0-9]{7,14}$'),
  email      text,
  dob        date,
  notes      text,
  intake     jsonb NOT NULL DEFAULT '{}'::jsonb,
  created_at timestamptz NOT NULL DEFAULT now(),
  deleted_at timestamptz,
  UNIQUE (tenant_id, id),
  UNIQUE (tenant_id, phone_e164)
);
CREATE INDEX customers_phone_prefix_idx ON business.customers (tenant_id, phone_e164 text_pattern_ops);
CREATE INDEX customers_name_trgm_idx    ON business.customers USING gin (full_name gin_trgm_ops);

-- Appointments
CREATE TABLE business.appointments (
  id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id       bigint NOT NULL,
  customer_id     bigint NOT NULL,
  service_id      bigint NOT NULL,
  staff_id        bigint,
  starts_at       timestamptz NOT NULL,
  ends_at         timestamptz NOT NULL,
  status          text NOT NULL DEFAULT 'booked'
                  CHECK (status IN ('pending','booked','confirmed','arrived',
                                    'in_progress','completed','cancelled','no_show')),
  source          text NOT NULL DEFAULT 'phone'
                  CHECK (source IN ('phone','walk_in','web')),
  created_by      bigint,                          -- loose user_id reference
  request_note    text,
  price_paise     integer NOT NULL DEFAULT 0 CHECK (price_paise >= 0),
  version         integer NOT NULL DEFAULT 0,      -- optimistic locking

  checked_in_at   timestamptz,
  started_at      timestamptz,
  completed_at    timestamptz,

  outcome         text,
  no_show_reason  text,
  cancel_reason   text,
  reaction        text CHECK (reaction IN ('happy','neutral','unhappy')),
  reaction_note   text,
  followup_status text NOT NULL DEFAULT 'none'
                  CHECK (followup_status IN ('none','needed','called','booked','declined')),
  followup_due_on date,

  created_at      timestamptz NOT NULL DEFAULT now(),
  updated_at      timestamptz NOT NULL DEFAULT now(),

  UNIQUE (tenant_id, id),
  FOREIGN KEY (tenant_id, customer_id) REFERENCES business.customers (tenant_id, id),
  FOREIGN KEY (tenant_id, service_id)  REFERENCES business.services (tenant_id, id),
  FOREIGN KEY (tenant_id, staff_id)    REFERENCES business.staff (tenant_id, id),

  CHECK (ends_at > starts_at),
  CHECK (status = 'pending' OR staff_id IS NOT NULL),

  -- Database-level double-booking prevention per tenant and staff
  CONSTRAINT appointments_no_overlap
    EXCLUDE USING gist (
      tenant_id WITH =,
      staff_id WITH =,
      tstzrange(starts_at, ends_at, '[)') WITH &&
    )
    WHERE (status IN ('booked','confirmed','arrived','in_progress','completed'))
);

CREATE INDEX appointments_calendar_idx ON business.appointments (tenant_id, starts_at);
CREATE INDEX appointments_staff_idx    ON business.appointments (staff_id, starts_at);
CREATE INDEX appointments_customer_idx ON business.appointments (tenant_id, customer_id, starts_at DESC);
CREATE INDEX appointments_pending_idx  ON business.appointments (tenant_id, created_at) WHERE status = 'pending';
CREATE INDEX appointments_followup_idx ON business.appointments (tenant_id, followup_status) WHERE status = 'completed';

CREATE TABLE business.appointment_status_history (
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id      bigint NOT NULL,
  appointment_id bigint NOT NULL,
  from_status    text,
  to_status      text NOT NULL,
  changed_by     bigint,
  changed_at     timestamptz NOT NULL DEFAULT now(),
  note           text,
  FOREIGN KEY (tenant_id, appointment_id) REFERENCES business.appointments (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX status_history_appt_idx ON business.appointment_status_history (appointment_id, changed_at);

CREATE FUNCTION business.log_appointment_status() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
  IF TG_OP = 'INSERT' THEN
    INSERT INTO business.appointment_status_history (tenant_id, appointment_id, from_status, to_status, changed_by)
    VALUES (NEW.tenant_id, NEW.id, NULL, NEW.status, NEW.created_by);
  ELSIF NEW.status IS DISTINCT FROM OLD.status THEN
    INSERT INTO business.appointment_status_history (tenant_id, appointment_id, from_status, to_status, changed_by)
    VALUES (NEW.tenant_id, NEW.id, OLD.status, NEW.status,
            NULLIF(current_setting('app.user_id', true), '')::bigint);
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER appointments_status_log
AFTER INSERT OR UPDATE OF status ON business.appointments
FOR EACH ROW EXECUTE FUNCTION business.log_appointment_status();

-- Payments (manual recording)
CREATE TABLE business.payments (
  id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id      bigint NOT NULL,
  customer_id    bigint NOT NULL,
  appointment_id bigint,
  amount_paise   integer NOT NULL CHECK (amount_paise > 0),
  method         text NOT NULL CHECK (method IN ('cash','upi','card')),
  reference      text,
  receipt_no     text NOT NULL,
  received_by    bigint,
  received_at    timestamptz NOT NULL DEFAULT now(),
  note           text,
  UNIQUE (tenant_id, receipt_no),
  FOREIGN KEY (tenant_id, customer_id)    REFERENCES business.customers (tenant_id, id),
  FOREIGN KEY (tenant_id, appointment_id) REFERENCES business.appointments (tenant_id, id)
);
CREATE INDEX payments_customer_idx ON business.payments (tenant_id, customer_id);
CREATE INDEX payments_date_idx     ON business.payments (tenant_id, received_at);

-- Waitlist
CREATE TABLE business.waitlist_entries (
  id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  tenant_id     bigint NOT NULL,
  customer_id   bigint NOT NULL,
  service_id    bigint NOT NULL,
  staff_id      bigint,
  earliest_date date,
  latest_date   date,
  notes         text,
  status        text NOT NULL DEFAULT 'waiting'
                CHECK (status IN ('waiting','offered','booked','expired','removed')),
  created_at    timestamptz NOT NULL DEFAULT now(),
  FOREIGN KEY (tenant_id, customer_id) REFERENCES business.customers (tenant_id, id),
  FOREIGN KEY (tenant_id, service_id)  REFERENCES business.services (tenant_id, id),
  FOREIGN KEY (tenant_id, staff_id)    REFERENCES business.staff (tenant_id, id)
);
CREATE INDEX waitlist_active_idx ON business.waitlist_entries (tenant_id, created_at) WHERE status = 'waiting';

-- Views
CREATE VIEW business.v_followup_call_list AS
SELECT a.tenant_id, a.id AS appointment_id, a.customer_id,
       a.ends_at AS last_visit_at, a.followup_due_on, a.followup_status
FROM business.appointments a
WHERE a.status = 'completed'
  AND a.reaction = 'happy'
  AND a.followup_status IN ('none','needed')
  AND NOT EXISTS (
    SELECT 1 FROM business.appointments n
    WHERE n.tenant_id = a.tenant_id
      AND n.customer_id = a.customer_id
      AND n.starts_at > a.starts_at
      AND n.status IN ('pending','booked','confirmed')
  );

CREATE VIEW business.v_customer_balance AS
SELECT c.tenant_id, c.id AS customer_id,
       COALESCE(b.billed, 0) - COALESCE(p.paid, 0) AS balance_paise
FROM business.customers c
LEFT JOIN (SELECT customer_id, sum(price_paise) AS billed
           FROM business.appointments WHERE status = 'completed' GROUP BY customer_id) b ON b.customer_id = c.id
LEFT JOIN (SELECT customer_id, sum(amount_paise) AS paid
           FROM business.payments GROUP BY customer_id) p ON p.customer_id = c.id;
