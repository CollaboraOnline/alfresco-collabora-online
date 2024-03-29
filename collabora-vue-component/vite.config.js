import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import eslintPlugin from "vite-plugin-eslint";
import { resolve } from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [eslintPlugin(), vue()],
  build: {
    lib: {
      entry: resolve(__dirname, "src/index.js"),
      name: "pristy-collabora-component",
    },
    rollupOptions: {
      external: ["vue"],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: "Vue",
        },
      },
    },
  },
  server: {
    host: "localhost",
    port: 8008,
    https: false,
    proxy: {
      "/alfresco": {
        target: "http://localhost:8080/",
        changeOrigin: true,
      },
    },
  },
});
