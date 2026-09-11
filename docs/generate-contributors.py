#!/usr/bin/env python3
"""Regenerate CONTRIBUTORS.md from the git history."""

import collections
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT = os.path.join(REPO, "CONTRIBUTORS.md")
WEBLATE = "https://hosted.weblate.org/projects/collabora-online/"

EXCLUDED = {"vincent.francois.ext@orange.com"}
BOT = {"noreply@weblate.org"}

CODE = {
    "jeremie.lesage@jeci.fr": ("Jeci", None),
    "cindy@jeci.fr": ("Jeci", None),
    "vincent@jeci.fr": ("Jeci", "collabora-vue-component"),
    "estelle.augier@jeci.fr": ("Jeci", "ACA viewer, Docker packaging"),
    "jean-philippe.boubou@jeci.fr": ("Jeci", "WOPI CheckFileInfo, Share editor"),
    "facundovelazco133@gmail.com": (
        "External", "Angular 19 rewrite of the ACA extension"),
    "peter@loftux.se": (
        "External",
        "Share full-page editor view, save dialog, Swedish translation"),
}

NOTE = {
    "andras.timar@collabora.com":
        "also English wording fixes and localization coordination",
}

LANG = {
    "ady": "Adyghe", "ar": "Arabic", "be_Latn": "Belarusian (Latin)",
    "ca": "Catalan", "cs": "Czech", "cy": "Welsh", "da": "Danish",
    "de": "German", "el": "Greek", "en_AU": "English (Australia)",
    "en_GB": "English (United Kingdom)", "en_NZ": "English (New Zealand)",
    "en_ZA": "English (South Africa)", "eo": "Esperanto", "es": "Spanish",
    "eu": "Basque", "fa": "Persian", "fi": "Finnish", "fr": "French",
    "fy": "Frisian", "ga": "Irish", "he": "Hebrew", "hr": "Croatian",
    "hu": "Hungarian", "ia": "Interlingua", "id": "Indonesian",
    "is": "Icelandic", "it": "Italian", "ja": "Japanese", "ko": "Korean",
    "ml": "Malayalam", "my": "Burmese", "nb_NO": "Norwegian Bokmål",
    "ne": "Nepali", "nl": "Dutch", "oc": "Occitan", "or": "Odia",
    "pl": "Polish", "pt": "Portuguese", "pt_BR": "Portuguese (Brazil)",
    "ru": "Russian", "si": "Sinhala", "sk": "Slovak", "sl": "Slovenian",
    "sq": "Albanian", "sv": "Swedish", "sw": "Swahili", "tr": "Turkish",
    "uk": "Ukrainian", "vi": "Vietnamese", "zh_CN": "Chinese (Simplified)",
    "zh_Hans": "Chinese (Simplified)", "zh_Hant": "Chinese (Traditional)",
    "zh_TW": "Chinese (Traditional)",
}

LANG_DIR = re.compile(
    r"(assets/i18n|web-extension/messages|libreofficepage/i18n|node-header)/")
LANG_FILE = (
    re.compile(r"^([A-Za-z]{2,3}(?:[_-][A-Za-z@]+)?)\.json$"),
    re.compile(r"_([A-Za-z]{2,3}(?:_[A-Za-z]+)?)\.properties$"),
)


def history():
    out = subprocess.run(
        ["git", "-C", REPO, "log", "--all",
         "--pretty=format:@@@%aN|%aE|%ad", "--date=format:%Y", "--name-only"],
        capture_output=True, text=True, check=True).stdout

    people = collections.defaultdict(
        lambda: {"names": collections.Counter(), "years": set(),
                 "commits": 0, "langs": set(), "unknown": set()})
    current = None

    for line in out.splitlines():
        if line.startswith("@@@"):
            name, email, year = line[3:].split("|")
            current = people[email.lower()]
            current["names"][name] += 1
            current["years"].add(int(year))
            current["commits"] += 1
        elif line.strip() and current is not None:
            path = line.strip()
            if not LANG_DIR.search(path):
                continue
            base = os.path.basename(path)
            for pattern in LANG_FILE:
                match = pattern.search(base)
                if not match:
                    continue
                code = match.group(1)
                if code in LANG:
                    current["langs"].add(code)
                elif code != "en":
                    current["unknown"].add(code)
                break

    return people


def period(person):
    low, high = min(person["years"]), max(person["years"])
    return str(low) if low == high else f"{low}-{high}"


def name_of(person):
    return person["names"].most_common(1)[0][0]


def table(people, emails, translations):
    header = ("| Contributor | %s | Commits | Period |\n|---|---|---|---|"
              % ("Languages" if translations else "Scope"))
    rows = []
    for email in emails:
        person = people[email]
        if translations:
            middle = ", ".join(sorted({LANG[c] for c in person["langs"]})) or "-"
            if email in NOTE:
                middle += f" ({NOTE[email]})"
        else:
            middle = CODE[email][1] or "core development"
        rows.append(f"| {name_of(person)} <{email}> | {middle} | "
                    f"{person['commits']} | {period(person)} |")
    return header + "\n" + "\n".join(rows)


def groups(people):
    def group(predicate):
        return sorted(
            (e for e in people if predicate(e)),
            key=lambda e: (-people[e]["commits"], name_of(people[e]).lower()))

    return (group(lambda e: CODE.get(e, (None,))[0] == "Jeci"),
            group(lambda e: CODE.get(e, (None,))[0] == "External"),
            group(lambda e: e not in CODE and e not in BOT))


def render(people):
    jeci, external, translators = groups(people)
    weblate = people["noreply@weblate.org"]

    return f"""# Contributors

Alfresco Collabora Online is developed by [Jeci SARL](https://jeci.fr) with
contributions from outside the company. Two distinct kinds of contribution are
tracked separately below: **code and documentation** on one side,
**translations** on the other.

Every identity that has landed at least one commit is listed, pseudonyms
included. Names, e-mail addresses and handles are reproduced as git recorded
them, through the `.mailmap` at the repository root; contributors who work
through Hosted Weblate appear under the handle they chose there.

## Code and documentation

### Jeci SARL

{table(people, jeci, False)}

### External contributors

{table(people, external, False)}

## Translations

Translations reach this repository through the
[Collabora Online project on Hosted Weblate]({WEBLATE})
and land as commits on the message bundles
(`collabora-aca-extension/.../assets/i18n/*.json`,
`collabora-share-extension/.../web-extension/messages/*.properties`).
{len(translators)} people have contributed translations.

{table(people, translators, True)}

## Automation

| Account | Role | Commits | Period |
|---|---|---|---|
| Weblate <noreply@weblate.org> | Hosted Weblate service account, commits translations on behalf of the translators above | {weblate['commits']} | {period(weblate)} |

## Updating this file

This file is generated by `docs/generate-contributors.py` and must be
regenerated whenever an external contribution is merged — a merge request, a
patch sent by e-mail, or a new translator appearing in a Weblate batch:

```bash
python3 docs/generate-contributors.py
```

The script reads the git history but it cannot guess intent, so two tables in
its header are maintained by hand:

- **`CODE`** places a contributor under *Code and documentation* and gives the
  scope shown in the second column. An address absent from it is treated as a
  translator, which is the safe default: a code contribution left out of `CODE`
  is visible as an empty language cell.
- **`LANG`** maps a locale code to its English name. An unknown code stops the
  script rather than printing a bare `xx`, so a newly translated language has to
  be named before it can be credited.

A translation pushed by the Weblate service account still credits a person: that
account is a transport, not an author. Keep identities as recorded, pseudonyms
included, and do not de-anonymise a contributor who chose a handle.

A significant module written by someone outside Jeci also deserves a
`## Credits` section in that module's own `README.md`, as
`collabora-aca-extension/README.md` does, and an `SPDX-FileCopyrightText` header
on the files they authored.
"""


def main():
    people = history()
    for email in EXCLUDED:
        people.pop(email, None)
    unknown = set()
    for email in groups(people)[2]:
        unknown |= people[email]["unknown"]
    if unknown:
        sys.exit("unmapped locale codes, add them to LANG: "
                 + ", ".join(sorted(unknown)))
    with open(OUTPUT, "w", encoding="utf-8") as handle:
        handle.write(render(people))
    print(f"{OUTPUT}: {len(people)} identities")


if __name__ == "__main__":
    main()
