import globals from "globals";
import js from "@eslint/js";
import pluginVue from "eslint-plugin-vue";

export default [
  {
    ignores: ["**/.*", "**/dist"],
  },
  {
    files: ["src/**/*.js", "src/main.js"],
    ...js.configs.recommended,
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: {
        ...globals.node,
      },
    },
  },
  {
    files: ["**/*.mjs"],
    languageOptions: { sourceType: "module" },
  },
  ...pluginVue.configs["flat/essential"],
  {
    rules: {
      "vue/no-lone-template": "warn",
      "vue/no-multiple-slot-args": "warn",
      "vue/no-v-html": "warn",
      "vue/this-in-template": "warn",
      "vue/no-template-shadow": "warn",
      "vue/one-component-per-file": "warn",
      "vue/require-explicit-emits": "warn",
      "vue/require-prop-types": "warn",
      "vue/require-default-prop": "warn",
    },
  },
];
