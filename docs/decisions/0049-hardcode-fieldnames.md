---
title: Hardcode `StandardField` names and use exact or customized names otherwise
nav_order: 49
parent: Decision Records
status: accepted
date: 2025-09-13
---
<!-- markdownlint-disable-next-line MD025 -->
# Hardcode `StandardField` names and use exact or customized names otherwise (disallow customization of `StandardField`s)

## Context and Problem Statement

JabRef allows users to define custom fields and customizing `StandardField`s with arbitrary names and arbitrary casing. Users reported inconsistent casing of field names across the tabs in the details panes of the entry editor (Required/Optional/Other fields), within saved BibTeX files and preferences.
Fields were partially forced to show in UI with a capital first letter by a UI method inside the `Field` model.
A argument was made in issue [#116](https://github.com/JabRef/jabref/issues/116) about how to case the field names in the `.bib` file. It was then decided to always lowercase the field names, as BibTeX itself is case-insensitive in that matter, but convention in bibtex style is to lowercase it.
There were complains, that this inconsistency [confuses users](https://github.com/JabRef/jabref/issues/10590) and makes it impossible to achieve a predictable, uniform presentation between UI and persisted data, especially when dealing with customized fields.

How should JabRef consistently determine the casing for field names in UI and persistence for both built‑in and custom fields?

## Decision Drivers

* Consistent user experience across all UI locations
* Predictable persistence and round‑trip stability (UI ↔ preferences ↔ `.bib`)
* Backward compatibility with existing libraries and preferences
* Internationalization (i18n) and localization concerns for built‑in fields
* Minimal complexity added to parsing/serialization logic
* Avoid breaking community conventions for BibTeX/BibLaTeX (lowercase keys in files)
* Avoid mixed casing rules per UI location
* Minimize changes to bib files
* Respect the choice of the user for casing of custom fields

## Considered Options

* Hardcode `StandardField` names and use exact or customized names otherwise (disallow customization of `StandardField`s)
* Make all field names (including `StandardField`s) fully user‑configurable for display casing across UI and persistence
* Normalize all field names to lowercase everywhere (UI, preferences, `.bib`)
* Normalize all field names to title case in UI and in preferences; lowercase in `.bib`

## Decision Outcome

Chosen option: "Hardcode `StandardField` names and use exact or customized names otherwise," because

* Aligns with community conventions (lowercase keys in `.bib`), ensuring compatibility.
* Provides a consistent and localized UI for built‑ins by using canonical, hardcoded display labels.
* Respects users’ expectations for custom fields by preserving the casing they define everywhere in the UI and in preferences.
* Minimizes behavioral surprises and avoids mixed casing rules per UI location.

## Pros and Cons of the Options

<!-- markdownlint-disable-next-line MD024 -->
### Hardcode `StandardField` names and use exact or customized names otherwise (disallow customization of `StandardField`s)

<!-- markdownlint-disable-next-line MD004 -->>
- For the build-in types (`StandardField`), the display names are hard-coded. Users cannot customize this. Optional/required can still be customized.
<!-- markdownlint-disable-next-line MD004 -->>
- Preserve exact user/customized names for non‑standard fields
<!-- markdownlint-disable-next-line MD004 -->>
- Serialize as customized (and lower-case as standard) to `.bib` file

* Good, because round‑trip: UI labels ↔ preferences ↔ UI remain stable.
* Good, because built‑in labels can be localized predictably (title case or localized form).
* Good, because consistent casing across entry editor tabs.
* Good, because matches BibTeX convention for stored keys.
* Good, because supports localization of displaying of `StandardField` names.
* Good, because `.bib` files keep canonical lowercase keys in the default case. This matches common BibTeX/BibLaTeX practice.
* Good, because decouples model (internal key) from UI (display label).
* Bad, because users cannot change casing of built‑in field display names by using the entry customization.
* Bad, because perceived as inconsistent at [#10590](https://github.com/JabRef/jabref/issues/10590).
* Bad, because requires a clear separation of internal key vs. display label, which slightly increases conceptual complexity.
* Bad, because migration needs to ensure older preferences do not inadvertently force lowercasing of custom fields in UI.

### Make all fields fully user‑configurable

This is option "Hardcode `StandardField` names and use exact or customized names otherwise (disallow customization of `StandardField`s)" with allowing customization of `StandardField`s.

* Good, because provides maximum flexibility for users: Users can change casing of built‑in field display names by using the entry customization.
* Good, because perceived as consistent at [#10590](https://github.com/JabRef/jabref/issues/10590).
* Bad, because increases settings complexity and risk of inconsistent UI and preferences.
* Bad, because harder to localize built‑ins; can lead to team‑specific divergences.

### Normalize all field names to lowercase everywhere (UI, preferences, `.bib`)

* Good, because simplest to implement; fully consistent.
* Bad, because Poor UX; clashes with common expectations for UI labels.
* Bad, because Undermines localization and readability.

### Normalize all field names to title case in UI and in preferences; lowercase in `.bib`

* Good, because UI looks consistent and readable.
* Bad, because ignores user intent for custom fields’ casing (less flexibility).
* Bad, because preferences may mask underlying canonical keys, complicating tooling.
