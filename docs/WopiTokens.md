# WOPI access tokens

Every WOPI call carries an `access_token` query parameter. Alfresco resolves it to a user, the files it may read and
the file it may write.

## Storage

Tokens are stored in the `AttributeService` (`alf_prop_*` tables) by `WopiTokenStore`, under the keys
`collabora` / `wopi-token` / SHA-256 of the token. The database never holds a usable token value. Reads go through
the `propertyUniqueContextCache` and `propertyValueCache` caches, and tokens survive a repository restart.

The job `job.fr.jeci.collabora.purgeWopiTokens` removes expired tokens every hour
(`job.fr.jeci.collabora.purgeWopiTokens.cron`).

## Kinds of tokens

| Kind      | Created by                  | Reads             | Writes              | Lifetime              | Renewed |
| --------- | --------------------------- | ----------------- | ------------------- | --------------------- | ------- |
| Collabora | `createAccessToken`         | its file          | its file            | `lool.wopi.token.ttl` | yes     |
| Settings  | `createSettingsAccessToken` | settings          | settings            | `lool.wopi.token.ttl` | no      |
| Task      | `createScopedAccessToken`   | input and output  | output, or nothing  | given by the caller   | no      |

The user's own permissions still apply on top of the token scope. A WOPI `POST` (PutFile, PutRelativeFile, Lock,
Unlock) on a file the token cannot write answers `401`, and CheckFileInfo reports `UserCanWrite=false` for it.
The WOPI settings endpoints refuse task tokens: they accept Collabora and settings tokens only.

An expired Collabora token is extended in place, keeping its value, as long as its user can still read the file and
the purge job has not removed it yet. An expired task token is rejected: a processing task never outlives the timeout
it was given.

Task tokens are created by Pristy Kafka when a worker claims a processing task (see the processing tasks design in
the `pristy-kafka` repository).
