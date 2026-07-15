import type {NextConfig} from 'next';

const repo = 'gestor-de-dinero-con-cambio-de-divisas';
const basePath = process.env.NEXT_PUBLIC_BASE_PATH || (process.env.GITHUB_ACTIONS === 'true' ? `/${repo}` : '');

const nextConfig: NextConfig = {
  output: 'export',
  distDir: 'docs',
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

export default nextConfig;
