# Changelog

All notable changes to the alfresco-collabora-online project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.3.0-SNAPSHOT] - Unreleased

### Changed
- Update to Alfresco SDK 4.11.0
- Update to Alfresco Platform 25.1.0 (Community Edition)
- Update to Alfresco Share 25.1.0
- Update Maven configuration
- Update copyright and license headers

### Added
- Add `IsAnonymousUser` property support
- Add mise task runner configuration (mise.toml)

### Removed
- Remove ACA Extension (no longer maintained, need community help)

### Fixed
- Format code according to project conventions

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

## [1.0.2] - 2022-XX-XX

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
