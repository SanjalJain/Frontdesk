import type { Config } from 'tailwindcss';

const config: Config = {
  content: [
    './src/**/*.{js,ts,jsx,tsx,mdx}',
    '../../packages/ui/src/**/*.{js,ts,jsx,tsx}',
  ],
  theme: {
    extend: {
      colors: {
        bg: 'var(--bg)',
        surface: 'var(--surface)',
        'surface-2': 'var(--surface-2)',
        border: 'var(--border)',
        text: 'var(--text)',
        'text-2': 'var(--text-2)',
        'text-3': 'var(--text-3)',
        accent: 'var(--accent)',
        'accent-soft': 'var(--accent-soft)',
      },
      fontFamily: {
        display: ['var(--font-display)', 'Georgia', 'serif'],
        ui: ['var(--font-ui)', 'system-ui', 'sans-serif'],
      },
      borderRadius: {
        card: 'var(--radius-card)',
        ctl: 'var(--radius-ctl)',
        pill: 'var(--radius-pill)',
      },
      boxShadow: {
        float: 'var(--shadow-float)',
      },
    },
  },
  plugins: [],
};

export default config;
