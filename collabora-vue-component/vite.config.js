import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import eslintPlugin from "vite-plugin-eslint";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [eslintPlugin(), vue()],
  build: {},
  server: {
    host: "localhost",
    port: 8008,
    https: false,
    proxy: {},
  },
});
