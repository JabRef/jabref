---
nav_order: 3
has_children: true
has_toc: false
---
# Architectural Decisions

Architectural decisions for JabRef:

* [ADR-0000](./adr/0000-use-markdown-architectural-decision-records.md) - Use Markdown Architectural Decision Records
* [ADR-0001](./adr/0001-use-crowdin-for-translations.md) - Use Crowdin for translations
* [ADR-0002](./adr/0002-use-slf4j-for-logging.md) - Use slf4j together with log4j2 for logging
* [ADR-0003](./adr/0003-use-gradle-as-build-tool.md) - Use Gradle as build tool
* [ADR-0004](./adr/0004-use-mariadb-connector.md) - Use MariaDB Connector
* [ADR-0005](./adr/0005-fully-support-utf8-only-for-latex-files.md) - Fully Support UTF-8 Only For LaTeX Files
* [ADR-0006](./adr/0006-only-translated-strings-in-language-file.md) - Only translated strings in language file
* [ADR-0007](./adr/0007-human-readable-changelog.md) - Provide a human-readable changelog
* [ADR-0008](./adr/0008-use-public-final-instead-of-getters.md) - Use public final instead of getters to offer access to immutable variables
* [ADR-0009](./adr/0009-use-plain-junit5-for-testing.md) - Use Plain JUnit5 for advanced test assertions
* [ADR-0010](./adr/0010-use-h2-as-internal-database.md) - Use H2 as Internal SQL Database
* [ADR-0011](./adr/0011-test-external-links-in-documentation.md) - Test external links in documentation
* [ADR-0012](./adr/0012-handle-different-bibEntry-formats-of-fetchers.md) - Handle different bibentry formats of fetchers by adding a layer
* [ADR-0013](./adr/0013-add-native-support-biblatex-software.md) - Add Native Support for BibLatex-Software
* [ADR-0014](./adr/0014-separate-URL-creation-to-enable-proper-logging.md) - Separate URL creation to enable proper logging
* [ADR-0015](./adr/0015-support-an-abstract-query-syntax-for-query-conversion.md) - Query syntax design
* [ADR-0016](./adr/0016-mutable-preferences-objects.md) - Mutable preferences objects
* [ADR-0017](./adr/0017-allow-model-access-logic.md) - Allow org.jabref.model to access org.jabref.logic
* [ADR-0018](./adr/0018-use-regular-expression-to-split-multiple-sentence-titles.md) - Use regular expression to split multiple-sentence titles
* [ADR-0019](./adr/0019-implement-special-fields-as-seperate-fields.md) - Implement special fields as separate fields
* [ADR-0020](./adr/0020-use-Jackson-to-parse-study-yml.md) - Use Jackson to parse study.yml
* [ADR-0021](./adr/0021-keep-study-as-a-dto.md) - Keep study as a DTO
* [ADR-0022](./adr/0022-remove-stop-words-during-query-transformation.md) - Remove stop words during query transformation
* [ADR-0023](./adr/0023-localized-preferences.md) - Localized Preferences
* [ADR-0024](./adr/0024-use-/README.md#-as-indicator-for-BibTeX-string-constants.md) - Use `#` as indicator for BibTeX string constants

For new ADRs, please use [template.md](https://github.com/JabRef/jabref/tree/main/docs/adr/template.md) as basis. More information on the used format is available at [https://adr.github.io/madr/](https://adr.github.io/madr/). General information about architectural decision records is available at [https://adr.github.io/](https://adr.github.io). Then add them to the above list.
