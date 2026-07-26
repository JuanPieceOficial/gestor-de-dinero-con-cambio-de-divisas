import type {NextConfig} from 'next';
import withPWAInit from '@ducanh2912/next-pwa';

const repo = 'gestor-de-dinero-con-cambio-de-divisas';
const basePath = process.env.NEXT_PUBLIC_BASE_PATH || (process.env.NODE_ENV === 'production' ? `/${repo}` : '');

const withPWA = withPWAInit({
  dest: 'public',
  register: true,
  skipWaiting: true,
  disable: process.env.NODE_ENV === 'development',
  runtimeCaching: [
    {
      urlPattern: /^https:\/\/fonts\.googleapis\.com\/.*/i,
      handler: 'CacheFirst',
      options: {
        cacheName: 'google-fonts',
        expiration: { maxEntries: 4, maxAgeSeconds: 365 * 24 * 60 * 60 },
      },
    },
    {
      urlPattern: /^https:\/\/fonts\.gstatic\.com\/.*/i,
      handler: 'CacheFirst',
      options: {
        cacheName: 'google-fonts-static',
        expiration: { maxEntries: 4, maxAgeSeconds: 365 * 24 * 60 * 60 },
      },
    },
    {
      urlPattern: /^https:\/\/xfailvysvwqieicdpnid\.supabase\.co\/rest\/v1\/.*/i,
      handler: 'NetworkFirst',
      options: {
        cacheName: 'supabase-api',
        networkTimeoutSeconds: 10,
        expiration: { maxEntries: 50, maxAgeSeconds: 24 * 60 * 60 },
      },
    },
  ],
});

const nextConfig: NextConfig = {
  output: 'export',
  basePath,
  trailingSlash: true,
  typescript: {
    ignoreBuildErrors: true,
  },
  eslint: {
    ignoreDuringBuilds: true,
  },
  images: {
    unoptimized: true,
  },
};

export default withPWA(nextConfig);
