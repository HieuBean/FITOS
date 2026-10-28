import '@/styles/globals.css';
import { AuthProvider } from '@/context/AuthContext';

export const metadata = {
  title: 'FITOS – Gym & Studio Management',
  description: 'FITOS connects studio operations, expert coaching and member progress in one focused platform.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <head>
        <meta charSet="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      </head>
      <body>
        <AuthProvider>
          {children}
        </AuthProvider>
      </body>
    </html>
  );
}
