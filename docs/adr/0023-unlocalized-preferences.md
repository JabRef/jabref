# Unlocalized Preferences

## Context and Problem Statement

Currently, JabRef uses some localized preferences. Example: The entry editor offers a tab "Comments". This heading can be customized.

We want to remove the Localization-dependency from `JabRefPreferences` and move the Localization to where the string is used.

The problem is: How to store the default values?

## Decision Drivers

* Preferences logic should not be mixed with translation logic.

## Considered Options

* Store the unlocalized string and consumers directly call Localization.lang
* Store the unlocalized string and consumers check if its default value
* Localized defaults
* Store changed preferences
* Store the localized default in the corresponding preference object
* Set the localized defaults in the get* method

## Decision Outcome

Chosen option: "Store the unlocalized string and consumers directly call Localization.lang", because comes out best (see below).

## Pros and Cons of the Options

### Store the unlocalized string and consumers directly call Localization.lang

The string is stored in English language. Consumers then just call `Localization.lang(String)` without any further check whether the stored string is default or not.

Example: If the user puts "Abstract" as a custom tab name (assume it's not in default for the moment), then when displaying the tab, we get the localized string, say "Zusamenfassung" if the language is set to German. If the user already puts "Zusammenfassung" in the preferences, then `Localization.lang` doesn't do anything and returns "Zusammenfassung" again. There might be very rare cases where the user puts in a word in some language, which is a valid english word with a different translation (artificial example: Chinese user puts yue (moon) in, which is a valid english word (synonymy for Cantonese), so that Jabref would show Guǎngdōng huà (Cantonese in Chinese)). However, these cases should be super rare with our limited dictionary. Would make the implementation a lot easier and might actually be what the user hopes to get

* Good, because Easy to implement
* Good, because Resolves force to separate logic from translation logic
* Good, because No check for the return value being the default value
* Bad, because When using the preference, the caller has to know that `Localization.lang` needs to be called. This can be resolved by a JavaDoc comment.
* Bad, because Default string in the preference needs to be held consistent with the content in `JabRef_en.properties` manually.
* Bad, because On each update of the string, there will be no automatic translation, because the string changed.
* Bad, because The language consistency checks need to updated to ignore these strings.

### Store the unlocalized string and consumers check if its default value

The string is stored in English language. Consumers then check the String they got as a preference against the defaults. If it matches, localize it. Otherwise, use it.

* Good, because Easy to implement
* Bad, because The caller needs to know that they need to do the default check (and call `Localization.lang`)
* Bad, because Check for the return value being the default value
* Bad, because (similar to option "Store the unlocalized string and directly call Localization.lang)

### Localized defaults

The default preference values are localized. That means that if JabRef does not find a value of the preference, it creates puts a default value. This value is the localized one.

* Good, because This is the implementation as of August, 2021
* Bad, because does not resolves force to separate logic from translation logic
* Bad, because Does not allow to change the language (and automatically adapt the strings in the default string case)

### Store changed preferences

Store the preference only when it was changed by the user. Otherwise, leave it empty or `Optional.empty()`.

* Good, because when a consumer gets such an empty preference, it knows that it needs to read the default and localize it.
* Bad, because this won't work if users actually want something to be empty.
* Bad, because inconsistent to current way preferences work
* Bad, because does not work for existing users (because default values are set); would need effort at preference migration

### Store the localized default in the corresponding preference object

In the example, the values should be moved to `EntryEditorPreferences`.

* Bad, because does not resolves force to separate preference logic from translation logic

### Set the localized defaults in the get* method

In the example: `org.jabref.preferences.JabRefPreferences#getEntryEditorPreferences` will use the language string.

* Bad, because does not resolve the force that `JabRefPreferences` should not contain translation code.
