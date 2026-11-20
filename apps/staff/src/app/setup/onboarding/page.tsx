'use client';

import React, { useState } from 'react';
import { Card, Button, Input } from '@booking/ui';

interface ServiceItem {
  id?: number;
  name: string;
  durationMin: number;
  pricePaise: number;
}

export default function OnboardingWizardPage() {
  const [step, setStep] = useState(1);
  const [vertical, setVertical] = useState('dental');
  const [practiceName, setPracticeName] = useState('');
  const [primaryStaffName, setPrimaryStaffName] = useState('');
  const [services, setServices] = useState<ServiceItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Step 1: Seed from template
  const handleSeedTemplate = async () => {
    setLoading(true);
    setError('');
    try {
      if (typeof window !== 'undefined') {
        localStorage.setItem('tenant_vertical', vertical);
      }

      const res = await fetch(`/api/v1/biz/setup/onboarding/seed?vertical=${vertical}&primaryStaffName=${encodeURIComponent(primaryStaffName || 'Primary Practitioner')}`, {
        method: 'POST',
      });

      if (!res.ok) {
        throw new Error('Failed to seed vertical template.');
      }

      const data = await res.json();
      setServices(data);
      setStep(2);
    } catch (err: any) {
      setError(err.message || 'Error seeding template.');
    } finally {
      setLoading(false);
    }
  };

  // Step 2: Add or edit services
  const handleAddService = () => {
    setServices([
      ...services,
      { name: 'New Service', durationMin: 30, pricePaise: 50000 },
    ]);
  };

  const updateService = (index: number, field: keyof ServiceItem, value: any) => {
    const updated = [...services];
    updated[index] = { ...updated[index], [field]: value };
    setServices(updated);
  };

  return (
    <div className="min-h-screen bg-[var(--bg)] py-12 px-4 max-w-4xl mx-auto">
      {/* Header */}
      <div className="text-center mb-10">
        <span className="label-caps">Quick Setup Wizard</span>
        <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-2">
          {step === 1 && 'Select Your Practice Type'}
          {step === 2 && 'Review & Customize Services'}
          {step === 3 && 'Working Hours & Availability'}
          {step === 4 && 'Setup Complete!'}
        </h1>
        <p className="text-sm text-[var(--text-2)] mt-1">
          {step === 1 && 'Choose your vertical to automatically pre-fill services, terminology, and hours.'}
          {step === 2 && 'Set durations and pricing in INR (paise). You can adjust these anytime.'}
          {step === 3 && 'Standard hours (Mon–Sat, 9:00 AM – 6:00 PM) have been configured.'}
          {step === 4 && 'Your practice is live and ready to take bookings.'}
        </p>

        {/* Stepper indicator */}
        <div className="flex justify-center items-center gap-3 mt-6">
          {[1, 2, 3, 4].map((s) => (
            <div
              key={s}
              className={`w-8 h-8 rounded-full flex items-center justify-center text-xs font-medium transition-colors ${
                s === step
                  ? 'bg-[var(--text)] text-[var(--bg)] font-semibold'
                  : s < step
                  ? 'bg-[var(--accent)] text-white'
                  : 'bg-[var(--surface-2)] text-[var(--text-3)]'
              }`}
            >
              {s < step ? '✓' : s}
            </div>
          ))}
        </div>
      </div>

      {error && (
        <div className="mb-6 p-4 rounded-[var(--radius-ctl)] bg-[rgba(176,122,122,0.1)] text-xs text-[#B07A7A] max-w-lg mx-auto">
          {error}
        </div>
      )}

      {/* Step 1: Vertical Selection */}
      {step === 1 && (
        <Card className="max-w-xl mx-auto">
          <div className="flex flex-col gap-5">
            <Input
              label="Practice / Business Name"
              placeholder="e.g. Apex Dental Studio or Luxe Hair Salon"
              value={practiceName}
              onChange={(e) => setPracticeName(e.target.value)}
            />

            <Input
              label="Lead Practitioner / Owner Name"
              placeholder="e.g. Dr. Rajesh Mehta or Priya Sharma"
              value={primaryStaffName}
              onChange={(e) => setPrimaryStaffName(e.target.value)}
            />

            <div className="flex flex-col gap-2 text-left">
              <label className="label-caps">Select Vertical</label>
              <div className="grid grid-cols-2 gap-4">
                <button
                  type="button"
                  onClick={() => setVertical('dental')}
                  className={`p-4 rounded-[var(--radius-ctl)] border text-left transition-all ${
                    vertical === 'dental'
                      ? 'border-[var(--accent)] bg-[var(--accent-soft)]/30 ring-1 ring-[var(--accent)]'
                      : 'border-[var(--border)] hover:bg-[var(--surface-2)]'
                  }`}
                >
                  <span className="block font-medium text-sm text-[var(--text)]">Dental Clinic</span>
                  <span className="block text-xs text-[var(--text-2)] mt-1">
                    Patients, Dentists, Scaling, Consultations, Root canals
                  </span>
                </button>

                <button
                  type="button"
                  onClick={() => setVertical('salon')}
                  className={`p-4 rounded-[var(--radius-ctl)] border text-left transition-all ${
                    vertical === 'salon'
                      ? 'border-[var(--accent)] bg-[var(--accent-soft)]/30 ring-1 ring-[var(--accent)]'
                      : 'border-[var(--border)] hover:bg-[var(--surface-2)]'
                  }`}
                >
                  <span className="block font-medium text-sm text-[var(--text)]">Salon & Beauty</span>
                  <span className="block text-xs text-[var(--text-2)] mt-1">
                    Clients, Stylists, Haircuts, Colouring, Treatments
                  </span>
                </button>
              </div>
            </div>

            <Button
              variant="primary"
              size="lg"
              disabled={loading}
              onClick={handleSeedTemplate}
              className="w-full mt-4"
            >
              {loading ? 'Seeding Configuration...' : 'Apply & Continue →'}
            </Button>
          </div>
        </Card>
      )}

      {/* Step 2: Services Customization */}
      {step === 2 && (
        <Card className="max-w-2xl mx-auto">
          <div className="flex justify-between items-center mb-6">
            <h2 className="font-display text-xl text-[var(--text)]">Pre-Configured Services</h2>
            <Button variant="secondary" size="sm" onClick={handleAddService}>
              + Add Service
            </Button>
          </div>

          <div className="flex flex-col gap-4">
            {services.map((srv, idx) => (
              <div
                key={idx}
                className="grid grid-cols-12 gap-3 items-center p-3 rounded-[var(--radius-ctl)] bg-[var(--surface-2)] border border-[var(--border)]"
              >
                <div className="col-span-6">
                  <Input
                    placeholder="Service Name"
                    value={srv.name}
                    onChange={(e) => updateService(idx, 'name', e.target.value)}
                  />
                </div>
                <div className="col-span-3">
                  <Input
                    type="number"
                    placeholder="Mins"
                    value={srv.durationMin}
                    onChange={(e) => updateService(idx, 'durationMin', parseInt(e.target.value) || 15)}
                  />
                </div>
                <div className="col-span-3">
                  <Input
                    type="number"
                    placeholder="₹ (INR)"
                    value={srv.pricePaise / 100}
                    onChange={(e) => updateService(idx, 'pricePaise', (parseInt(e.target.value) || 0) * 100)}
                  />
                </div>
              </div>
            ))}
          </div>

          <div className="flex justify-between mt-8 pt-4 border-t border-[var(--border)]">
            <Button variant="ghost" onClick={() => setStep(1)}>
              ← Back
            </Button>
            <Button variant="primary" onClick={() => setStep(3)}>
              Continue to Hours →
            </Button>
          </div>
        </Card>
      )}

      {/* Step 3: Hours Review */}
      {step === 3 && (
        <Card className="max-w-xl mx-auto">
          <div className="space-y-4">
            <div className="p-4 rounded-[var(--radius-ctl)] bg-[var(--surface-2)] border border-[var(--border)]">
              <span className="font-medium text-sm text-[var(--text)] block">Standard Business Week</span>
              <p className="text-xs text-[var(--text-2)] mt-1">
                Monday to Saturday: 9:00 AM – 6:00 PM (Sunday closed).
              </p>
              <span className="inline-block mt-2 text-xs text-[var(--accent)] font-medium">
                15-minute slot intervals enabled
              </span>
            </div>
          </div>

          <div className="flex justify-between mt-8 pt-4 border-t border-[var(--border)]">
            <Button variant="ghost" onClick={() => setStep(2)}>
              ← Back
            </Button>
            <Button variant="primary" onClick={() => setStep(4)}>
              Finish Setup →
            </Button>
          </div>
        </Card>
      )}

      {/* Step 4: Complete */}
      {step === 4 && (
        <Card className="max-w-md mx-auto text-center py-8">
          <div className="w-16 h-16 rounded-full bg-[var(--accent-soft)] text-[var(--accent)] text-2xl flex items-center justify-center mx-auto mb-4">
            ✓
          </div>
          <h2 className="font-display text-2xl text-[var(--text)] mb-2">Practice Ready!</h2>
          <p className="text-sm text-[var(--text-2)] mb-8">
            Your {vertical === 'dental' ? 'clinic' : 'salon'} is initialized with default services, hours, and dynamic labels.
          </p>
          <div className="flex flex-col gap-3">
            <Button
              variant="primary"
              size="lg"
              onClick={() => (window.location.href = '/customers')}
            >
              Go to Customer Directory
            </Button>
          </div>
        </Card>
      )}
    </div>
  );
}
