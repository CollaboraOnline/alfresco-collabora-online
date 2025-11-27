// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

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
      curly: ["error", "all"],
      "vue/no-lone-template": "error",
      "vue/no-multiple-slot-args": "error",
      "vue/no-v-html": "error",
      "vue/this-in-template": "error",
      "vue/no-template-shadow": "error",
      "vue/one-component-per-file": "error",
      "vue/require-explicit-emits": "error",
      "vue/require-prop-types": "warn",
      "vue/require-default-prop": "warn",
      "vue/no-root-v-if": "warn",
      "vue/no-static-inline-styles": "warn",
      "vue/no-this-in-before-route-enter": "error",
      "vue/no-useless-mustaches": "error",
      "vue/prefer-separate-static-class": "warn",
      "vue/prefer-true-attribute-shorthand": "warn",
      "vue/component-definition-name-casing": ["error", "PascalCase"],
      "vue/mustache-interpolation-spacing": ["error", "always"],
      "vue/no-multi-spaces": [
        "error",
        {
          ignoreProperties: false,
        },
      ],
      "vue/prop-name-casing": [
        "error",
        "camelCase",
        {
          ignoreProps: [],
        },
      ],
    },
  },
];
