
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
cd alfresco-collabora-online
./run.sh build_start
```

To compile each project independently you shortcut

```
# Build and reload only Alfresco Content Services
./run.sh reload_acs

# Build and reload only Alfresco Share
./run.sh reload_share
```


## Installation

### ACS Extension

Add `collabora-platform-extension-<version>`.jar in the folder `INSTALL_DIR/webapps/alfresco/WEB-INF/lib`.
You must configure the following properties in `alfresco-global.properties` :

From version `0.3.1` onwards :

```
collabora.public.url=https://<collabora_server_domain>:<port>/
alfresco.public.url=https://<alfresco_server_domain>:<port>/alfresco/
```

Prior to version `0.3.1` :

```
lool.wopi.url=https://<collabora_server_domain>:<port>/
lool.wopi.alfresco.host=https://<alfresco_server_domain>:<port>/alfresco/s/
lool.wopi.url.discovery=https://<collabora_server_domain>:<port>/hosting/discovery
```

#### Job to clean locks

From version `0.4.1` onwards, there are a job that clean obsolete locks. To configure the job you can define in `alfresco-global.properties`
the following properties :

```
job.fr.jeci.collabora.cleanLock.cron=0 0/5 * * * ?
job.fr.jeci.collabora.cleanLock.cronstartdelay=240000
job.fr.jeci.collabora.cleanLock.enabled=true
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

#### Add ViewerCollaboraModule

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

#### Add Collabora Viewer

Add the component `viewer-collabora-online` in `viewer.component.html` file in in the folder `src/app/components/viewer`

```
<!-- Viewer collabora -->
<adf-viewer-extension [supportedExtensions]="supportedExtensions" #extension>
  <ng-template let-urlFileContent="urlFileContent">
    <viewer-collabora-online urlFileContent="urlFileContent" [nodeId]="nodeId"></viewer-collabora-online>
  </ng-template>
</adf-viewer-extension>
```

#### Define extensions list (prior to version 0.5.1)

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

#### Define extensions list (from version 0.5.1)

Define the extensions supported by Collabora Online in `app.config.json` file.

```
...
"collabora": {
    "enable": true,
    "edit": [ ... ],
    "view": [ ... ]
},
...
```

Add the extensions list in `viewer.component.ts` file in the folder `src/app/components/viewer`

```
...
import { CollaboraOnlineService } from '@jeci/collabora-online-extension';
...
supportedExtensions: string[] = [];
...
constructor(
...
  private collaboraOnlineService : CollaboraOnlineService,
...  
)
...
ngOnInit() {
  ...
  this.supportedExtensions = this.collaboraOnlineService.getExtensions();
}
...
```

## Test

You can start the application for local test with docker-compose.

```
mvn clean package
mvn resources:resources
pip install docker-compose
docker-compose -f ./target/classes/docker-compose.yml up --build -d
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
|         | Back implementation for save as action ( On front this action is disabled waiting the front implementation )                  |
| 0.4.0   | Soft lock - Replace LoolMonitor. Display in Share interface a banner when the file is already editing by another user.        |
| 0.4.1   | Add a job to clean the locks that are no longer valid                                                                         |
| 0.4.2   | Update of file formats accepted by collabora 6.4.9 for view or edit mode                                                      |
|         | Fix synchronisation banners which appears in Alfresco Share 6                                                                 |
| 0.4.3   | Update of file formats accepted by collabora 6.4.11 for view or edit mode                                                     |
| 0.5.0   | Fix the bug to open big files                                                                                                 |
|         | Action SaveAs is enabled                                                                                                      |
|         | Update of file formats accepted by collabora 6.4.11.3                                                                         |
|         | Change the position the icon to edit with collabora online                                                                    |
| 0.5.1   | Open files directly in edit mode when the format allows it                                                                    |
|         | Configure the extensions that can be opened or edited with Collabora Online in `app.config.json`.                             |
| 0.5.2   | Looks like last CODE release (21.11) change the date format used for timestamp                                                |
| 0.6.0   | Update to Alfresco SDK 4.4, Alfresco ACS 7.2 and Alfresco Share 7.2                                                           |
|         | Add run.sh script to help beginners.                                                                                          |

## About Jeci

This project is maintained by [Jeci](https://jeci.fr) a french company that specializes in Free and Open Source technologies (FLOSS).

For any question or professional services, please send us an email info@jeci.fr
