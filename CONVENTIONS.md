# Development Conventions - alfresco-collabora-online

This document describes the code and development conventions for the `alfresco-collabora-online` project, an extension that integrates Collabora Online with Alfresco Content Services.

## Project Overview

This is a Maven multi-module project comprising:

- **collabora-platform-extension**: Alfresco Content Services extension (backend)
- **collabora-share-extension**: Share interface extension (legacy)
- **collabora-vue-component**: Vue.js component for Pristy
- **collabora-platform-extension-docker**: Docker configuration for ACS
- **collabora-share-extension-docker**: Docker configuration for Share
- **alfresco-collabora-online-docker**: Complete Docker Compose stack

## Technologies

### Backend
- **Java**: Version 17 (Temurin)
- **Alfresco SDK**: 4.11.0
- **Alfresco Platform**: 25.1.0 (Community Edition)
- **Alfresco Share**: 25.1.0
- **Maven**: 3.9.9
- **Spring Framework**: Via Alfresco
- **WOPI Protocol**: For Collabora integration

### Build and Tools
- **Maven**: Build manager
- **Spotless**: Automatic Java code formatting
- **mise**: Task runner for common commands
- **Docker Compose**: Local testing and development

## Java Conventions

### Code Formatting

The project uses **Spotless Maven Plugin** with an Eclipse formatter configuration defined in `formatter.xml`.

**Main rules**:
- **Indentation**: Mixed tabs (tabs + spaces), size 3
- **Line width**: 120 characters maximum
- **Braces**: `end_of_line` style (K&R)
  ```java
  public void method() {
      // code
  }
  ```
- **Spaces**:
  - After commas: `method(a, b, c)`
  - Around operators: `x = a + b`
  - Before opening braces: `if (condition) {`
- **Blank lines**:
  - 1 blank line after package
  - 1 blank line after imports
  - 1 blank line before methods
  - 0 blank lines at beginning/end of class bodies

**Formatting commands**:
```bash
mise run format:check   # Check formatting
mise run format:write   # Apply formatting
```

Formatting is automatically checked during Maven's `verify` phase.

### Code Structure

#### License Headers

**All Java files must start with the Apache 2.0 license header**:

```java
// SPDX-FileCopyrightText: 2025 Jeci SARL - https://jeci.fr
//
// SPDX-License-Identifier: Apache-2.0

package fr.jeci.collabora.alfresco;
```

#### Class Organization

1. **Constants**: At the top of the class, in uppercase with underscores
   ```java
   private static final String CANT_LOCK = "Can't lock node %s, ";
   private static final int ONE_HOUR_MS = 1000 * 60 * 60;
   ```

2. **Logger**: Right after constants
   ```java
   private static final Logger logger = LoggerFactory.getLogger(CollaboraOnlineServiceImpl.class);
   ```

3. **Instance fields**: After constants and logger

4. **Methods**: Ordered logically (public then private)

#### Naming Conventions

- **Classes**: PascalCase (`CollaboraOnlineServiceImpl`, `WopiDiscovery`)
- **Interfaces**: PascalCase without "I" prefix (`CollaboraOnlineService`)
- **Methods**: camelCase (`createAccessToken`, `checkAccessToken`)
- **Variables**: camelCase (`nodeRef`, `accessToken`, `tokenInfo`)
- **Constants**: SCREAMING_SNAKE_CASE (`DEFAULT_TOKEN_TTL_MS`, `CANT_LOCK`)
- **Packages**: lowercase with dots (`fr.jeci.collabora.alfresco`, `fr.jeci.collabora.wopi`)

#### Braces and Blocks

**Always use braces**, even for single-line blocks:

```java
// Correct
if (condition) {
    doSomething();
}

// Incorrect
if (condition)
    doSomething();
```

#### Exception Handling

- Use `WebScriptException` for REST API errors with appropriate HTTP status
- Use `AlfrescoRuntimeException` for configuration errors
- Create custom exceptions when needed (`ConflictException`)

```java
if (accessToken == null) {
    throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "AccessToken is null");
}
```

### JavaDoc and Comments

- **All comments in English**
- JavaDoc for public methods and interfaces
- Standard JavaDoc format:
  ```java
  /**
   * Generate and store an access token only valid for the current user/file id combination.
   * <p>
   * We check if we have at least READ permission, so a user must be connected.
   */
  @Override
  public WOPIAccessTokenInfo createAccessToken(NodeRef nodeRef) {
      // implementation
  }
  ```

## Alfresco Module Structure

### Platform Extension (AMP)

Standard resource structure:

```
src/main/resources/
├── alfresco/
│   ├── extension/templates/webscripts/     # REST webscripts
│   │   └── fr/jeci/collabora/
│   │       ├── *.get.desc.xml              # GET descriptor
│   │       ├── *.get.json.ftl              # JSON template
│   │       ├── *.post.desc.xml             # POST descriptor
│   │       └── *.lib.ftl                   # FreeMarker libraries
│   └── module/collabora-platform-extension/
│       ├── alfresco-global.properties      # Default configuration
│       ├── module.properties               # Module metadata
│       ├── model/
│       │   └── collabora-online-model.xml  # Content model
│       └── context/
│           ├── service-context.xml         # Spring service beans
│           ├── webscript-context.xml       # Webscript beans
│           ├── job-context.xml             # Scheduled jobs
│           └── bootstrap-context.xml       # Initialization
```

### Webscripts

Alfresco webscripts follow this naming convention:

- **URL**: `/alfresco/s/jeci/collabora/{endpoint}`
- **Files**:
  - `{endpoint}.{method}.desc.xml`: Descriptor (HTTP method, URL, authentication)
  - `{endpoint}.{method}.json.ftl`: Response template (FreeMarker)
  - `{endpoint}.{method}.js`: JavaScript controller (optional)
  - Java class: For complex webscripts

**Java webscript example**:

```java
public class GetTokenWebScript extends AbstractWopiWebScript {

    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status) {
        // Webscript logic
        Map<String, Object> model = new HashMap<>();
        model.put("token", tokenInfo);
        return model;
    }
}
```

### Spring Configuration

- **service-context.xml**: Service bean declarations
- **Dependency injection**: Via setters or constructors
- **Lifecycle**: `init()` method for initialization

```xml
<bean id="collaboraOnlineService"
      class="fr.jeci.collabora.alfresco.CollaboraOnlineServiceImpl"
      init-method="init">
    <property name="nodeService" ref="NodeService"/>
    <property name="permissionService" ref="PermissionService"/>
    <property name="collaboraPublicUrl" value="${collabora.public.url}"/>
</bean>
```

## Configuration

### alfresco-global.properties

The module defines configurable properties in `alfresco-global.properties`:

```properties
# Public URLs (accessible from browser)
collabora.public.url=http://localhost:9980/
alfresco.public.url=${alfresco.protocol}://${alfresco.host}:${alfresco.port}/${alfresco.context}

# Private URLs (internal network if different)
collabora.private.url=${collabora.public.url}
alfresco.private.url=${alfresco.public.url}

# Token configuration
lool.wopi.token.ttl=86400000  # 24h in ms

# Lock cleanup job
job.fr.jeci.collabora.cleanLock.cron=0 0/5 * * * ?
job.fr.jeci.collabora.cleanLock.enabled=true
```

**Property naming convention**:
- Prefix: `collabora.` or `lool.` (legacy)
- Hierarchy: dot-separated (`collabora.public.url`)
- Jobs: `job.fr.jeci.collabora.{jobName}.{property}`

## WOPI Protocol

The project implements the WOPI (Web Application Open Platform Interface) protocol for integration with Collabora Online.

### Main WOPI Endpoints

```
GET  /alfresco/s/wopi/files/{fileId}               # CheckFileInfo
GET  /alfresco/s/wopi/files/{fileId}/contents      # GetFile
POST /alfresco/s/wopi/files/{fileId}               # PutFile, Lock, Unlock, RefreshLock
POST /alfresco/s/wopi/files/{fileId}/contents      # PutRelativeFile
```

### WOPI Headers

Use the `WopiHeader` class for header constants:

```java
public class WopiHeader {
    public static final String OVERRIDE = "X-WOPI-Override";
    public static final String LOCK = "X-WOPI-Lock";
    public static final String OLD_LOCK = "X-WOPI-OldLock";
    // ...
}
```

### WOPI Operations

Operations are defined in `WopiOverride`:

```java
public enum WopiOverride {
    LOCK, UNLOCK, REFRESH_LOCK, GET_LOCK,
    PUT_RELATIVE, DELETE, RENAME_FILE
}
```

## Lock Management

The project uses Alfresco's `LockService` for collaborative lock management:

- **Lock type**: `LockType.WRITE_LOCK`
- **Duration**: 30 minutes (1800 seconds)
- **Lifetime**: `Lifetime.EPHEMERAL` (ephemeral locks)
- **Lock ID**: Identifier provided by Collabora Online

```java
this.lockService.lock(nodeRef, LockType.WRITE_LOCK, 30 * 60, Lifetime.EPHEMERAL, lockId);
```

**Cleanup job**:
- Cleans obsolete locks every 5 minutes by default
- Configurable via `job.fr.jeci.collabora.cleanLock.cron`

## Testing

### Unit Tests

- **Framework**: JUnit 4
- **Mocking**: Mockito 4.2.0
- **Location**: `src/test/java/`

```java
@Test
public void testCreateAccessToken() {
    // Given
    NodeRef nodeRef = new NodeRef("workspace://SpacesStore/test-id");

    // When
    WOPIAccessTokenInfo tokenInfo = service.createAccessToken(nodeRef);

    // Then
    assertNotNull(tokenInfo);
    assertEquals(nodeRef.getId(), tokenInfo.getFileId());
}
```

### Integration Tests

- **Plugin**: maven-failsafe-plugin
- **Execution**: `integration-test` phase
- **Location**: Classes ending with `IT.java`

### Test Commands

```bash
mvn test                    # Unit tests
mvn integration-test       # Integration tests
mvn verify                 # Tests + checks (formatting, etc.)
```

## Build and Deployment

### mise Commands (Recommended)

```bash
mise install               # Install tools (Java, Maven)
mise run build            # Complete build (mvn clean package)
mise run start            # Build + start Docker Compose
mise run stop             # Stop Docker Compose
mise run purge            # Stop and remove volumes
mise run format:check     # Check Java formatting
mise run format:write     # Apply Java formatting
```

### Maven Commands

```bash
mvn clean install                           # Standard build
mvn clean package -Dmaven.test.skip=true   # Build without tests (faster)
mvn spotless:check                         # Check formatting
mvn spotless:apply                         # Apply formatting
```

### Packaging

The project generates:
- **AMP files**: Deployable archives for Alfresco (`*.amp`)
- **JAR files**: Java libraries (for Share)
- **Docker images**: Development images

**Produced artifacts**:
- `collabora-platform-extension-{version}.amp`
- `collabora-share-extension-{version}.jar`

## Docker Compose

### Structure

```yaml
services:
  alfresco:           # Alfresco Content Services
  postgres:          # Database
  solr:              # Search
  code:              # Collabora Online
  proxy:             # Nginx reverse proxy
```

### Exposed Ports

- **8080**: Alfresco (via proxy)
- **9980**: Collabora Online

### Docker Commands

```bash
./run.sh build_start       # Build + start
./run.sh reload_acs       # Reload only ACS
./run.sh reload_share     # Reload only Share
docker compose logs -f    # View logs
```

## Git and Versioning

### Commits

**Convention**: Conventional Commits in **English**

```
type(scope): subject

[optional body]
```

**Types**:
- `feat`: New feature
- `fix`: Bug fix
- `refactor`: Refactoring without behavior change
- `docs`: Documentation
- `style`: Formatting, missing semicolons, etc.
- `test`: Adding or modifying tests
- `chore`: Maintenance, dependencies, etc.

**Examples**:
```
feat(wopi): add lock refresh mechanism
fix(webscript): handle null pointer in token validation
refactor(service): simplify access token generation
docs(readme): update installation instructions
chore(deps): upgrade Alfresco SDK to 4.11.0
```

### Branches

- **main**: Stable production branch
- **develop**: Development branch
- **feature/xxx**: New features
- **fix/xxx**: Bug fixes
- **release/x.x.x**: Release preparation

## Production Deployment

### Manual Installation

1. **ACS Extension**:
   ```bash
   # Copy JAR to webapps/alfresco/WEB-INF/lib/
   cp collabora-platform-extension-{version}.jar \
      $ALFRESCO_HOME/webapps/alfresco/WEB-INF/lib/
   ```

2. **Share Extension**:
   ```bash
   # Copy JAR to webapps/share/WEB-INF/lib/
   cp collabora-share-extension-{version}.jar \
      $ALFRESCO_HOME/webapps/share/WEB-INF/lib/
   ```

3. **Configuration**:
   - Edit `alfresco-global.properties`
   - Set `collabora.public.url` and `alfresco.public.url`
   - Restart Alfresco

### Required Configuration

**Mandatory**:
```properties
collabora.public.url=https://collabora.example.com/
alfresco.public.url=https://alfresco.example.com/alfresco/
```

**Recommended for production**:
```properties
# Private URLs if internal network
collabora.private.url=http://collabora-internal:9980/
alfresco.private.url=http://alfresco-internal:8080/alfresco/

# Active cleanup job
job.fr.jeci.collabora.cleanLock.enabled=true
job.fr.jeci.collabora.cleanLock.cron=0 0/5 * * * ?

# Token TTL (24h)
lool.wopi.token.ttl=86400000
```

## Internationalization (i18n)

### Share Extension

Translations are provided in:
```
src/main/resources/alfresco/web-extension/messages/
├── collabora-online-share.properties           # English (default)
├── collabora-online-share_fr.properties       # French
├── collabora-online-share_de.properties       # German
├── collabora-online-share_es.properties       # Spanish
└── ... (27 supported languages)
```

**Translation keys**:
```properties
node-header.edit-online=Edit Online with Collabora
node-header.view-online=View Online with Collabora
```

### Webscripts

Webscripts use `.properties` files for i18n:
```
node-header.get_fr.properties
node-header.get_en.properties
```

## Security

### WOPI Authentication

- **Tokens**: Randomly generated (BigInteger 130 bits, base 32)
- **TTL**: 24h by default, configurable
- **Storage**: Hazelcast distributed cache
- **Validation**: Token + NodeRef + User

### Permissions

- Check Alfresco permissions before token generation
- Minimum `READ` for viewing
- `WRITE` required for editing
- Respect collaborative locks

```java
AccessStatus perm = this.permissionService.hasPermission(nodeRef, PermissionService.READ);
if (AccessStatus.ALLOWED != perm) {
    throw new WebScriptException(Status.STATUS_UNAUTHORIZED, "Not allow to READ " + nodeRef);
}
```

## Contributing

### Pre-commit Checklist

- [ ] Code formatted with Spotless (`mise run format:write`)
- [ ] Unit tests pass (`mvn test`)
- [ ] Integration tests pass (`mvn verify`)
- [ ] No compilation warnings
- [ ] License headers present
- [ ] Documentation updated if necessary
- [ ] Commit message follows Conventional Commits

### Contribution Workflow

1. Create a branch from `develop`
2. Develop the feature/fix
3. Test locally with Docker Compose
4. Commit with conventional message
5. Push and create a Pull Request
6. Code review
7. Merge into `develop`

## Resources

### External Documentation

- [Alfresco Content Services](https://docs.alfresco.com/)
- [WOPI Protocol](https://docs.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/rest/)
- [Collabora Online](https://www.collaboraoffice.com/code/)
- [Maven](https://maven.apache.org/)
- [Spotless](https://github.com/diffplug/spotless)

### Contacts

- **Maintainer**: [Jeci](https://jeci.fr)
- **Email**: info@jeci.fr
- **GitHub**: [CollaboraOnline/alfresco-collabora-online](https://github.com/CollaboraOnline/alfresco-collabora-online)

---

**Document version**: 1.0
**Last updated**: 2025-10-20
