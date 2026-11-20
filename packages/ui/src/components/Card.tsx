import React from 'react';

export interface CardProps extends React.HTMLAttributes<HTMLDivElement> {
  padding?: 'none' | 'sm' | 'md' | 'lg';
}

export const Card: React.FC<CardProps> = ({
  padding = 'md',
  className = '',
  children,
  ...props
}) => {
  const paddingStyles = {
    none: 'p-0',
    sm: 'p-4',
    md: 'p-6', // 24px standard padding
    lg: 'p-8',
  }[padding];

  return (
    <div
      className={`bg-[var(--surface)] border border-[var(--border)] rounded-[var(--radius-card)] ${paddingStyles} ${className}`}
      {...props}
    >
      {children}
    </div>
  );
};
