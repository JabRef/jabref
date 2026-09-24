# Git Commit Message Localization Strategy

## Context
Automatically generating Git commit messages based on database differences (added, deleted, modified entries) requires internationalization (i18n). However, constructing messages dynamically across different languages poses several challenges:
- Sentence structure, ordering, and separators vary across languages.
- Proper pluralization rules differ significantly across languages.

## Considered Options
1. **Dynamic Fragment Construction**: Dynamically appending localized strings (e.g., "X entries added, Y entries modified").
   - *Con*: Violates localization standards as language syntax/ordering cannot be guaranteed across different locales.
2. **Static Pre-formatted Templates**: Using a template containing all variables (e.g., "Added X entries, modified Y entries, deleted Z entries").
   - *Con*: Cluttered with zero-value entries when certain actions are not performed.
3. **English-Only Messages (Selected)**: Generate automatic commit messages exclusively in English for now.
   - *Pro*: Simple, grammatically correct, avoids complex i18n pitfalls.
   - *Drawback / Inconsistency*: 
     - **Inconsistency to localization approach**: JabRef has a heavily localized user interface, but this specific feature will temporarily ignore non-English localizations.
     - **User Disruption**: Users who prefer non-English languages in JabRef might find unexpected English commit messages disruptive or confusing in their workflow.
     - **ICU4J/MessageFormat Examples**: Proper multi-language pluralization and phrasing (such as handling singular vs plural forms or word order rules) would require heavy libraries like [ICU4J Pluralization Support](https://stuartgunter.wordpress.com/2011/08/14/even-better-java-i18n-pluralisation-using-icu4j/) or future standards like [Unicode MessageFormat 2.0 Plural Selection](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html#plural-selection-message).

## Decision
We choose **Option 3 (English-Only Messages)** for commit message generation to ensure correctness and simplicity while avoiding improper dynamic string concatenation across locales.

## Future Considerations
If full localization support is revisited in the future, advanced i18n frameworks and standards should be evaluated:
- [ICU4J Pluralization Support](https://stuartgunter.wordpress.com/2011/08/14/even-better-java-i18n-pluralisation-using-icu4j/)
- [Phrase Guide on i18n Pluralization](https://phrase.com/blog/posts/pluralization/)
- [Unicode MessageFormat 2.0 Specification](https://unicode-org.github.io/icu/userguide/format_parse/messages/mf2.html#plural-selection-message)
