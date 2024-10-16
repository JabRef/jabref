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

* _Localize Defaults_ (current implementation)
* _Store the Preference only when it was changed by the user_. Otherwise, leave it empty. When a consumer gets such an empty preference, it knows that it needs to read the default and localize it. This won't work if users actually want something to be empty.
* _Store the unlocalized String._ Consumers then check the String they got as a preference against the defaults. If it matches, localize it. Otherwise, use it. 
* _Localize any stored value in the preferences if the string is 'escaped' by `%` as the first character._ Already used in fxml files to indicate translateable strings.

## Decision Outcome

Chosen option: "_Localize any stored value in the preferences if the string is 'escaped' by `%` as the first character._", because Achieves goals without requiring too much refactoring and reuses a pattern already in use.
