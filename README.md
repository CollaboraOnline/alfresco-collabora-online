# Alfresco Collabora Online

This project contains 3 sub-projects :
* collabora-platform-extension : extension for Alfresco Content Services
* collabora-share-extension : extension for Share interface
* collabora-aca-extension : extension for Angular interface, Alfresco Content Application

The extension adds an action `Edit with Collabora™ Online` on documents which can be opened with Collabora Online and if the user has the write permission. The document will be opened in an iFrame.

Many users can open the same document at the same time.

The version `0.1-SNAPSHOT` is still in beta. It should not be used in production.

## Compilation

You can clone the project and compile all projects :

```
git clone git@github.com:jecicorp/alfresco-collabora-online.git
mvn install
```

To compile each project independently you can use the option `-pl`

```
mvn install -pl collabora-aca-extension,collabora-platform-extension,collabora-share-extension
```

## Installation

### ACS Extension

Add `collabora-platform-extension-<version>`.jar in the folder `INSTALL_DIR/webapps/alfresco/WEB-INF/lib`.
You must configure the following properties in `alfresco-global.properties` :

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

## Test

You can start the application for local test with docker-compose.

```
mvn install
docker-compose -f target/classes/docker-compose/docker-compose.yml up -d
```

You can access the applications :
* [ACS](http://localhost:8080/alfresco) : http://localhost:8080/alfresco
* [Share](http://localhost:8080/share) : http://localhost:8080/share
* [ACA](http://localhost:8080/) : http://localhost:8080/
