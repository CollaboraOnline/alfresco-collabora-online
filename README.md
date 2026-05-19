
[![L10n](https://img.shields.io/badge/L10n-Weblate-lightgrey.svg)](https://hosted.weblate.org/projects/collabora-online/)

# Alfresco Collabora Online

This project contains 4 sub-projects :
* collabora-platform-extension : extension for Alfresco Content Services
* collabora-share-extension : extension for lagacy Share interface
* collabora-aca-extension : extension for Angular interface, Alfresco Content Application
* collabora-vue-component : component for Pristy

If the user has the write permission, the extension adds an action `Edit with Collabora™ Online` on documents which can be opened with Collabora Online.

The document will be opened in an iFrame. Many users can open the same document at the same time.

## Compilation

You can clone the project and compile all projects :

```
git clone git@github.com:CollaboraOnline/alfresco-collabora-online.git
cd alfresco-collabora-online
./run.sh build
```
or with mise

```
mise install
mise run start 
```

## Installation

### Requirements

- **Java**: 17 or higher
- **Alfresco Content Services**: 7.2+ (tested with 25.3.0)
- **Alfresco Share**: 7.2+ (tested with 25.3.0)
- **Collabora Online**: 6.4+ (tested with 25.04)
- **Docker Compose**: Only for testing

### ACS Extension

1. Copy the extension JAR to Alfresco's library folder:
   ```bash
   cp collabora-platform-extension-<version>.jar $ALFRESCO_HOME/webapps/alfresco/WEB-INF/lib/
   ```

2. Configure `alfresco-global.properties` (see [Configuration Reference](#configuration-reference) for details):
   ```properties
   # Public URLs (accessible from browser)
   collabora.public.url=https://<collabora_server_domain>:<port>/
   alfresco.public.url=https://<alfresco_server_domain>:<port>/alfresco/

   # Private URLs (optional, for internal network)
   collabora.private.url=${collabora.public.url}
   alfresco.private.url=${alfresco.public.url}

   # Token time-to-live (24 hours in milliseconds)
   lool.wopi.token.ttl=86400000
   ```

3. Restart Alfresco

### Share Extension

1. Copy the extension JAR to Share's library folder:
   ```bash
   cp collabora-share-extension-<version>.jar $ALFRESCO_HOME/webapps/share/WEB-INF/lib/
   ```

2. Restart Share

## Test

You can start the application for local test with docker-compose.

```
./run.sh build_start
```

To compile and reload a project independently you can use :

```
# Build and reload only Alfresco Content Services
./run.sh reload_acs

# Build and reload only Alfresco Share
./run.sh reload_share
```


Then you can access applications :

* [ACS](http://localhost:8080/alfresco) : http://localhost:8080/alfresco
* [Share](http://localhost:8080/share) : http://localhost:8080/share

## Network Architecture

Understanding the network flows is important for a secure deployment.

### Network Communication Flows

```
┌─────────────┐                    ┌──────────────────┐                    ┌─────────────┐
│   Browser   │ ◄──────HTTPS─────► │  Reverse Proxy   │ ◄──────HTTP──────► │  Collabora  │
│   (User)    │                    │                  │                    │   Online    │
└─────────────┘                    └──────────────────┘                    └─────────────┘
                                            │
                                            │ HTTP (WOPI)
                                            │
                                            ▼
┌─────────────────────────────────────────────────────┐
│              Alfresco Repository                    │
│  - Serves WOPI endpoints                            │
│  - Fetches discovery XML at startup ONLY            │
└─────────────────────────────────────────────────────┘
```

### Key Points

1. **Browser ↔ Collabora Online**: Users' browsers load the Collabora Online editor interface via `collabora.public.url`
2. **Browser ↔ Alfresco**: WOPI protocol communication (CheckFileInfo, GetFile, PutFile) via `alfresco.public.url`
3. **Alfresco → Collabora**: **Only at startup** - Alfresco fetches `/hosting/discovery` to know which file types are supported

### Security Best Practice: Avoid Outbound Traffic

For maximum security, you can avoid opening network traffic from Alfresco to Collabora Online by hosting the discovery XML internally.

**Steps:**

1. Download the discovery XML once:
   ```bash
   curl https://collabora.example.com/hosting/discovery > discovery.xml
   ```

2. Host it on your reverse proxy or internal web server:
   ```nginx
   # Nginx example
   location /collabora-discovery/hosting/discovery {
       alias /var/www/discovery.xml;
       default_type application/xml;
   }
   ```

3. Configure Alfresco to use the internal URL:
   ```properties
   collabora.public.url=https://collabora.example.com/
   collabora.private.url=https://proxy-internal/collabora-discovery/
   ```

**Result:** No direct network communication between Alfresco and Collabora Online. The discovery XML is loaded from your internal infrastructure.

**Note:** Remember to update the discovery XML when upgrading Collabora Online, as supported file formats may change.

## Configuration Reference

All configuration properties should be defined in `$ALFRESCO_HOME/tomcat/shared/classes/alfresco-global.properties`.

### Connection URLs

#### `collabora.public.url` (Required)
**Type:** URL
**Default:** `http://localhost:9980/`

Public URL of the Collabora Online server, accessible from the user's browser. This URL is used to load the Collabora Online editor in the user's web browser.

**Example:**
```properties
collabora.public.url=https://collabora.example.com/
```

#### `collabora.private.url` (Optional)
**Type:** URL
**Default:** `${collabora.public.url}`

Internal URL of the Collabora Online server. Use this if Collabora Online is accessible via a different URL from within the Alfresco server's network (e.g., internal hostname or IP address).

**Important:** This URL is used by Alfresco at startup to fetch the WOPI discovery XML from `<collabora.private.url>/hosting/discovery`. This is the **only** network communication needed between Alfresco and Collabora Online.

**Example:**
```properties
collabora.private.url=http://collabora-internal:9980/
```

**Special case - Avoiding outbound network traffic:**

If you want to avoid opening network traffic from Alfresco to Collabora Online (recommended for security), you can:

1. Copy the content of `https://collabora.example.com/hosting/discovery` to your reverse proxy or internal web server
2. Configure `collabora.private.url` to point to this internal copy

**Example configuration:**
```properties
# Public URL for browser access
collabora.public.url=https://collabora.example.com/

# Private URL pointing to local copy of discovery XML
collabora.private.url=https://proxy-internal/collabora-discovery/

# The discovery XML should be available at:
# https://proxy-internal/collabora-discovery/hosting/discovery
```

This way, Alfresco loads the discovery configuration from an internal source, while browsers still access Collabora Online via the public URL. No direct network communication is needed between Alfresco and Collabora Online.

#### `alfresco.public.url` (Required)
**Type:** URL
**Default:** `${alfresco.protocol}://${alfresco.host}:${alfresco.port}/${alfresco.context}`

Public URL of the Alfresco server, accessible from the user's browser. This URL is used by the browser to communicate with Alfresco.

**Example:**
```properties
alfresco.public.url=https://alfresco.example.com/alfresco/
```

#### `alfresco.private.url` (Optional)
**Type:** URL
**Default:** `${alfresco.public.url}`

Internal URL of the Alfresco server used by Collabora Online to fetch documents. Use this if Alfresco is accessible via a different URL from within Collabora's network.

**Example:**
```properties
alfresco.private.url=http://alfresco-internal:8080/alfresco/
```

### Token Configuration

#### `lool.wopi.token.ttl` (Optional)
**Type:** Integer (milliseconds)
**Default:** `86400000` (24 hours)

Time-to-live for WOPI access tokens in milliseconds. After this period, tokens expire and users must reload the document.

**Examples:**
```properties
# 12 hours
lool.wopi.token.ttl=43200000

# 24 hours (default)
lool.wopi.token.ttl=86400000

# 48 hours
lool.wopi.token.ttl=172800000
```

**Note:** Tokens shorter than 1 hour (3600000 ms) will generate a warning in the logs.

### Renditions

#### `fr.jeci.collabora.renditions` (Optional)
**Type:** Comma-separated list
**Default:** `imgpreview,medium,doclib,pdf`

List of renditions to automatically regenerate after a document is saved with Collabora Online. This ensures thumbnails and previews are updated with the latest content.

**Available renditions:**
- `imgpreview` - Image preview
- `medium` - Medium size thumbnail
- `doclib` - Document library thumbnail
- `pdf` - PDF rendition

**Example:**
```properties
# Generate only essential renditions
fr.jeci.collabora.renditions=doclib,pdf

# Disable automatic rendition generation
fr.jeci.collabora.renditions=
```

### Remote/Dynamic Configuration

This feature implements [Collabora Online Remote Configuration](https://sdk.collaboraonline.com/docs/installation/Configuration.html#remote-dynamic-configuration) to allow Alfresco to serve configuration and custom fonts to Collabora Online. Collabora polls the remote config endpoint every 60 seconds using ETag-based caching.

Configuration and fonts are stored in the Alfresco repository under `/Data Dictionary/collabora-online/`.

#### `fr.jeci.collabora.remoteConfig.basePath` (Optional)
**Type:** String
**Default:** `collabora-online`

Folder name under `/Data Dictionary/` for storing the Collabora configuration and fonts. Created automatically on first startup.

#### `fr.jeci.collabora.remoteConfig.fontsUrl` (Optional)
**Type:** URL
**Default:** `${alfresco.public.url}/s/collabora/fonts`

Public base URL for serving font files. This URL is used in the font configuration JSON to build font URIs.

#### Public Endpoints (no authentication)

These endpoints are called by Collabora Online directly and require no authentication. Secure them at the network/reverse proxy level.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/collabora/remote-config` | GET | Main configuration JSON (polled by coolwsd every 60s) |
| `/collabora/fonts-config` | GET | Font configuration JSON (generated from fonts folder) |
| `/collabora/fonts/{font_name}` | GET | Serves individual font files |

#### Admin Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/collabora/admin/remote-config` | GET | Read current configuration |
| `/collabora/admin/remote-config` | POST | Update configuration (JSON body with `"kind": "configuration"`) |
| `/collabora/admin/fonts` | GET | List available fonts |
| `/collabora/admin/fonts?name={name}` | POST | Upload a font file (binary body) |
| `/collabora/admin/fonts/{font_name}` | DELETE | Delete a font |

#### Collabora Online Server Configuration

Configure `coolwsd.xml` to point to the Alfresco remote config endpoint:

```xml
<remote_config>
    <remote_url type="string" default="">https://alfresco.example.com/alfresco/s/collabora/remote-config</remote_url>
</remote_config>
```

#### Font Preloading with pristy-init

Use the `pristy-init` example configuration to preload fonts:

```bash
pristy-init --server http://localhost:8080 --app collabora-online --init-file examples/collabora-online/init.yaml
```

See `pristy-init/examples/collabora-online/` for the full configuration.

### Feature Restriction (License Management)

This feature implements [Collabora Online Feature Restriction](https://sdk.collaboraonline.com/docs/installation/Configuration.html#feature-restriction) to manage user licenses. When enabled, only users belonging to a configurable Alfresco group can fully edit documents. Other users receive `IsUserLocked=true` in the WOPI CheckFileInfo response, which — combined with `feature_lock.is_lock_readonly=true` in Collabora's `coolwsd.xml` — forces them into read-only mode.

#### `fr.jeci.collabora.featureRestriction.enabled` (Optional)
**Type:** Boolean
**Default:** `false`

Enable or disable group-based license restriction.

#### `fr.jeci.collabora.featureRestriction.group` (Optional)
**Type:** String (Alfresco group full name)
**Default:** `GROUP_COLLABORA_ONLINE`

The Alfresco group whose members are considered licensed. The group is automatically created on first startup if it does not exist. Users in this group (including via nested groups) get full editing access.

#### `fr.jeci.collabora.featureRestriction.maxLicenses` (Optional)
**Type:** Integer
**Default:** `-1` (unlimited)

Maximum number of users that can be added to the licensed group. Set to `-1` for unlimited. This value is persisted via Alfresco's `AttributeService` and can be updated at runtime via the admin webscript without restarting Alfresco.

#### Collabora Online Server Configuration

For the feature restriction to enforce read-only mode, you must configure Collabora Online's `coolwsd.xml`:

```xml
<feature_lock>
    <is_lock_readonly type="bool" default="true">true</is_lock_readonly>
</feature_lock>
```

#### Admin Webscripts

All license management webscripts require **admin authentication**.

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/collabora/license` | GET | List license status: enabled flag, count, max, and licensed user IDs |
| `/collabora/license/grant?user={userId}` | POST | Grant a license to a user (add to group) |
| `/collabora/license/revoke?user={userId}` | POST | Revoke a license from a user (remove from group) |
| `/collabora/license/config?maxLicenses={n}` | POST | Update the maximum number of licenses at runtime |

**Examples:**
```bash
# List licensed users
curl -u admin:admin "http://localhost:8080/alfresco/s/collabora/license"

# Grant a license
curl -u admin:admin -X POST "http://localhost:8080/alfresco/s/collabora/license/grant?user=jdoe"

# Revoke a license
curl -u admin:admin -X POST "http://localhost:8080/alfresco/s/collabora/license/revoke?user=jdoe"

# Set max licenses to 50
curl -u admin:admin -X POST "http://localhost:8080/alfresco/s/collabora/license/config?maxLicenses=50"
```

Each grant or revoke operation is logged at `INFO` level with the total license count.

### Lock Cleanup Job (Deprecated)

The lock cleanup job is **deprecated** since version 1.0.0. Alfresco's native lock management is now used instead.

#### `job.fr.jeci.collabora.cleanLock.enabled` (Deprecated)
**Type:** Boolean
**Default:** `false`

Enable or disable the lock cleanup job. **Should remain disabled** in version 1.0+.

#### `job.fr.jeci.collabora.cleanLock.cron` (Deprecated)
**Type:** Cron expression
**Default:** `0 0/5 * * * ?` (every 5 minutes)

Cron schedule for the lock cleanup job. Only used if enabled.

#### `job.fr.jeci.collabora.cleanLock.cronstartdelay` (Deprecated)
**Type:** Integer (milliseconds)
**Default:** `240000` (4 minutes)

Delay before the first execution of the cleanup job after Alfresco startup.

### Example Complete Configuration

```properties
# ============================================
# Alfresco Collabora Online Configuration
# ============================================

# Connection URLs
collabora.public.url=https://collabora.example.com/
collabora.private.url=http://collabora-internal:9980/
alfresco.public.url=https://alfresco.example.com/alfresco/
alfresco.private.url=http://alfresco-internal:8080/alfresco/

# Token configuration
lool.wopi.token.ttl=86400000

# Renditions (optional)
fr.jeci.collabora.renditions=imgpreview,medium,doclib,pdf

# Remote/Dynamic Configuration (optional)
fr.jeci.collabora.remoteConfig.basePath=collabora-online
fr.jeci.collabora.remoteConfig.fontsUrl=https://alfresco.example.com/alfresco/s/collabora/fonts

# Feature Restriction (optional - disabled by default)
fr.jeci.collabora.featureRestriction.enabled=false
fr.jeci.collabora.featureRestriction.group=GROUP_COLLABORA_ONLINE
fr.jeci.collabora.featureRestriction.maxLicenses=-1

# Lock cleanup job (deprecated - keep disabled)
job.fr.jeci.collabora.cleanLock.enabled=false
job.fr.jeci.collabora.cleanLock.cron=0 0/5 * * * ?
job.fr.jeci.collabora.cleanLock.cronstartdelay=240000
```

## Release Notes

For detailed release history and changelog, see [CHANGELOG.md](CHANGELOG.md).

**Current version:** 1.4.0

## Migration Guide

### Migrating from version < 0.3.1 to version 1.0+

If you are upgrading from a version prior to 0.3.1, you need to update your configuration properties.

#### Step 1: Update Configuration Properties

**Remove old properties** from `alfresco-global.properties`:
```properties
# OLD - Remove these:
lool.wopi.url=...
lool.wopi.alfresco.host=...
lool.wopi.url.discovery=...
```

**Add new properties**:
```properties
# NEW - Add these:
collabora.public.url=https://<collabora_server_domain>:<port>/
alfresco.public.url=https://<alfresco_server_domain>:<port>/alfresco/

# Optional - for internal network
collabora.private.url=${collabora.public.url}
alfresco.private.url=${alfresco.public.url}
```

#### Step 2: Update Lock Management (version 1.0+)

Version 1.0.0 introduced a major change: the extension now uses Alfresco's native `LockService` instead of custom aspects.

**Before migration:**
- Locks were managed using the `collabora:collaboraOnline` aspect
- Custom lock properties were stored on documents
- Automatic cleanup job removes obsolete locks

**After migration:**
- Locks use Alfresco's standard lock mechanism

**Required actions:**

1. **Lock cleanup job** - The lock cleanup job is now **deprecated** and disabled by default in version 1.0+. Alfresco's native lock management is used instead. If you have the job enabled from a previous configuration, you should disable it in `alfresco-global.properties`:
   ```properties
   job.fr.jeci.collabora.cleanLock.enabled=false
   ```

2. **Clean up old aspects** (optional, recommended for large repositories):

   After upgrading, you may want to remove old `collabora:collaboraOnline` aspects from documents. This can be done via:

   - JavaScript console in Alfresco
   - Custom migration script
   - Manual cleanup using the Node Browser

   Example JavaScript to remove old aspects:
   ```javascript
   var nodes = search.luceneSearch("ASPECT:\"collabora:collaboraOnline\"");
   for (var i = 0; i < nodes.length; i++) {
       if (nodes[i].hasAspect("collabora:collaboraOnline")) {
           nodes[i].removeAspect("collabora:collaboraOnline");
           nodes[i].save();
       }
   }
   logger.log("Cleaned up " + nodes.length + " nodes");
   ```

3. **Verify lock behavior**:
   - Test document editing with multiple users
   - Verify lock indicators appear correctly in Share
   - Check that locks are automatically released

#### Step 3: Test Rendition Generation (version 1.0+)

Version 1.0.0 also introduced automatic rendition generation after document changes.

**Configure renditions** in `alfresco-global.properties` (optional):
```properties
# Comma-separated list of rendition names to generate
fr.jeci.collabora.renditions=imgpreview,medium,doclib,pdf
```

**Verify**:
- Edit a document with Collabora Online
- Save changes
- Check that thumbnails are regenerated automatically

## About Jeci

This project is maintained by [Jeci](https://jeci.fr) a french company that specializes in Free and Open Source technologies (FLOSS).

For any question or professional services, please send us an email info@jeci.fr
