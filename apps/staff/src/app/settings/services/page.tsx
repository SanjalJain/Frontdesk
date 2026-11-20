'use client';

import React, { useState, useEffect } from 'react';
import { Card, Input, Button } from '@booking/ui';

interface Service {
  id: number;
  name: string;
  durationMin: number;
  bufferAfterMin: number;
  pricePaise: number;
  isActive: boolean;
}

export default function ServicesSettingsPage() {
  const [services, setServices] = useState<Service[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [durationMin, setDurationMin] = useState(30);
  const [bufferAfterMin, setBufferAfterMin] = useState(0);
  const [priceInr, setPriceInr] = useState(500);
  const [saving, setSaving] = useState(false);

  const fetchServices = async () => {
    try {
      const res = await fetch('/api/v1/biz/setup/services');
      if (res.ok) {
        const data = await res.json();
        setServices(data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchServices();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await fetch('/api/v1/biz/setup/services', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name,
          durationMin,
          bufferAfterMin,
          pricePaise: priceInr * 100,
        }),
      });
      if (res.ok) {
        setShowModal(false);
        setName('');
        fetchServices();
      }
    } catch (err) {
      console.error(err);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="min-h-screen bg-[var(--bg)] p-8 max-w-6xl mx-auto">
      <div className="flex justify-between items-center pb-6 border-b border-[var(--border)] mb-8">
        <div>
          <span className="label-caps">Configuration</span>
          <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-1">
            Services & Pricing
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-1">
            Manage your service catalog, standard durations, and prices in INR.
          </p>
        </div>
        <Button variant="primary" onClick={() => setShowModal(true)}>
          + Add Service
        </Button>
      </div>

      <Card padding="none" className="overflow-hidden border border-[var(--border)]">
        <table className="w-full text-left text-sm">
          <thead className="bg-[var(--surface-2)] border-b border-[var(--border)] text-xs font-medium text-[var(--text-2)] uppercase tracking-wider">
            <tr>
              <th className="py-3 px-6">Service Name</th>
              <th className="py-3 px-6">Duration</th>
              <th className="py-3 px-6">Buffer After</th>
              <th className="py-3 px-6">Price</th>
              <th className="py-3 px-6 text-right">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-[var(--border)]">
            {loading ? (
              <tr>
                <td colSpan={5} className="py-8 text-center text-xs text-[var(--text-3)]">
                  Loading services...
                </td>
              </tr>
            ) : services.length === 0 ? (
              <tr>
                <td colSpan={5} className="py-12 text-center text-sm text-[var(--text-2)]">
                  No services configured yet.
                </td>
              </tr>
            ) : (
              services.map((s) => (
                <tr key={s.id} className="hover:bg-[var(--surface-2)]/50">
                  <td className="py-3.5 px-6 font-medium text-[var(--text)]">{s.name}</td>
                  <td className="py-3.5 px-6 tabular-nums text-xs text-[var(--text-2)]">
                    {s.durationMin} mins
                  </td>
                  <td className="py-3.5 px-6 tabular-nums text-xs text-[var(--text-3)]">
                    {s.bufferAfterMin > 0 ? `${s.bufferAfterMin} mins` : 'None'}
                  </td>
                  <td className="py-3.5 px-6 font-semibold tabular-nums text-sm text-[var(--text)]">
                    ₹{(s.pricePaise / 100).toLocaleString('en-IN')}
                  </td>
                  <td className="py-3.5 px-6 text-right">
                    <span className="text-xs font-medium text-[var(--accent)]">Active</span>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </Card>

      {/* Add Service Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/30 flex items-center justify-center p-4">
          <Card className="w-full max-w-md bg-[var(--surface)] shadow-[var(--shadow-float)] border border-[var(--border)]">
            <div className="flex justify-between items-center mb-6">
              <h2 className="font-display text-xl text-[var(--text)]">Add New Service</h2>
              <button
                onClick={() => setShowModal(false)}
                className="text-[var(--text-3)] hover:text-[var(--text)]"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreate} className="flex flex-col gap-4">
              <Input
                label="Service Name"
                required
                placeholder="e.g. Scaling & Polishing"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />

              <div className="grid grid-cols-2 gap-3">
                <Input
                  label="Duration (Minutes)"
                  type="number"
                  required
                  min={5}
                  value={durationMin}
                  onChange={(e) => setDurationMin(parseInt(e.target.value) || 15)}
                />
                <Input
                  label="Buffer After (Mins)"
                  type="number"
                  min={0}
                  value={bufferAfterMin}
                  onChange={(e) => setBufferAfterMin(parseInt(e.target.value) || 0)}
                />
              </div>

              <Input
                label="Price (₹ INR)"
                type="number"
                required
                min={0}
                value={priceInr}
                onChange={(e) => setPriceInr(parseInt(e.target.value) || 0)}
              />

              <div className="flex justify-end gap-3 mt-4">
                <Button type="button" variant="secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? 'Saving...' : 'Add Service'}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}
