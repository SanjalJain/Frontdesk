'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Card, Input, Button } from '@booking/ui';

function slugify(text: string): string {
  let s = text
    .toLowerCase()
    .trim()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .slice(0, 40);

  if (s.length > 0 && s.length < 3) {
    s = s + '-care';
  }
  return s;
}

export default function SignupPage() {
  const [practiceName, setPracticeName] = useState('');
  const [slug, setSlug] = useState('');
  const [customizingSlug, setCustomizingSlug] = useState(false);
  const [vertical, setVertical] = useState('dental');
  const [ownerName, setOwnerName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  // Specific field errors mapped directly to inputs
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [generalError, setGeneralError] = useState('');
  const [loading, setLoading] = useState(false);

  const handlePracticeNameChange = (val: string) => {
    setPracticeName(val);
    if (!customizingSlug) {
      setSlug(slugify(val));
    }
    if (fieldErrors.practiceName) {
      setFieldErrors((prev) => ({ ...prev, practiceName: '' }));
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setGeneralError('');
    setFieldErrors({});

    // Client-side quick checks with clear, human-friendly messages
    const errors: Record<string, string> = {};
    if (!practiceName.trim()) {
      errors.practiceName = 'Please enter your practice or business name.';
    }

    let finalSlug = slug.trim() || slugify(practiceName);
    if (!finalSlug || finalSlug.length < 3) {
      finalSlug = 'practice-' + Math.floor(1000 + Math.random() * 9000);
    }

    if (!ownerName.trim()) {
      errors.ownerName = 'Please enter the owner or lead practitioner’s name.';
    }
    if (!email.trim() || !email.includes('@')) {
      errors.email = 'Please enter a valid email address (e.g. doctor@clinic.com).';
    }
    if (!password || password.length < 6) {
      errors.password = 'Password must be at least 6 characters long.';
    }

    if (Object.keys(errors).length > 0) {
      setFieldErrors(errors);
      setGeneralError('Please fill in all required fields highlighted below.');
      return;
    }

    setLoading(true);

    try {
      console.log('Submitting signup payload:', {
        practiceName: practiceName.trim(),
        slug: finalSlug,
        vertical,
        ownerName: ownerName.trim(),
        email: email.trim().toLowerCase(),
      });

      const res = await fetch('/api/v1/biz/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          practiceName: practiceName.trim(),
          slug: finalSlug,
          vertical,
          ownerName: ownerName.trim(),
          email: email.trim().toLowerCase(),
          password,
        }),
      });

      let data: any = {};
      try {
        data = await res.json();
      } catch (parseErr) {
        data = { message: `Server error (${res.status} ${res.statusText || 'Unknown'})` };
      }

      if (!res.ok) {
        if (data.fieldErrors) {
          setFieldErrors(data.fieldErrors);
          if (data.fieldErrors.slug) setCustomizingSlug(true);
        } else if (data.error === 'SLUG_TAKEN') {
          setFieldErrors({ slug: data.message });
          setCustomizingSlug(true);
        } else if (data.error === 'EMAIL_EXISTS') {
          setFieldErrors({ email: data.message });
        } else {
          setGeneralError(data.message || 'Failed to register practice.');
        }
        return;
      }

      console.log('Signup successful, response:', data);

      if (typeof window !== 'undefined') {
        localStorage.setItem('tenant_vertical', vertical);
      }

      // Redirect straight to onboarding wizard
      window.location.href = '/setup/onboarding';
    } catch (err: any) {
      console.error('Signup error:', err);
      setGeneralError(err.message || 'A network error occurred. Please verify backend is running on port 8080.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-[var(--bg)]">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <span className="label-caps">Register Practice</span>
          <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-2">
            Get started with your practice
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-1">
            Create your account and launch appointment scheduling in minutes.
          </p>
        </div>

        <Card padding="md" className="border border-[var(--border)]">
          <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-4">
            {/* Practice Name */}
            <div>
              <Input
                label="Practice / Business Name"
                placeholder="e.g. Apex Dental Studio or Luxe Hair Salon"
                value={practiceName}
                error={fieldErrors.practiceName}
                onChange={(e) => handlePracticeNameChange(e.target.value)}
              />

              {/* Live URL Preview & Slug Option */}
              <div className="mt-2 flex items-center justify-between text-xs text-[var(--text-2)]">
                <span className="tabular-nums font-mono text-[var(--text-3)]">
                  Booking Link:{' '}
                  <strong className="text-[var(--text)] font-semibold">
                    /{slug || slugify(practiceName) || 'your-practice'}
                  </strong>
                </span>
                <button
                  type="button"
                  onClick={() => setCustomizingSlug(!customizingSlug)}
                  className="text-[var(--accent)] hover:underline ml-2"
                >
                  {customizingSlug ? 'Auto-generate' : 'Customize URL'}
                </button>
              </div>

              {customizingSlug && (
                <div className="mt-2">
                  <Input
                    label="Custom URL Slug"
                    placeholder="apex-dental"
                    value={slug}
                    error={fieldErrors.slug}
                    hint="Only lowercase letters, numbers, and hyphens (3-40 characters)."
                    onChange={(e) => {
                      setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9-]+/g, ''));
                      if (fieldErrors.slug) setFieldErrors((prev) => ({ ...prev, slug: '' }));
                    }}
                  />
                </div>
              )}
            </div>

            {/* Vertical Selection */}
            <div className="flex flex-col gap-1 text-left">
              <label className="label-caps">Practice Type</label>
              <select
                value={vertical}
                onChange={(e) => setVertical(e.target.value)}
                className="h-11 px-3 rounded-[var(--radius-ctl)] bg-[var(--surface-2)] text-[var(--text)] border border-transparent focus:bg-[var(--surface)] focus:border-[var(--accent)] focus:outline-none text-sm cursor-pointer"
              >
                <option value="dental">Dental Clinic (Patients, Dentists, Scaling & Polishing)</option>
                <option value="salon">Salon & Beauty (Clients, Stylists, Hair & Body Care)</option>
                <option value="generic">Generic Practice / Consultation</option>
              </select>
            </div>

            {/* Owner Credentials */}
            <div className="border-t border-[var(--border)] pt-4 mt-2">
              <span className="label-caps block mb-2">Owner Credentials</span>
              <div className="flex flex-col gap-3">
                <Input
                  label="Owner Full Name"
                  placeholder="e.g. Dr. Rajesh Mehta"
                  value={ownerName}
                  error={fieldErrors.ownerName}
                  onChange={(e) => {
                    setOwnerName(e.target.value);
                    if (fieldErrors.ownerName) setFieldErrors((prev) => ({ ...prev, ownerName: '' }));
                  }}
                />

                <Input
                  label="Email Address"
                  type="email"
                  placeholder="doctor@clinic.com"
                  value={email}
                  error={fieldErrors.email}
                  onChange={(e) => {
                    setEmail(e.target.value);
                    if (fieldErrors.email) setFieldErrors((prev) => ({ ...prev, email: '' }));
                  }}
                />

                <Input
                  label="Password"
                  type="password"
                  placeholder="At least 6 characters"
                  value={password}
                  error={fieldErrors.password}
                  hint="Minimum 6 characters."
                  onChange={(e) => {
                    setPassword(e.target.value);
                    if (fieldErrors.password) setFieldErrors((prev) => ({ ...prev, password: '' }));
                  }}
                />
              </div>
            </div>

            {/* General Alert */}
            {generalError && (
              <div className="p-3 rounded-[var(--radius-ctl)] bg-[rgba(176,122,122,0.1)] border border-[#B07A7A]/20 text-xs text-[#B07A7A] font-medium">
                {generalError}
              </div>
            )}

            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={loading}
              className="w-full mt-2 cursor-pointer"
            >
              {loading ? 'Creating Practice...' : 'Create Practice & Launch Setup →'}
            </Button>
          </form>
        </Card>

        <p className="text-center text-sm text-[var(--text-2)] mt-6">
          Already registered your practice?{' '}
          <Link href="/login" className="text-[var(--accent)] font-medium hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  );
}
