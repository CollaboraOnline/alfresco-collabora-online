import { visualizer } from "rollup-plugin-visualizer";
import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    visualizer({
      open: true,
      title: "Collabora Vue application analysis",
    }),
  ],
});
