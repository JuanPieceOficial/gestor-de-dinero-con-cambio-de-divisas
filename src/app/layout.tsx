import type {Metadata} from 'next';
import './globals.css';
import { Toaster } from "@/components/ui/toaster";

import type {Viewport} from 'next';

export const metadata: Metadata = {
  title: 'GestorFácil - Controla tus Finanzas',
  description: 'Gestor de dinero inteligente para controlar ingresos, gastos y presupuestos.',
  manifest: '/manifest.json',
};

export const viewport: Viewport = {
  themeColor: '#3b82f6',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="es">
      <head>
        <link rel="preconnect" href="https://fonts.googleapis.com" />
        <link rel="preconnect" href="https://fonts.gstatic.com" crossOrigin="anonymous" />
        <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet" />
        <link rel="manifest" href="/manifest.json" />
        <meta name="theme-color" content="#3b82f6" />
        <meta name="apple-mobile-web-app-capable" content="yes" />
        <meta name="apple-mobile-web-app-status-bar-style" content="default" />
        <meta name="apple-mobile-web-app-title" content="GestorFácil" />
        <link rel="apple-touch-icon" href="/icons/icon-192x192.png" />
      </head>
      <body className="font-body antialiased bg-background text-foreground min-h-screen">
        {children}
        <Toaster />
      </body>
    </html>
  );
}
