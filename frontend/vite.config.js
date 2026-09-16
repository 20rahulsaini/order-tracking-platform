import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The proxy lets the dev server forward API calls to the backend services
// so the React app can use same-origin URLs during local development.
export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/api/orders': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
      '/api/tracking': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/delivery': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
    },
  },
});
