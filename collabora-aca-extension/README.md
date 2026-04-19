<!--
SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
SPDX-FileCopyrightText: 2026 Facundo Velazco - https://github.com/FacundoVelazco13

SPDX-License-Identifier: Apache-2.0
-->

# Collabora Online — ACA Extension

Angular library that adds Collabora Online **edit** and **view** actions to the
[Alfresco Content App (ACA)](https://github.com/Alfresco/alfresco-content-app).

When a user has write permission on a supported document, the extension exposes
an `Edit with Collabora Online` action from the toolbar, context menu and
viewer. Documents open in an iframe served by a Collabora Online server and
multiple users can edit the same document concurrently.

## Credits

This module was largely written by **[Facundo Velazco](https://github.com/FacundoVelazco13)**, who contributed the Angular 19 rewrite targeting the
current [Alfresco Content App](https://github.com/Alfresco/alfresco-content-app) (components, services, NgRx effects, viewer
integration and extension configuration). His work brought the extension back
to life on a modern ACA stack — thank you!

## Requirements

- Angular 19.2+
- ADF 8.x (`@alfresco/adf-core`, `@alfresco/adf-extensions`, `@alfresco/adf-content-services`)
- `@alfresco/aca-shared` 7.3+
- A running Collabora Online server
- Alfresco Content Services with the companion [`collabora-platform-extension`](../collabora-platform-extension) AMP installed (it exposes the WOPI endpoints this library calls)

## Installation

```bash
npm install @pristy/aca-collabora-extension
```

## Usage

Register the extension in your ACA application's providers:

```ts
import { provideCollaboraExtension } from '@pristy/aca-collabora-extension';

bootstrapApplication(AppComponent, {
  providers: [
    // ...existing providers
    provideCollaboraExtension()
  ]
});
```

`provideCollaboraExtension()` registers:

- the `collabora-online` route (`/collabora-online/:action/:nodeId`) bound to `CollaboraOnlineComponent`
- toolbar, context-menu and viewer entries (`collabora.plugin.viewer.edit` / `.view`)
- translations under `assets/collabora-extension`
- NgRx effects for the Collabora actions
- the `collabora.canEditWithCollaboraOnline` / `canViewWithCollaboraOnline` rule evaluators

## Configuration

The extension reads runtime flags from ACA's `app.config.json`:

| Key                 | Type       | Description                                     |
| ------------------- | ---------- | ----------------------------------------------- |
| `collabora.enable`  | `boolean`  | Master switch for the extension                 |
| `collabora.edit`    | `string[]` | File extensions openable in edit mode           |
| `collabora.view`    | `string[]` | File extensions openable in view-only mode      |

Example:

```json
{
  "collabora": {
    "enable": true,
    "edit": ["odt", "ods", "odp", "docx", "xlsx", "pptx"],
    "view": ["pdf"]
  }
}
```

## Development

This package is a standalone Angular library

```bash
npm install                # install deps
npm run build              # build into ./dist (ng-packagr)
npm run test               # single-run tests (karma + jasmine)
npm run test:watch         # watch mode
npm run lint               # eslint
```

The build artifact is published from `./dist/`.

## Changelog

- v0.1.0:
  - standalone build (flatten from `projects/collabora-extension/` to the module root, drop Nx `project.json`)
  - Angular 19 peer/dev dependencies pinned via npm `overrides`
  - patch `serialize-javascript` to 7.0.5 (GHSA-5c6j-r48x-rmvq, GHSA-qj8w-gfj5-8c6v)
  - fix TypeScript strict-mode errors (definite assignment, `override` modifiers, `RuleEvaluator` cast)
