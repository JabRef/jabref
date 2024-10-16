---
parent: Decision Records
nav_order: 23
---
# Localized Preferences

* Status: proposed
* Date: 2024-10-16

## Context and Problem Statement

Currently, JabRef uses some localized preferences. We want to remove the Localization-dependency from the `JabRefPreferences` and move the Localization to where the String is used.
The problem is how to store the default values.

## Considered Options

* Localize Defaults
* Store the preference only when it was changed by the user
* Store the unlocalized string
* Mark default value with `%`

## Decision Outcome

Chosen option: "Mark default value with `%`", because it achieves goals without requiring too much refactoring and reuses a pattern already in use.

## Considered Options

### Localize Defaults

* Good, because it is the current implementaton

### Store the pPreference only when it was changed by the user

Otherwise, leave it empty. When a consumer gets such an empty preference, it knows that it needs to read the default and localize it.

- Bad, because this won't work if users actually want something to be empty.

### Store the unlocalized string

Consumers then check the string they got as a preference against the defaults. If it matches, localize it. Otherwise, use it.

### Mark default value with `%`

If user stores value, the value is stored as is.
The default value is stored with `%` in front.
A caller has to localize any stored value in the preferences if the string is 'escaped' by `%` as the first character

- Good, because clear distinction between default value and user-supplied value
- Good, because on update of JabRef's defaults, the string is not modified
- Good, because already used in fxml files to indicate translateable strings.
