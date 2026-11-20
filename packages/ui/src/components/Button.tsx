import React from 'react';

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'ghost' | 'accent';
  size?: 'sm' | 'md' | 'lg';
}

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(({
  variant = 'primary',
  size = 'md',
  className = '',
  children,
  ...props
}, ref) => {
  const baseStyles = 'inline-flex items-center justify-center font-medium transition-colors duration-150 rounded-[var(--radius-ctl)] focus:outline-none focus:ring-2 focus:ring-[var(--accent)] disabled:opacity-50 disabled:pointer-events-none';
  
  const sizeStyles = {
    sm: 'h-8 px-3 text-xs',
    md: 'h-10 px-4 text-sm',
    lg: 'h-12 px-6 text-base font-semibold',
  }[size];

  const variantStyles = {
    primary: 'bg-[var(--text)] text-[var(--bg)] hover:bg-[#2c2a27]',
    secondary: 'border border-[var(--border)] bg-transparent text-[var(--text)] hover:bg-[var(--surface-2)]',
    ghost: 'bg-transparent text-[var(--text)] hover:bg-[var(--surface-2)]',
    accent: 'bg-[var(--accent)] text-white hover:opacity-90',
  }[variant];

  return (
    <button
      ref={ref}
      className={`${baseStyles} ${sizeStyles} ${variantStyles} ${className}`}
      {...props}
    >
      {children}
    </button>
  );
});

Button.displayName = 'Button';
