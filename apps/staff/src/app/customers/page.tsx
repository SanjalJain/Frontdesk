'use client';

import React, { useState, useEffect } from 'react';
import { Card, Input, Button } from '@booking/ui';
import { useTenantConfig } from '@/lib/useTenantConfig';

interface Customer {
  id: number;
  fullName: string;
  phoneE164: string;
  email?: string;
  dob?: string;
  notes?: string;
  intakeJson?: string;
  createdAt: string;
}

export default function CustomersPage() {
  const { labels } = useTenantConfig();
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);
  const [selectedCustomer, setSelectedCustomer] = useState<Customer | null>(null);

  // New Customer Modal
  const [showModal, setShowModal] = useState(false);
  const [fullName, setFullName] = useState('');
  const [phone, setPhone] = useState('');
  const [email, setEmail] = useState('');
  const [notes, setNotes] = useState('');
  const [intakeData, setIntakeData] = useState<Record<string, string>>({});
  const [error, setError] = useState<{ message: string; existingId?: number } | null>(null);
  const [saving, setSaving] = useState(false);

  const fetchCustomers = async (q = '') => {
    try {
      const res = await fetch(`/api/v1/biz/customers?q=${encodeURIComponent(q)}&limit=30`);
      if (res.ok) {
        const data = await res.json();
        setCustomers(data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCustomers(searchQuery);
  }, [searchQuery]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSaving(true);

    try {
      const res = await fetch('/api/v1/biz/customers', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          fullName,
          phone,
          email: email || undefined,
          notes: notes || undefined,
          intakeJson: JSON.stringify(intakeData),
        }),
      });

      const data = await res.json();

      if (!res.ok) {
        if (res.status === 409 && data.existingCustomerId) {
          setError({
            message: data.message || `Phone number already registered.`,
            existingId: data.existingCustomerId,
          });
          return;
        }
        throw new Error(data.message || 'Failed to save customer.');
      }

      setShowModal(false);
      setFullName('');
      setPhone('');
      setEmail('');
      setNotes('');
      setIntakeData({});
      fetchCustomers();
      setSelectedCustomer(data);
    } catch (err: any) {
      setError({ message: err.message || 'An error occurred.' });
    } finally {
      setSaving(false);
    }
  };

  const openExistingCustomer = (id: number) => {
    const existing = customers.find((c) => c.id === id);
    if (existing) {
      setSelectedCustomer(existing);
      setShowModal(false);
      setError(null);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--bg)] p-8 max-w-7xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 pb-6 border-b border-[var(--border)] mb-8">
        <div>
          <span className="label-caps">{labels.customer} Directory</span>
          <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-1">
            {labels.customer}s
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-1">
            Phone-first lookups, visit history, and {labels.customer.toLowerCase()} records.
          </p>
        </div>

        <Button variant="primary" onClick={() => setShowModal(true)}>
          + New {labels.customer}
        </Button>
      </div>

      {/* Phone-First Search Bar */}
      <div className="mb-6 max-w-md">
        <Input
          placeholder={`Search by phone prefix or name (e.g. 98765 or Rajesh)...`}
          value={searchQuery}
          onChange={(e) => setSearchQuery(e.target.value)}
          className="h-12 text-base font-normal bg-[var(--surface)]"
        />
      </div>

      {/* Customers Table */}
      <Card padding="none" className="overflow-hidden border border-[var(--border)]">
        <table className="w-full text-left text-sm">
          <thead className="bg-[var(--surface-2)] border-b border-[var(--border)] text-xs font-medium text-[var(--text-2)] uppercase tracking-wider">
            <tr>
              <th className="py-3 px-6">Name</th>
              <th className="py-3 px-6">Phone Number</th>
              <th className="py-3 px-6">Email</th>
              <th className="py-3 px-6">Added On</th>
              <th className="py-3 px-6 text-right">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--border)]">
            {loading ? (
              <tr>
                <td colSpan={5} className="py-8 text-center text-xs text-[var(--text-3)]">
                  Loading records...
                </td>
              </tr>
            ) : customers.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-12 text-center">
                  <p className="font-display text-base text-[var(--text)]">No records found</p>
                  <p className="text-xs text-[var(--text-2)] mt-1">
                    Try searching for another phone number or create a new {labels.customer.toLowerCase()}.
                  </p>
                </td>
              </tr>
            ) : (
              customers.map((c) => (
                <tr
                  key={c.id}
                  onClick={() => setSelectedCustomer(c)}
                  className="hover:bg-[var(--surface-2)]/60 cursor-pointer transition-colors"
                >
                  <td className="py-3.5 px-6 font-medium text-[var(--text)]">{c.fullName}</td>
                  <td className="py-3.5 px-6 font-mono text-xs tabular-nums text-[var(--text-2)]">
                    {c.phoneE164}
                  </td>
                  <td className="py-3.5 px-6 text-xs text-[var(--text-3)]">{c.email || '—'}</td>
                  <td className="py-3.5 px-6 text-xs tabular-nums text-[var(--text-3)]">
                    {new Date(c.createdAt).toLocaleDateString()}
                  </td>
                  <td className="py-3.5 px-6 text-right">
                    <span className="text-xs text-[var(--accent)] hover:underline font-medium">
                      View Profile →
                    </span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </Card>

      {/* New Customer Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/30 flex items-center justify-center p-4">
          <Card className="w-full max-w-lg bg-[var(--surface)] shadow-[var(--shadow-float)] border border-[var(--border)]">
            <div className="flex justify-between items-center mb-6">
              <h2 className="font-display text-xl text-[var(--text)]">New {labels.customer}</h2>
              <button
                onClick={() => {
                  setShowModal(false);
                  setError(null);
                }}
                className="text-[var(--text-3)] hover:text-[var(--text)]"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreate} className="flex flex-col gap-4">
              <Input
                label="Full Name"
                required
                placeholder="e.g. Ramesh Patel"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
              />

              <Input
                label="Phone Number"
                required
                placeholder="e.g. 9876543210 (Normalized to E.164)"
                hint="Normalized automatically to +91..."
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
              />

              <Input
                label="Email (Optional)"
                type="email"
                placeholder="ramesh@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />

              {/* Dynamic Intake Fields from Vertical Template */}
              {labels.intakeFields.length > 0 && (
                <div className="border-t border-[var(--border)] pt-4 mt-2">
                  <span className="label-caps block mb-2">{labels.customer} Intake Notes</span>
                  <div className="grid grid-cols-1 gap-3">
                    {labels.intakeFields.map((field) => (
                      <Input
                        key={field}
                        label={field.replace(/_/g, ' ')}
                        placeholder={`Details for ${field.replace(/_/g, ' ')}...`}
                        value={intakeData[field] || ''}
                        onChange={(e) =>
                          setIntakeData({ ...intakeData, [field]: e.target.value })
                        }
                      />
                    ))}
                  </div>
                </div>
              )}

              <Input
                label="General Notes"
                placeholder="Any special preferences or clinical remarks..."
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
              />

              {error && (
                <div className="p-3 rounded-[var(--radius-ctl)] bg-[rgba(176,122,122,0.1)] border border-[#B07A7A]/20 text-xs text-[#B07A7A]">
                  <p>{error.message}</p>
                  {error.existingId && (
                    <button
                      type="button"
                      onClick={() => openExistingCustomer(error.existingId!)}
                      className="mt-2 text-xs font-semibold underline text-[var(--accent)] block"
                    >
                      Click here to open existing profile →
                    </button>
                  )}
                </div>
              )}

              <div className="flex justify-end gap-3 mt-4">
                <Button
                  type="button"
                  variant="secondary"
                  onClick={() => {
                    setShowModal(false);
                    setError(null);
                  }}
                >
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? 'Saving...' : `Save ${labels.customer}`}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}

      {/* Customer Profile Drawer (420px slide-in from right) */}
      {selectedCustomer && (
        <div className="fixed inset-0 z-50 overflow-hidden">
          <div
            className="absolute inset-0 bg-black/20 transition-opacity"
            onClick={() => setSelectedCustomer(null)}
          />

          <div className="fixed inset-y-0 right-0 max-w-full flex">
            <div className="w-screen max-w-[420px] bg-[var(--surface)] border-l border-[var(--border)] shadow-[var(--shadow-float)] p-6 flex flex-col justify-between">
              <div>
                <div className="flex justify-between items-start pb-4 border-b border-[var(--border)]">
                  <div>
                    <span className="label-caps">{labels.customer} Profile</span>
                    <h2 className="font-display text-2xl text-[var(--text)] mt-1">
                      {selectedCustomer.fullName}
                    </h2>
                  </div>
                  <button
                    onClick={() => setSelectedCustomer(null)}
                    className="text-[var(--text-3)] hover:text-[var(--text)]"
                  >
                    ✕
                  </button>
                </div>

                <div className="mt-6 space-y-4 text-sm">
                  <div>
                    <span className="label-caps">Phone (E.164)</span>
                    <p className="font-mono tabular-nums text-sm font-medium text-[var(--text)] mt-0.5">
                      {selectedCustomer.phoneE164}
                    </p>
                  </div>

                  <div>
                    <span className="label-caps">Email</span>
                    <p className="text-sm text-[var(--text-2)] mt-0.5">
                      {selectedCustomer.email || 'None on file'}
                    </p>
                  </div>

                  <div>
                    <span className="label-caps">Registration Date</span>
                    <p className="text-xs tabular-nums text-[var(--text-3)] mt-0.5">
                      {new Date(selectedCustomer.createdAt).toLocaleString()}
                    </p>
                  </div>

                  {selectedCustomer.notes && (
                    <div className="pt-2">
                      <span className="label-caps">Notes</span>
                      <p className="text-sm text-[var(--text-2)] bg-[var(--surface-2)] p-3 rounded-[var(--radius-ctl)] mt-1">
                        {selectedCustomer.notes}
                      </p>
                    </div>
                  )}

                  {selectedCustomer.intakeJson && selectedCustomer.intakeJson !== '{}' && (
                    <div className="pt-2">
                      <span className="label-caps">Intake Details</span>
                      <div className="bg-[var(--surface-2)] p-3 rounded-[var(--radius-ctl)] mt-1 space-y-2 text-xs">
                        {Object.entries((JSON.parse(selectedCustomer.intakeJson) || {}) as Record<string, any>).map(
                          ([k, v]) =>
                            v ? (
                              <div key={k}>
                                <span className="font-semibold text-[var(--text-2)] capitalize">
                                  {k.replace(/_/g, ' ')}:
                                </span>{' '}
                                <span className="text-[var(--text)]">{String(v)}</span>
                              </div>
                            ) : null
                        )}
                      </div>
                    </div>
                  )}
                </div>
              </div>

              <div className="pt-6 border-t border-[var(--border)]">
                <Button
                  variant="primary"
                  className="w-full"
                  onClick={() => alert(`Phase 3 booking drawer will pre-fill this ${labels.customer.toLowerCase()}`)}
                >
                  Book {labels.appointment}
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
