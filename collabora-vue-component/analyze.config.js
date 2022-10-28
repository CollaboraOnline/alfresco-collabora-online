import { visualizer } from "rollup-plugin-visualizer";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
import { resolve } from "path";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    visualizer({
      open: true,
      title: "Collabora Vue application analysis",
    }),
  ],
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
});
