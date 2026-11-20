import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Appointments & Booking',
  description: 'Book your appointment online',
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
