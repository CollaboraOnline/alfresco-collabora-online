# ACA Extension Collabora

Extension for [Alfresco-Content-Application](https://github.com/Alfresco/alfresco-content-app) to edit document with Collabora Online.
This extension add a button and open a iFrame to modify document.
## Building

Run the following script to build the library

```sh
npm run build:collabora-online-extension
```

## Publishing

```sh
cd dist/@jeci/collabora-online-extension
npm publish --access=public
```

## Testing with local ACA instance

Build and package the extension library locally without publishing to NPM:

```sh
npm run package:collabora-online-extension
```

The script produces the `dist/collabora-online-extension/collabora-online-extension-0.1.0.tgz` file
that can be used to install dependency.

Switch to the ACA project and run:

```sh
npm i <path>/aca-extension-collabora/dist/@jeci/collabora-online-extension/jeci-collabora-online-extension-0.1.0.tgz
```

Update the `extensions.module.ts` file in the folder `src/app/` and append the module:

```ts
import { CollaboraOnlineModule } from '@jeci/collabora-online-extension';

@NgModule({
  imports: [
    ...,
    CollaboraOnlineModule
  ]
})
export class AppExtensionsModule {}
```

Update the `app.extensions.json` file in the foler `src/assets/` and register new plugin:

```json
{
  "$schema": "../../extension.schema.json",
  "$name": "app",
  "$version": "1.0.0",
  "$references": [
    "collabora-online.plugin.json"
  ],
}
```

In `angular.json` file add these lines :

```
{
  ...
  "projects": {
    "app": {
      ...
      "architect": {
        "build": {
          ...
          "options": {
            ...
            "assets": [
              ...
              {
               "glob": "collabora-online.plugin.json",
               "input": "node_modules/@jeci/ccollabora-online-extension/assets",
               "output": "/assets/plugins"
              },
              {
               "glob": "**/*",
               "input": "node_modules/@jeci/collabora-online-extension/assets",
               "output": "/assets/collabora-online-extension"
              }
            ]
          }
        }
      }
    }
  }
}
```

Run the ACA application

```sh
npm start
```

Depending on the setup, you might need to log in as an administrator
and enable external plugins feature for your local run.
