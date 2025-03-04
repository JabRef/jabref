---
nav_order: 41
parent: Decision Records
---
<!-- we need to disable MD025, because we use the different heading "ADR Template" in the homepage (see above) than it is foreseen in the template -->
<!-- markdownlint-disable-next-line MD025 -->
# Use one language string for pluralization localization

## Context and Problem Statement

For user-facing messages, sometimes, it needs to be counted: E.g., 1 entry updated, 2 entries updated, etc.

In some languages, there is not only "one" and "more than one", but other forms:

* zero → “لم نزرع أي شجرة حتى الآن”
* one → “لقد زرعنا شجرة ١ حتى الآن”
* two → “لقد زرعنا شجرتين ٢ حتى الآن”
* few → “لقد زرعنا ٣ شجرات حتى الآن”
* many → “لقد زرعنا ١١ شجرة حتى الآن”
* other → “لقد زرعنا ١٠٠ شجرة حتى الآن”

(Example is from [Pluralization: A Guide to Localizing Plurals](https://phrase.com/blog/posts/pluralization/))

How to localize pluralization?

## Decision Drivers

* Good English language
* Good localization to other languages

## Considered Options

* Use one language string for pluralization (no explicit pluralization)
* Use singular and plural
* Handling of multiple forms

## Decision Outcome

Chosen option: "Use one form only (no explicit pluralization)", because it is the most easiest to handle in the code.

## Pros and Cons of the Options

### Use one language string for pluralization (no explicit pluralization)

Example:

- `Imported 0 entry(s)`
- `Imported 1 entry(s)`
- `Imported 12 entry(s)`

There are sub alternatives here:

* `Imported %0 entry(ies)`.
* `Number of entries imported: %0` (always use "other" plural form)

These arguments are for the general case of using a single text for all kinds of numbers:

* Good, because easy to handle in the code
* Bad, because reads strange in English UI

### Use singular and plural

Example:

- `Imported 0 entries`
- `Imported 1 entry`
- `Imported 12 entries`

* Good, because reads well in English
* Bad, because all localizations need to take an `if` check for the count
* Bad, because Arabic not localized properly

### Handling of multiple forms

Example:

- `Imported 0 entries`
- `Imported 1 entry`
- `Imported 12 entries`

Code: `Localization.lang("Imported %0 entries", "Imported %0 entry.", "Imported %0 entries.", "Imported %0 entries.", "Imported %0 entries.", "Imported %0 entries.", count)`

* Good, because reads well in English
* Bad, because sophisticated localization handling is required
* Bad, because no Java library for handling pluralization is known
* Bad, because Arabic not localized properly

## More Information

- [Pluralization: A Guide to Localizing Plurals](https://phrase.com/blog/posts/pluralization/)
- [Language Plural Rules](https://www.unicode.org/cldr/charts/43/supplemental/language_plural_rules.html)
- [Unicode CLDR Project's Plural Rules](https://cldr.unicode.org/index/cldr-spec/plural-rules)
- [Implementation in Mozilla Firefox](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/PluralRules)
- [SX discussion on plural forms](https://english.stackexchange.com/a/90283/66058)

<!-- markdownlint-disable-file MD004 -->
