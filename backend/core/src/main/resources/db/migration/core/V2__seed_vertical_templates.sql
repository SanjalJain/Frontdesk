-- =====================================================================
-- Seed vertical templates into core.vertical_templates
-- =====================================================================

INSERT INTO core.vertical_templates (key, label, config) VALUES
('dental', 'Dental clinic', '{
  "labels": {"customer": "Patient", "staff": "Dentist", "appointment": "Appointment"},
  "intake_fields": ["allergies", "medical_conditions", "chief_complaint"],
  "default_services": [
    {"name": "Consultation", "duration_min": 20, "price_paise": 30000},
    {"name": "Scaling and polishing", "duration_min": 40, "price_paise": 120000},
    {"name": "Root canal (visit)", "duration_min": 60, "price_paise": 400000}
  ],
  "recall_months": 6
}')
ON CONFLICT (key) DO NOTHING;

INSERT INTO core.vertical_templates (key, label, config) VALUES
('salon', 'Salon', '{
  "labels": {"customer": "Client", "staff": "Stylist", "appointment": "Booking"},
  "intake_fields": ["hair_type", "skin_notes", "preferred_stylist"],
  "default_services": [
    {"name": "Haircut", "duration_min": 30, "price_paise": 40000},
    {"name": "Hair colour", "duration_min": 90, "price_paise": 250000}
  ],
  "recall_months": 2
}')
ON CONFLICT (key) DO NOTHING;

INSERT INTO core.vertical_templates (key, label, config) VALUES
('generic', 'Generic', '{
  "labels": {"customer": "Customer", "staff": "Staff", "appointment": "Appointment"},
  "intake_fields": [],
  "default_services": [],
  "recall_months": 3
}')
ON CONFLICT (key) DO NOTHING;
