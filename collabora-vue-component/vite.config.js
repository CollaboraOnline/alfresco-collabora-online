// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
import { resolve } from "path";
import { visualizer } from "rollup-plugin-visualizer";
import path from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue(), visualizer()],
  optimizeDeps: {
    force: true,
  },
  resolve: {
    preserveSymlinks: true,
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    lib: {
      entry: resolve(__dirname, "src/index.js"),
      name: "pristy-collabora-component",
    },
    sourcemap: true,
    emptyOutDir: true,
    rollupOptions: {
      external: ["vue", "axios", "saas"],
      output: {
        // Provide global variables to use in the UMD build
        // for externalized deps
        globals: {
          vue: "Vue",
          axios: "axios",
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
