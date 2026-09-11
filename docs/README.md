# Documentation

Technical documentation for Alfresco Collabora Online.

| Document | Purpose |
|---|---|
| [generate-contributors.py](generate-contributors.py) | Regenerates the root [`CONTRIBUTORS.md`](../CONTRIBUTORS.md) from the git history. Run it whenever an external contribution is merged; the *Updating this file* section of `CONTRIBUTORS.md` describes the two tables that are maintained by hand. |

Contributor identities are canonicalised through the [`.mailmap`](../.mailmap)
at the repository root, which merges the spelling variants of a same author.
Add an entry there rather than editing `CONTRIBUTORS.md` by hand when a name
appears under several forms in `git shortlog -sne --all`.
