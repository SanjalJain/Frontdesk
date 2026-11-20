'use client';

import React, { useState, useEffect } from 'react';
import { Card, Input, Button } from '@booking/ui';

interface Tenant {
  id: number;
  slug: string;
  name: string;
  vertical: string;
  timezone: string;
  currency: string;
  slotMinutes: number;
  isActive: boolean;
  createdAt: string;
}

export default function PlatformAdminTenantsPage() {
  const [tenants, setTenants] = useState<Tenant[]>([]);
  const [loading, setLoading] = useState(true);
  const [creating, setCreating] = useState(false);
  const [showCreateModal, setShowCreateModal] = useState(false);

  // Form fields
  const [slug, setSlug] = useState('');
  const [name, setName] = useState('');
  const [vertical, setVertical] = useState('dental');
  const [ownerEmail, setOwnerEmail] = useState('');
  const [ownerPassword, setOwnerPassword] = useState('');
  const [ownerName, setOwnerName] = useState('');
  const [error, setError] = useState('');

  const fetchTenants = async () => {
    try {
      const res = await fetch('/api/v1/admin/tenants');
      if (res.ok) {
        const data = await res.json();
        setTenants(data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTenants();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setCreating(true);

    try {
      const res = await fetch('/api/v1/admin/tenants', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          slug,
          name,
          vertical,
          timezone: 'Asia/Kolkata',
          currency: 'INR',
          slotMinutes: 15,
          ownerEmail,
          ownerPassword,
          ownerName,
        }),
      });

      if (!res.ok) {
        if (res.status === 409) {
          throw new Error('Tenant slug already taken. Choose another.');
        }
        throw new Error('Failed to create tenant.');
      }

      setShowCreateModal(false);
      setSlug('');
      setName('');
      setOwnerEmail('');
      setOwnerPassword('');
      setOwnerName('');
      fetchTenants();
    } catch (err: any) {
      setError(err.message || 'Creation error.');
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--bg)] p-8 max-w-6xl mx-auto">
      <div className="flex items-center justify-between pb-6 border-b border-[var(--border)] mb-8">
        <div>
          <span className="label-caps">Platform Admin</span>
          <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-1">
            Tenants Directory
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-1">
            Provision and oversee clinics, salons, and practices on the platform.
          </p>
        </div>
        <Button
          variant="primary"
          onClick={() => setShowCreateModal(true)}
        >
          + Provision Tenant
        </Button>
      </div>

      {showCreateModal && (
        <div className="fixed inset-0 z-50 bg-black/30 flex items-center justify-center p-4">
          <Card className="w-full max-w-lg bg-[var(--surface)] shadow-[var(--shadow-float)] border border-[var(--border)]">
            <div className="flex justify-between items-center mb-6">
              <h2 className="font-display text-xl text-[var(--text)]">Provision New Tenant</h2>
              <button
                onClick={() => setShowCreateModal(false)}
                className="text-[var(--text-3)] hover:text-[var(--text)]"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreate} className="flex flex-col gap-4">
              <Input
                label="Practice Name"
                required
                placeholder="Apex Dental Studio"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  if (!slug) {
                    setSlug(e.target.value.toLowerCase().replace(/[^a-z0-9]+/g, '-').slice(0, 30));
                  }
                }}
              />

              <Input
                label="Public Slug (URL identifier)"
                required
                pattern="^[a-z0-9-]{3,40}$"
                hint="Used for public forms, e.g. /apex-dental"
                placeholder="apex-dental"
                value={slug}
                onChange={(e) => setSlug(e.target.value.toLowerCase().trim())}
              />

              <div className="flex flex-col gap-1 text-left">
                <label className="label-caps">Vertical Template</label>
                <select
                  value={vertical}
                  onChange={(e) => setVertical(e.target.value)}
                  className="h-11 px-3 rounded-[var(--radius-ctl)] bg-[var(--surface-2)] text-[var(--text)] border border-transparent focus:bg-[var(--surface)] focus:border-[var(--accent)] focus:outline-none text-sm"
                >
                  <option value="dental">Dental Clinic (Patients, Dentists, Dental Services)</option>
                  <option value="salon">Salon (Clients, Stylists, Hair & Beauty)</option>
                  <option value="generic">Generic Practice</option>
                </select>
              </div>

              <div className="border-t border-[var(--border)] pt-4 mt-2">
                <span className="label-caps block mb-2">Initial Owner Account</span>
                <div className="grid grid-cols-2 gap-3">
                  <Input
                    label="Owner Full Name"
                    required
                    placeholder="Dr. Rajesh Mehta"
                    value={ownerName}
                    onChange={(e) => setOwnerName(e.target.value)}
                  />
                  <Input
                    label="Owner Email"
                    type="email"
                    required
                    placeholder="rajesh@apexdental.in"
                    value={ownerEmail}
                    onChange={(e) => setOwnerEmail(e.target.value)}
                  />
                </div>
                <div className="mt-3">
                  <Input
                    label="Initial Password"
                    type="password"
                    required
                    placeholder="••••••••••••"
                    value={ownerPassword}
                    onChange={(e) => setOwnerPassword(e.target.value)}
                  />
                </div>
              </div>

              {error && (
                <div className="p-3 rounded-[var(--radius-ctl)] bg-[rgba(176,122,122,0.1)] text-xs text-[#B07A7A]">
                  {error}
                </div>
              )}

              <div className="flex justify-end gap-3 mt-4">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  disabled={creating}
                >
                  {creating ? 'Provisioning...' : 'Confirm & Provision'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {loading ? (
        <p className="text-sm text-[var(--text-3)]">Loading tenants...</p>
      ) : tenants.length === 0 ? (
        <Card className="text-center py-12">
          <h3 className="font-display text-lg text-[var(--text)]">No tenants provisioned yet</h3>
          <p className="text-xs text-[var(--text-2)] mt-1 mb-4">
            Provision the first clinic or salon to get started.
          </p>
          <Button variant="primary" onClick={() => setShowCreateModal(true)}>
            Provision Tenant
          </Button>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {tenants.map((t) => (
            <Card key={t.id} className="flex flex-col justify-between hover:border-[var(--accent)] transition-colors">
              <div>
                <div className="flex justify-between items-start mb-2">
                  <span className="text-xs font-semibold px-2 py-0.5 rounded bg-[var(--surface-2)] text-[var(--text-2)] uppercase">
                    {t.vertical}
                  </span>
                  <span className="text-xs text-[var(--text-3)] tabular-nums">
                    {new Date(t.createdAt).toLocaleDateString()}
                  </span>
                </div>
                <h3 className="font-display text-xl text-[var(--text)]">{t.name}</h3>
                <p className="text-xs text-[var(--text-3)] font-mono mt-1">/{t.slug}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-[var(--border)] flex justify-between items-center text-xs text-[var(--text-2)]">
                <span>{t.timezone}</span>
                <span className="text-[var(--accent)] font-medium">Active</span>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
