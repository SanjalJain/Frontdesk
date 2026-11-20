import React from 'react';

export type AppointmentStatus =
  | 'booked'
  | 'confirmed'
  | 'arrived'
  | 'in_progress'
  | 'completed'
  | 'cancelled'
  | 'no_show';

export interface StatusPillProps {
  status: AppointmentStatus | string;
  className?: string;
}

const statusConfig: Record<string, { label: string; color: string; bg: string }> = {
  booked: { label: 'Booked', color: '#6B7A8F', bg: 'rgba(107, 122, 143, 0.12)' },
  confirmed: { label: 'Confirmed', color: '#6E8B74', bg: 'rgba(110, 139, 116, 0.12)' },
  arrived: { label: 'Arrived', color: '#A8895A', bg: 'rgba(168, 137, 90, 0.12)' },
  in_progress: { label: 'In Progress', color: '#2F5D62', bg: 'rgba(47, 93, 98, 0.12)' },
  completed: { label: 'Completed', color: '#8A857C', bg: 'rgba(138, 133, 124, 0.12)' },
  cancelled: { label: 'Cancelled', color: '#B07A7A', bg: 'rgba(176, 122, 122, 0.12)' },
  no_show: { label: 'No Show', color: '#B5694A', bg: 'rgba(181, 105, 74, 0.12)' },
};

export const StatusPill: React.FC<StatusPillProps> = ({ status, className = '' }) => {
  const normalized = status.toLowerCase();
  const config = statusConfig[normalized] || {
    label: status,
    color: '#6F6A62',
    bg: 'rgba(111, 106, 98, 0.12)',
  };

  return (
    <span
      className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium tracking-wide uppercase ${className}`}
      style={{
        color: config.color,
        backgroundColor: config.bg,
        border: `1px solid ${config.color}22`,
      }}
    >
      <span
        className="w-1.5 h-1.5 rounded-full mr-1.5"
        style={{ backgroundColor: config.color }}
      />
      {config.label}
    </span>
  );
};
