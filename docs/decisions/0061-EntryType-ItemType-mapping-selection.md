---
nav_order: 61
parent: Decision Records
---
# Entry type and item type mapping selection

## Context and Problem Statement

We are implementing compatibility with Zotero. Zotero uses its item types, while JabRef uses BibTeX/BibLaTeX entry types. We need a suitable mapping strategy that preserves as much information as possible in both directions, from JabRef to Zotero and from Zotero to JabRef.

Which mapping should JabRef use to convert between entry types and Zotero's item types?

## Decision Drivers

* Entry type to item type should try to avoid many-to-one mappings.
* Mapped item type should try to be semantically consistent with entry type.
* Mapped item type should contain as many fields from entry type as possible.

## Considered Options

1. Use citeproc-java's mapping
2. Use Zotero's import/export mapping
3. Use Better BibTeX's mapping

## Decision Outcome

Chosen option: Use Better BibTeX's mapping in converter, because it provides both mapping directions, from BibTeX/BibLaTeX entry type to Zotero item type and CSL/Zotero item type to entry type.

### Consequences

* Good, because Better BibTeX provides mapping directly from CSL item type to BibTeX/BibLaTeX entry type.
* Good, because Better BibTeX provides mapping between Zotero item type and BibTeX/BibLaTeX entry type.
* Good, because Better BibTeX supports mapping for both BibTeX and BibLaTeX entry types.
* Good, because Better BibTeX supports more entry types than citeproc-java.
* Good, because Better BibTeX is designed specifically for Zotero and BibTeX and BibLaTeX interoperability.
* Good, because the repository is actively maintained.
* Bad, because Better BibTeX does not support all JabRef's entry types.
* Bad, because Better BibTeX contains many-to-one mappings when converting BibTeX/BibLaTeX entry type to Zotero item type, which may lose information.
* Bad, because JabRef needs to track Better BibTeX's updates, or import it as a submodule.

## Pros and Cons of the Options

### Use citeproc-java's mapping

Converter can be found via [BibTeXConverter#toType](https://github.com/michel-kraemer/citeproc-java/blob/09d31a49090e06e6ab062016012b593897b3cb26/citeproc-java/src/main/java/de/undercouch/citeproc/bibtex/BibTeXConverter.java#L420-L461)

* Good, because citeproc-java provides an existing converter, reducing the amount of time to customize mapping.
* Good, because citeproc-java is already a JabRef's submodule, no new dependency is needed
* Bad, because citeproc-java only provides converter from entry type to item type.
* Bad, because entry type to item type mapping contains multiple many-to-one mappings, mapping back from item type to entry type would lose information
* Bad, because citeproc-java converts unsupported entry type to "journal-article"
* Bad, because the repository is not actively maintained.

### Use Zotero's import/export mapping

Zotero provides mappings from Zotero item type to BibTeX/BibLaTeX entry type. However, Zotero's reference mark uses CSL JSON, whose item types and field names differ from the ones in Zotero item type. Zotero does not provide a direct mapping from CSL item type to BibTeX/BibLaTeX entry type, so we first need to map CSL item types back to Zotero item types, then apply Zotero’s BibTeX/BibLaTeX export mapping.

1. Converter from Zotero item type to BibLaTeX entry type can be found via [BibLaTeX.js](https://github.com/zotero/translators/blob/cfc69de47e1e1fb88122cbbba6c30f56d6ed63fe/BibLaTeX.js#L135-L170)
2. Converter between Zotero item type to BibTeX entry type can be found via [BibTeX.js](https://github.com/zotero/translators/blob/cfc69de47e1e1fb88122cbbba6c30f56d6ed63fe/BibTeX.js#L219-L270)

* Good, because the mapping follows Zotero's internal behavior.
* Bad, because Zotero does not provide mapping directly from CSL item type to BibLaTeX/BibTeX entry type.
* Bad, because some entry types are not supported by Zotero.
* Bad, because we have limited control on the mapping.
* Bad, because round-trip conversion may cause mismatch (electronic -> web page -> online)

### Use Better BibTeX's mapping

1. Converter from entry type to item type can be found via [bibtex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/7b7237e60aad44c47656484cd5eaa40201882449/translators/bibtex/bibtex.ts#L632-L679).
2. Converter from CSL/Zotero item type to BibTeX entry type can be found via [bibtex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/7b7237e60aad44c47656484cd5eaa40201882449/translators/bibtex/bibtex.ts#L56-L131).
3. Converter from CSL/Zotero item type to BibLaTeX entry type can be found via [biblatex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/7b7237e60aad44c47656484cd5eaa40201882449/translators/bibtex/biblatex.ts#L47-L127).

* Good, because Better BibTeX provides mapping directly from CSL item type to BibTeX/BibLaTeX entry type.
* Good, because Better BibTeX provides mapping between Zotero item type and BibTeX/BibLaTeX entry type.
* Good, because Better BibTeX supports mapping for both BibTeX and BibLaTeX entry types.
* Good, because Better BibTeX supports more entry types than citeproc-java.
* Good, because Better BibTeX is designed specifically for Zotero and BibTeX and BibLaTeX interoperability.
* Good, because the repository is actively maintained.
* Bad, because Better BibTeX does not support all JabRef's entry types.
* Bad, because Better BibTeX contains many-to-one mappings when converting BibTeX/BibLaTeX entry type to Zotero item type, which may lose information.
* Bad, because JabRef needs to track Better BibTeX's updates, or import it as a submodule.
