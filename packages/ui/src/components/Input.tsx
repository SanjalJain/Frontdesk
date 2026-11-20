import React from 'react';

export interface InputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  error?: string;
  hint?: string;
}

export const Input = React.forwardRef<HTMLInputElement, InputProps>(({
  label,
  error,
  hint,
  id,
  className = '',
  ...props
}, ref) => {
  const inputId = id || (label ? label.toLowerCase().replace(/\s+/g, '-') : undefined);

  return (
    <div className="flex flex-col gap-1 w-full text-left">
      {label && (
        <label htmlFor={inputId} className="label-caps">
          {label}
        </label>
      )}
      <input
        ref={ref}
        id={inputId}
        className={`h-11 px-3 rounded-[var(--radius-ctl)] bg-[var(--surface-2)] text-[var(--text)] border border-transparent transition-colors duration-150 focus:bg-[var(--surface)] focus:border-[var(--accent)] focus:outline-none focus:ring-1 focus:ring-[var(--accent)] disabled:opacity-50 text-sm placeholder:text-[var(--text-3)] ${
          error ? '!border-[#B07A7A] focus:!ring-[#B07A7A]' : ''
        } ${className}`}
        {...props}
      />
      {hint && !error && <span className="text-xs text-[var(--text-3)]">{hint}</span>}
      {error && <span className="text-xs text-[#B07A7A] font-medium">{error}</span>}
    </div>
  );
});

Input.displayName = 'Input';
