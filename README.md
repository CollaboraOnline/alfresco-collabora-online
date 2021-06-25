
[![L10n](https://img.shields.io/badge/L10n-Weblate-lightgrey.svg)](https://hosted.weblate.org/projects/collabora-online/)

# Alfresco Collabora Online

This project contains 3 sub-projects :
* collabora-platform-extension : extension for Alfresco Content Services
* collabora-share-extension : extension for Share interface
* collabora-aca-extension : extension for Angular interface, Alfresco Content Application

If the user has the write permission, the extension adds an action `Edit with Collabora™ Online` on documents which can be opened with Collabora Online.

The document will be opened in an iFrame. Many users can open the same document at the same time.

## Compilation

You can clone the project and compile all projects :

```
git clone git@github.com:CollaboraOnline/alfresco-collabora-online.git
mvn install
```

To compile each project independently you can use the option `-pl`

```
mvn install -pl collabora-aca-extension,collabora-platform-extension,collabora-share-extension
```

Before version 6.x, compile with only `java8` profile :
```
mvn clean package -P '!java11',java8
```

## Installation

### ACS Extension

Add `collabora-platform-extension-<version>`.jar in the folder `INSTALL_DIR/webapps/alfresco/WEB-INF/lib`.
You must configure the following properties in `alfresco-global.properties` :

from version `3.0.1` onwards :
```
collabora.public.url=https://<collabora_server_domain>:<port>/
alfresco.public.url=https://<alfresco_server_domain>:<port>/alfresco/
```

Prio to version `0.3.1` :

```
lool.wopi.url=https://<collabora_server_domain>:<port>/
lool.wopi.alfresco.host=https://<alfresco_server_domain>:<port>/alfresco/s/
lool.wopi.url.discovery=https://<collabora_server_domain>:<port>/hosting/discovery
```

### Share Extension

Add `collabora-share-extension-<version>.jar` in the folder `INSTALL_DIR/webapps/share/WEB-INF/lib`.

### ACA Extension

Install the library `collabora-aca-extension-<version>-dist.tgz` in your project alfresco-content-application
```
cd ~/alfresco-content-application
npm install
npm run ng add ~/alfresco-collabora-online/collabora-aca-extension/target/collabora-aca-extnsion-<version>-dist.tgz
```

Modify the `app.extensions.json` file in the folder `src/assets` :
```
...
  "$references": [..., "collabora-online.plugin.json"],
...
```

Add the `CollaboraOnlineModule` in the `extensions.module.ts` file in the folder `src/app/`
```
...
import { CollaboraOnlineModule } from '@jeci/collabora-online-extension';
...
@NgModule({
  imports: [..., CollaboraOnlineModule]
})
...
```

#### Viewer Collabora-Online

Since **version 0.3.0**, it is possible to replace the standard viewer by collabora online in mode read-only for supported format.

For that you must add the module `ViewerCollaboraModule` in `viewer.module.ts` file in the folder `src/app/components/viewer`

```
...
import { ViewerCollaboraModule } from '@jeci/collabora-online-extension';
...
@NgModule({
  imports: [
  ...
      ViewerCollaboraModule
  ],
...
```

Add the component `viewer-collabora-online` in `viewer.component.html` file in in the folder `src/app/components/viewer`

```
<!-- Viewer collabora -->
<adf-viewer-extension [supportedExtensions]="supportedExtensions" #extension>
  <ng-template let-urlFileContent="urlFileContent">
    <viewer-collabora-online urlFileContent="urlFileContent" [nodeId]="nodeId"></viewer-collabora-online>
  </ng-template>
</adf-viewer-extension>
```
Define the extensions supported by Collabora Online in `viewer.component.ts` file in the folder `src/app/components/viewer`

```
...
import * as utilsCollabora from '@jeci/collabora-online-extension';
...
supportedExtensions: string[] = [];
...
ngOnInit() {
  ...
  this.supportedExtensions = utilsCollabora.getExtensions();
}
...
```

## Test

You can start the application for local test with docker-compose.

```
mvn resources:resources
pip install docker-compose
docker-compose -f ./target/classes/docker-compose.yml up --build -d
```

or using podman-compose

```
pip install podman-compose
podman-compose -f ./target/classes/docker-compose.yml build
```


Then you can access applications :

* [ACS](http://localhost:8080/alfresco) : http://localhost:8080/alfresco
* [Share](http://localhost:8080/share) : http://localhost:8080/share
* [ACA](http://localhost:8080/) : http://localhost:8080/

## Release

| Version | Commits                                                                                                                       |
|---------|:------------------------------------------------------------------------------------------------------------------------------|
| 0.1.0   | Beta version                                                                                                                  |
| 0.2.0   | Delete Close button in iFrame Collabora Online                                                                                |
|         | Add action fullscreen                                                                                                         |
| 0.2.1   | Add translations for Share and Alfresco Content Application interface (Hungarian, Turkish, Polish, Ukrainian, Spanish, Norwegian Bokmål, Dutch, Hebrew Japanese, Slovak, English New Zealand, Icelandic, Portuguese Brazil, Croatian)                            |
| 0.3.0   | Add a viewer with Collabora Online                                                                                            |
| 0.3.1   | Refactoring                                                                                                                   |
|         | Back implementation for save as action ( On front this action is disabled waiting the front implementation )                 |

## About Jeci

This project is maintained by [Jeci](https://jeci.fr) a french company that specializes in Free and Open Source technologies (FLOSS).

For any question or professional services, please send us an email info@jeci.fr
