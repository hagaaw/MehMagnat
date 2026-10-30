import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Куда проксировать запросы к бэкенду при локальной разработке (npm run dev).
// По умолчанию - nginx из docker compose на порту 80.
// Можно переопределить: VITE_PROXY_TARGET=http://localhost:8080 npm run dev
const target = process.env.VITE_PROXY_TARGET || "http://localhost";

const proxy = Object.fromEntries(
  ["/api", "/csrf", "/login", "/logout"].map((path) => [
    path,
    { target, changeOrigin: true },
  ])
);

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy,
  },
  build: {
    outDir: "dist",
    sourcemap: false,
  },
});
