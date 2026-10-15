# WOPI access tokens

Every WOPI call carries an `access_token` query parameter. Alfresco resolves it to a user, the files it may read and
the file it may write.

## Storage

Tokens are stored in the `AttributeService` (`alf_prop_*` tables) by `WopiTokenStore`, under the keys
`collabora` / `wopi-token` / SHA-256 of the token, with their kind and their dates as epoch milliseconds (UTC). The
database never holds a usable token value. Reads go through the `propertyUniqueContextCache` and `propertyValueCache`
caches, and tokens survive a repository restart. A stored token without a known kind is ignored.

The job `job.fr.jeci.collabora.purgeWopiTokens` removes expired tokens every hour
(`job.fr.jeci.collabora.purgeWopiTokens.cron`). Collabora tokens are kept `lool.wopi.token.renewalRetention` (24 h)
after their expiry, so that they can still be renewed; other tokens are removed as soon as they expire.

Removing a token leaves its values (hash, dates) in `alf_prop_string_value` and `alf_prop_value` until Alfresco's
`propTablesCleaner` job runs (`attributes.propcleaner.cronExpression`, weekly by default). The growth is small.

## Kinds of tokens

| Kind        | Created by                  | Reads             | Writes              | Lifetime              | Renewed |
| ----------- | --------------------------- | ----------------- | ------------------- | --------------------- | ------- |
| `COLLABORA` | `createAccessToken`         | its file          | its file            | `lool.wopi.token.ttl` | yes     |
| `SETTINGS`  | `createSettingsAccessToken` | settings          | settings            | `lool.wopi.token.ttl` | no      |
| `TASK`      | `createScopedAccessToken`   | input and output  | output, or nothing  | given by the caller   | no      |

The user's own permissions still apply on top of the token scope. A WOPI `POST` (PutFile, PutRelativeFile, Lock,
Unlock) on a file the token cannot write answers `401`, and CheckFileInfo reports `UserCanWrite=false` for it.
The WOPI settings endpoints accept `COLLABORA` and `SETTINGS` tokens only, and PutRelativeFile `COLLABORA` tokens
only.

An expired Collabora token is extended in place, keeping its value, as long as its user can still read the file and
the purge job has not removed it yet. It is never extended beyond `lool.wopi.token.maxLifetime` (7 days) after its
creation. An expired task token is rejected: a processing task never outlives the timeout
it was given. `createScopedAccessToken` throws `IllegalArgumentException` for a missing node or a non-positive
duration, and `AccessDeniedException` for the System user or a missing permission.

Task tokens are created by Pristy Kafka when a worker claims a processing task (see the processing tasks design in
the `pristy-kafka` repository).
