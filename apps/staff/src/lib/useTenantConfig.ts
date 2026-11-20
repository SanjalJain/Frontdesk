'use client';

import { useState, useEffect } from 'react';

export interface TenantLabels {
  customer: string;
  staff: string;
  appointment: string;
  intakeFields: string[];
}

export interface TenantConfig {
  name: string;
  slug: string;
  vertical: string;
  timezone: string;
  currency: string;
  accentColor: string;
  labels: TenantLabels;
}

const defaultLabels: Record<string, TenantLabels> = {
  dental: {
    customer: 'Patient',
    staff: 'Dentist',
    appointment: 'Appointment',
    intakeFields: ['allergies', 'medical_conditions', 'chief_complaint'],
  },
  salon: {
    customer: 'Client',
    staff: 'Stylist',
    appointment: 'Booking',
    intakeFields: ['hair_type', 'skin_notes', 'preferred_stylist'],
  },
  generic: {
    customer: 'Customer',
    staff: 'Staff',
    appointment: 'Appointment',
    intakeFields: [],
  },
};

export function useTenantConfig() {
  const [config, setConfig] = useState<TenantConfig>({
    name: 'My Practice',
    slug: 'default',
    vertical: 'dental',
    timezone: 'Asia/Kolkata',
    currency: 'INR',
    accentColor: '#A8895A',
    labels: defaultLabels.dental,
  });

  useEffect(() => {
    // If dynamic tenant settings are available from current session or local storage
    const storedVertical = typeof window !== 'undefined' ? localStorage.getItem('tenant_vertical') || 'dental' : 'dental';
    const labels = defaultLabels[storedVertical] || defaultLabels.generic;
    
    setConfig(prev => ({
      ...prev,
      vertical: storedVertical,
      labels,
    }));
  }, []);

  return config;
}
