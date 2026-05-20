# Changelog

## [1.5.2] - 2026-05-20

### Bug Fixes

- **restriction**: Run hasLicense authority check as system user

### Features

- **restriction**: Send email notification on license quota change

### Miscellaneous

- **version**: 1.5.2-SNAPSHOT
## [1.5.1] - 2026-05-19

### Miscellaneous

- **version**: 1.5.1-SNAPSHOT

### Refactoring

- **discovery**: Remove dead discoveryDoc field and add configurable timeout
## [1.5.0] - 2026-05-19

### Bug Fixes

- **remoteconfig**: Resolve Data Dictionary by QName instead of localized name
- **remoteconfig**: Normalize fontsBaseUrl to prevent double slashes
- **remoteconfig**: Use argument format style to preserve font file extensions
- **discovery**: Fix misleading double slash in WOPI discovery log message
- **remoteconfig**: Move isRemoteConfigEnabledInternal call inside runAsSystem

### Build

- **compose**: Update docker compose stack

### Documentation

- Update README with status endpoint, remote config toggle and font validation

### Features

- **restriction**: Add Collabora Online feature restriction with license management
- **remoteconfig**: Add Collabora Online remote/dynamic configuration
- **status**: Add status endpoint with reload and improved error messages
- **remoteconfig**: Add enable/disable remote config via status endpoint

### Miscellaneous

- **version**: 1.4.2-SNAPSHOT
- **doc**: Update readme
- **logging**: Set default log level to info and demote WOPI runtime logs to debug
## [1.4.1] - 2026-04-19

### Build

- **aca-extension**: Flatten layout and make standalone

### Miscellaneous

- **aca-extension**: Release version 0.1.0
- **format**: Format comment
## [1.4.0] - 2026-02-27

- Update build Alfresco 25.3.0


## [1.3.1] - 2026-02-06

### Bug Fixes

- **share**: Add clipboard and fullscreen permissions to iframe
- **webscript**: Move import directives before license comments

## [1.3.0] - 2025-11-28

### Removed
- Remove ACA Extension (no longer maintained, need community help)

### Changed
- Update to Alfresco SDK 4.11.0
- Update to Alfresco Platform 25.1.0 (Community Edition)
- Update to Alfresco Share 25.1.0
- Update Maven configuration
- Update copyright and license headers

### Added
- Add `IsAnonymousUser` property support

### Bug Fixes

- **iframe**: Add clipboard permissions to Collabora iframe
- **wopi**: Typo on IsAdminUser
- **share**: Encode WOPISrc with encodeURIComponent
- **service**: Prevent race condition when asking serverInfo
- **wopi**: Safe Property Parsing
- **wopi**: ValidateNodeExists after auth to get full context
- **cleanLock**: Disable cleanLock jobs
- Use dateTimeParser that is less strict.
- **share**: Un-hide exception

### Refactoring

- **wopi**: Use objectMapper.writeValueAsString instead of manual code

### Security

- Prevent XML External Entity (XXE) attack
- **wopi**: Generates a random access token using 256 bits of entropy
- **log**: Returns a masked version of the token for safe logging
- **wopi**: Validating NodeRef inputs
- **wopi**: Validate Response Headers to prevent HTTP response splitting attacks
- **log**: Escape Log Output to prevent log injection attacks

---

## [1.2.0] - 2025-03-02

### Changed
- **BREAKING**: Update to Java 17 (minimum required version)
- Update to Alfresco SDK 4.7
- Update to Alfresco ACS 7.4
- Replace slf4j logging implementation
- Replace ACA by Nginx in Docker configuration
- Update unit tests: add `--add-opens` for Java 17 compatibility

### Added
- Add `isAdminUser` property in WopiCheckFileWebScript
- Add mise.toml for task runner
- Add test coverage for PDF view_comment functionality
- Add support for lowercase file extensions (e.g., pdf matches PDF)

### Fixed
- Fix `writeFileToDisk` method that could create content twice
- Fix concurrency failure when trying to remove aspect
- Fix issue rm-711
- Fix module share language handling
- Update `ensureVersioningEnabled` method
- Correct `isAdminUser` property in WopiCheckFileWebscript
- Wrap `executeAsUser` to catch Throwable exceptions
- Return `STATUS_SERVICE_UNAVAILABLE` when Collabora is not available
- Prevent NPE if discoveryAction is not found
- Fix visibility of static classes

### Security
- **CRITICAL**: Prevent XML External Entity (XXE) attack vulnerability

### Removed
- Revert iframe allowfullscreen feature (caused issues)

---

## [1.1.0] - Unreleased

### Changed
- Code quality improvements
- Clean code and comments

### Fixed
- Fix typo in function name

---

## [1.0.2] - Unreleased

### Fixed
- Minor bug fixes and improvements

---

## [1.0.1] - 2024-01-29

### Fixed
- Fix the error to open Collabora Online in French

---

## [1.0.0] - 2023-11-06

### Changed
- Use lockService instead of the collabora aspect
- Create the rendition after changes

---

## [0.7.1] - 2023-04-19

### Added
- Add build amp

---

## [0.7.0] - 2022-10-24

### Added
- Add new header to manage aspect and properties
- Use WOPI protocol to pass the full username

### Changed
- Code quality improvements

### Fixed
- Fix NullPointerException

---

## [0.6.0] - 2022-04-20

### Changed
- Update to Alfresco SDK 4.4
- Update to Alfresco ACS 7.2
- Update to Alfresco Share 7.2

### Added
- Add run.sh script to help beginners

---

## [0.5.2] - 2022-04-11

### Fixed
- Looks like last CODE release (21.11) changed the date format used for timestamp

---

## [0.5.1] - 2022-01-06

### Added
- Open files directly in edit mode when the format allows it
- Configure the extensions that can be opened or edited with Collabora Online in `app.config.json`

---

## [0.5.0] - 2021-11-02

### Added
- Action SaveAs is enabled

### Changed
- Update of file formats accepted by Collabora 6.4.11.3
- Change the position of the icon to edit with Collabora Online

### Fixed
- Fix the bug to open big files

---

## [0.4.3] - 2021-09-09

### Changed
- Update of file formats accepted by Collabora 6.4.11 for view or edit mode

---

## [0.4.2] - 2021-07-27

### Changed
- Update of file formats accepted by Collabora 6.4.9 for view or edit mode

### Fixed
- Fix synchronisation banners which appears in Alfresco Share 6

---

## [0.4.1] - 2022-07-22

### Added
- Add a job to clean the locks that are no longer valid

---

## [0.4.0] - 2021-07-08

### Added
- Soft lock - Replace LoolMonitor
- Display in Share interface a banner when the file is already being edited by another user

---

## [0.3.1] - 2021-06-22

### Changed
- Refactoring

### Added
- Back implementation for save as action (On front this action is disabled waiting the front implementation)

---

## [0.3.0] - 2021-02-11

### Added
- Add a viewer with Collabora Online

---

## [0.2.1] - 2021-01-13

### Added
- Add translations for Share and Alfresco Content Application interface
  - Languages: Hungarian, Turkish, Polish, Ukrainian, Spanish, Norwegian Bokmål, Dutch, Hebrew, Japanese, Slovak, English (New Zealand), Icelandic, Portuguese (Brazil), Croatian

---

## [0.2.0] - 2020-10-02

### Added
- Add fullscreen action

### Changed
- Delete Close button in iFrame Collabora Online

---

## [0.1.0] - 2020-10-16

### Added
- Beta version - Initial release

---

## Versioning Notes

### Configuration Changes by Version

#### Version 0.3.1 onwards
Use these properties in `alfresco-global.properties`:
```properties
collabora.public.url=https://<collabora_server_domain>:<port>/
alfresco.public.url=https://<alfresco_server_domain>:<port>/alfresco/
```

#### Prior to version 0.3.1
Use these legacy properties:
```properties
lool.wopi.url=https://<collabora_server_domain>:<port>/
lool.wopi.alfresco.host=https://<alfresco_server_domain>:<port>/alfresco/s/
lool.wopi.url.discovery=https://<collabora_server_domain>:<port>/hosting/discovery
```

#### Version 0.4.1 onwards
Lock cleanup job configuration:
```properties
job.fr.jeci.collabora.cleanLock.cron=0 0/5 * * * ?
job.fr.jeci.collabora.cleanLock.cronstartdelay=240000
job.fr.jeci.collabora.cleanLock.enabled=true
```

---

**Project maintained by:** [Jeci](https://jeci.fr)
