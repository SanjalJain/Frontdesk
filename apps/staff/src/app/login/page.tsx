'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { Card, Input, Button } from '@booking/ui';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const res = await fetch('/api/v1/biz/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      if (!res.ok) {
        if (res.status === 401) {
          throw new Error('Invalid email or password.');
        } else if (res.status === 403) {
          throw new Error('Access denied. Clinic staff only.');
        } else {
          throw new Error('Failed to sign in. Please try again.');
        }
      }

      const data = await res.json();
      window.location.href = '/front-desk';
    } catch (err: any) {
      setError(err.message || 'An error occurred.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center p-4 bg-[var(--bg)]">
      <div className="w-full max-w-md">
        <div className="text-center mb-8">
          <span className="label-caps">Front-Desk Workspace</span>
          <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-2">
            Welcome back
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-1">
            Sign in to manage appointments, patients, and schedule.
          </p>
        </div>

        <Card padding="md" className="border border-[var(--border)]">
          <form onSubmit={handleSubmit} className="flex flex-col gap-5">
            <Input
              label="Staff Email"
              type="email"
              required
              placeholder="dentist@clinic.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />

            <Input
              label="Password"
              type="password"
              required
              placeholder="••••••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />

            {error && (
              <div className="p-3 rounded-[var(--radius-ctl)] bg-[rgba(176,122,122,0.1)] border border-[#B07A7A]/20 text-xs text-[#B07A7A] font-medium">
                {error}
              </div>
            )}

            <Button
              type="submit"
              variant="primary"
              size="lg"
              disabled={loading}
              className="w-full mt-2"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </Button>
          </form>
        </Card>

        <p className="text-center text-sm text-[var(--text-2)] mt-6">
          Don&apos;t have an account yet?{' '}
          <Link href="/signup" className="text-[var(--accent)] font-medium hover:underline">
            Register your practice
          </Link>
        </p>

        <p className="text-center text-xs text-[var(--text-3)] mt-4">
          Booking &amp; Appointment Platform · Secure Session Protected
        </p>
      </div>
    </div>
  );
}
