import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Staff Portal | Booking & Appointments',
  description: 'Front-desk and scheduling operations for modern practices',
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="min-h-screen bg-[var(--bg)] text-[var(--text)] font-ui antialiased">
        {children}
      </body>
    </html>
  );
}
