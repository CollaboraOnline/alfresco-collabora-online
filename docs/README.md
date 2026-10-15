# Documentation

Technical documentation for Alfresco Collabora Online.

| Document | Purpose |
|---|---|
| [DependencyScanning.md](DependencyScanning.md) | CVE scanning of dependencies: the two SBOM perimeters (our artefacts versus the ACS and Share platform), the blocking CI gate, and how to remediate a finding. |
| [WopiTokens.md](WopiTokens.md) | Storage, scope, lifetime and renewal of the WOPI access tokens, for Collabora and for processing tasks. |
| [generate-contributors.py](generate-contributors.py) | Regenerates the root [`CONTRIBUTORS.md`](../CONTRIBUTORS.md) from the git history. Run it whenever an external contribution is merged; the *Updating this file* section of `CONTRIBUTORS.md` describes the two tables that are maintained by hand. |

Contributor identities are canonicalised through the [`.mailmap`](../.mailmap)
at the repository root, which merges the spelling variants of a same author.
Add an entry there rather than editing `CONTRIBUTORS.md` by hand when a name
appears under several forms in `git shortlog -sne --all`.
