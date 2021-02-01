# Only translated strings in language file

## Context and Problem Statement

JabRef has translation files `JabRef_it.properties`, ... There are translated and untranslated strings. Which ones should be in the translation file?

## Decision Drivers

* Translators should find new strings to translate easily
* New strings to translate should be written into `JabRef_en.properties` to enable translation by the translators
* Crowdin should be kept as translation platform, because 1\) it is much easier for the translators than the GitHub workflow and 2\) it is free for OSS projects.

## Considered Options

* Only translated strings in language file
* Translated and untranslated strings in language file, have value the untranslated string to indicate untranslated
* Translated and untranslated strings in language file, have empty to indicate untranslated

## Decision Outcome

Chosen option: "Only translated strings in language file", because comes out best \(see below.

## Pros and Cons of the Options

### Only translated strings in language file

* Good, because Crowdin supports it
* Bad, because translators need tooling to see untranslated strings
* Bad, because issues with FXML \([https://github.com/JabRef/jabref/issues/3796](https://github.com/JabRef/jabref/issues/3796)\)

### Translated and untranslated strings in language file, have value the untranslated string to indicate untranslated

* Good, because no issues with FXML
* Good, because Crowdin supports it
* Bad, because untranslated strings cannot be identified easily in Latin languages

### Translated and untranslated strings in language file, have empty to indicate untranslated

* Good, because untranslated strings can be identified easily
* Good, because works with FMXL \(?\)
* Bad, because Crowdin does not support it

## Links

* Related to [ADR-0001](0001-use-crowdin-for-translations.md).

<!-- markdownlint-disable-file MD024 -->
