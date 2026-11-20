'use client';

import React, { useState, useEffect } from 'react';
import { Card, Input, Button } from '@booking/ui';
import { useTenantConfig } from '@/lib/useTenantConfig';

interface StaffMember {
  id: number;
  name: string;
  title: string;
  color: string;
  isBookable: boolean;
  serviceIds: number[];
}

export default function StaffSettingsPage() {
  const { labels } = useTenantConfig();
  const [staffList, setStaffList] = useState<StaffMember[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);
  const [name, setName] = useState('');
  const [title, setTitle] = useState('');
  const [color, setColor] = useState('#A8895A');
  const [saving, setSaving] = useState(false);

  const fetchStaff = async () => {
    try {
      const res = await fetch('/api/v1/biz/setup/staff');
      if (res.ok) {
        const data = await res.json();
        setStaffList(data);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStaff();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      const res = await fetch('/api/v1/biz/setup/staff', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name,
          title,
          color,
          isBookable: true,
          sortOrder: staffList.length,
          serviceIds: [],
        }),
      });

      if (res.ok) {
        setShowModal(false);
        setName('');
        setTitle('');
        fetchStaff();
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
          <span className="label-caps">Team Directory</span>
          <h1 className="font-display text-3xl font-normal text-[var(--text)] mt-1">
            {labels.staff} Management
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-1">
            Manage bookable {labels.staff.toLowerCase()} members, titles, and calendar colors.
          </p>
        </div>
        <Button variant="primary" onClick={() => setShowModal(true)}>
          + Add {labels.staff}
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {loading ? (
          <p className="text-xs text-[var(--text-3)]">Loading {labels.staff.toLowerCase()} list...</p>
        ) : staffList.length === 0 ? (
          <Card className="col-span-full text-center py-12">
            <p className="text-sm text-[var(--text-2)]">No {labels.staff.toLowerCase()} members configured yet.</p>
          </Card>
        ) : (
          staffList.map((st) => (
            <Card key={st.id} className="flex flex-col justify-between">
              <div>
                <div className="flex items-center gap-3 mb-3">
                  <div
                    className="w-4 h-4 rounded-full border border-black/10"
                    style={{ backgroundColor: st.color }}
                  />
                  <h3 className="font-display text-lg text-[var(--text)]">{st.name}</h3>
                </div>
                <p className="text-xs text-[var(--text-2)]">{st.title || 'Practitioner'}</p>
              </div>

              <div className="mt-6 pt-4 border-t border-[var(--border)] flex justify-between items-center text-xs">
                <span className="text-[var(--text-3)]">
                  {st.serviceIds?.length ? `${st.serviceIds.length} services` : 'All services'}
                </span>
                <span className="text-[var(--accent)] font-medium">Bookable</span>
              </div>
            </Card>
          ))
        )}
      </div>

      {/* Add Staff Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 bg-black/30 flex items-center justify-center p-4">
          <Card className="w-full max-w-md bg-[var(--surface)] shadow-[var(--shadow-float)] border border-[var(--border)]">
            <div className="flex justify-between items-center mb-6">
              <h2 className="font-display text-xl text-[var(--text)]">Add {labels.staff}</h2>
              <button
                onClick={() => setShowModal(false)}
                className="text-[var(--text-3)] hover:text-[var(--text)]"
              >
                ✕
              </button>
            </div>

            <form onSubmit={handleCreate} className="flex flex-col gap-4">
              <Input
                label="Full Name"
                required
                placeholder="e.g. Dr. Sunita Rao"
                value={name}
                onChange={(e) => setName(e.target.value)}
              />

              <Input
                label="Professional Title"
                placeholder="e.g. Orthodontist / Senior Stylist"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
              />

              <div className="flex flex-col gap-1 text-left">
                <label className="label-caps">Calendar Highlight Color</label>
                <div className="flex items-center gap-3">
                  <input
                    type="color"
                    value={color}
                    onChange={(e) => setColor(e.target.value)}
                    className="w-10 h-10 p-0 border-0 rounded cursor-pointer"
                  />
                  <span className="font-mono text-xs text-[var(--text-2)] uppercase">{color}</span>
                </div>
              </div>

              <div className="flex justify-end gap-3 mt-4">
                <Button type="button" variant="secondary" onClick={() => setShowModal(false)}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" disabled={saving}>
                  {saving ? 'Saving...' : `Add ${labels.staff}`}
                </Button>
              </div>
            </form>
          </Card>
        </div>
      )}
    </div>
  );
}
