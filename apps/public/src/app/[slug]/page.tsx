import React from 'react';
import { Card, Button } from '@booking/ui';

interface PageProps {
  params: Promise<{ slug: string }>;
}

export default async function PublicTenantPage({ params }: PageProps) {
  const { slug } = await params;

  return (
    <div className="min-h-screen bg-[var(--bg)] py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto">
        <div className="text-center mb-8">
          <span className="label-caps">Verified Practice</span>
          <h1 className="font-display text-4xl font-normal text-[var(--text)] mt-2 capitalize">
            {slug.replace(/-/g, ' ')}
          </h1>
          <p className="text-sm text-[var(--text-2)] mt-2">
            Online consultation and appointment requests.
          </p>
        </div>

        <Card padding="lg" className="border border-[var(--border)]">
          <div className="text-center py-8">
            <h2 className="font-display text-2xl text-[var(--text)] mb-2">
              Request an Appointment
            </h2>
            <p className="text-sm text-[var(--text-2)] max-w-md mx-auto mb-6">
              Our front desk will review your preferred time and contact you to confirm your visit.
            </p>

            <div className="flex justify-center gap-4">
              <Button variant="primary" size="lg">
                Request Appointment
              </Button>
              <Button variant="secondary" size="lg">
                Request Call Back
              </Button>
            </div>
          </div>
        </Card>

        <p className="text-center text-xs text-[var(--text-3)] mt-8">
          Powered by Booking & Appointment Platform
        </p>
      </div>
    </div>
  );
}
