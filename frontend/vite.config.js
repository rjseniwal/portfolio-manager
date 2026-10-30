import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  return {
    plugins: [react(), tailwindcss()],
    server: {
      proxy: {
        '/backend': {
          target: env.BACKEND_ORIGIN || 'http://localhost:9090',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/backend(?=\/|$)/, ''),
        },
      },
    },
  };
});
