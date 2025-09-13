---
title: Hardcode StandardField Names and use exact or customized names otherwise
nav_order: 49
parent: Decision Records
status: accepted
date: 2025-09-13
---
<!-- markdownlint-disable-next-line MD025 -->
# Hardcode StandardField Names and use exact or customized names otherwise

## Context and Problem Statement

JabRef allows users to define custom fields and customizing StandardFields with arbitrary names and arbitrary casing. Users reported inconsistent casing of field names across the tabs in the details panes of the entry editor (Required/Optional/Other fields), within saved BibTeX files and preferences. 
Fields were partially forced to show in UI with a capital first letter by a ui method inside the Field model.
This inconsistency confuses users and makes it impossible to achieve a predictable, uniform presentation between UI and persisted data, especially when dealing with customized fields.

How should JabRef consistently determine the casing for field names in UI and persistence for both built‑in and custom fields?

## Decision Drivers

* Consistent user experience across all UI locations
* Predictable persistence and round‑trip stability (UI ↔ preferences ↔ .bib)
* Backward compatibility with existing libraries and preferences
* Internationalization (i18n) and localization concerns for built‑in fields
* Minimal complexity added to parsing/serialization logic
* Avoid breaking community conventions for BibTeX/BibLaTeX (lowercase keys in files)
* Avoid mixed casing rules per UI location
* Minimize changes to bib files

## Considered Options

* Hardcode display names for built‑in fields; preserve exact user/customized names for non‑standard fields; serialize canonical lowercase keys to .bib
* Make all field names (including built‑ins) fully user‑configurable for display casing across UI and persistence
* Normalize all field names to lowercase everywhere (UI, prefs, .bib)
* Normalize all field names to Title Case in UI and in prefs; lowercase in .bib

## Decision Outcome

Chosen option: "Hardcode display names for built‑in fields; preserve exact user/customized names for non‑standard fields; serialize canonical lowercase keys to .bib.", because comes out best (see below).

Rationale:
- Aligns with community conventions (lowercase keys in .bib), ensuring compatibility.
- Provides a consistent and localized UI for built‑ins by using canonical, hardcoded display labels.
- Respects users’ expectations for custom fields by preserving the casing they define everywhere in the UI and in preferences.
- Minimizes behavioral surprises and avoids mixed casing rules per UI location.

## Pros and Cons of the Options

### Hardcode built‑in display names; preserve custom names; lowercase in .bib

- Good, because:
    - Round‑trip: UI labels ↔ preferences ↔ UI remain stable.
    - Built‑in labels can be localized predictably (Title Case or localized form).
    - Consistent casing across Details pane (all tabs) and custom tabs.
    - Matches BibTeX convention for stored keys.
    - Supports localization of built‑ins.
    - .bib files keep canonical lowercase keys, matching common BibTeX/BibLaTeX practice.
    - Decouples model (internal key) from ui (display label).
- Bad, because:
    - Users cannot change casing of built‑in field display names (by design).
    - Requires a clear separation of “internal canonical key” vs. “display label,” which slightly increases conceptual complexity.
    - Migration needs to ensure older preferences do not inadvertently force lowercasing of custom fields in UI.

### Make all fields fully user‑configurable

- Good, because:
    - Maximum flexibility for users.
- Neutral, because:
    - Could still serialize lowercase to .bib for compatibility.
- Bad, because:
    - Increases settings complexity and risk of inconsistent UI/prefs.
    - Harder to localize built‑ins; can lead to team‑specific divergences.

### Lowercase everywhere

- Good, because:
    - Simplest to implement; fully consistent.
- Bad, because:
    - Poor UX; clashes with common expectations for UI labels.
    - Undermines localization and readability.

### Title Case everywhere in UI and prefs; lowercase in .bib

- Good, because:
    - UI looks consistent and readable.
- Neutral, because:
    - Still needs dual representation (UI vs. .bib).
- Bad, because:
    - Ignores user intent for custom fields’ casing (less flexibility).
    - Preferences may mask underlying canonical keys, complicating tooling.
