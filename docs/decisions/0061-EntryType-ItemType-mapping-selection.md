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
2. Use JabRef's natural BibTeX/BibLaTeX mapping
3. Use Zotero's natural mapping
4. Use Better BibTeX's mapping

## Decision Outcome

Chosen option: Use Better BibTeX's mapping in converter, because it provides both mapping directions, from entry type to item type and item type to entry type. When it comes to types that Better BibTeX does not support, we use JabRef's natural BibTeX/BibLaTeX mapping or Zotero's natural mapping.

### Consequences

* Good, because Better BibTeX supports mapping for both BibTeX and BibLaTeX entry types.
* Good, because Better BibTeX supports more entry types than citeproc-java.
* Good, because Better BibTeX provides mapping for both entry type to item type and item type to entry type.
* Good, because Better BibTeX is designed specifically for Zotero and BibTeX and BibLaTeX interoperability.
* Good, because the repository is actively maintained.
* Bad, because Better BibTeX does not support all JabRef's entry types.
* Bad, because Better BibTeX contains many-to-one mappings when converting entry type to item type, which may lose information.
* Bad, because we need to track Better BibTeX's updates, or import it as a dependency.

## Pros and Cons of the Options

### Use citeproc-java's mapping

Converter can be found via [BibTeXConverter#toType](https://github.com/michel-kraemer/citeproc-java/blob/09d31a49090e06e6ab062016012b593897b3cb26/citeproc-java/src/main/java/de/undercouch/citeproc/bibtex/BibTeXConverter.java#L420-L461)

* Good, because citeproc-java provides an existing converter, reducing the amount of time to customize mapping.
* Good, because citeproc-java is already a JabRef's submodule, no new dependency is needed
* Bad, because citeproc-java only provides converter from entry type to item type.
* Bad, because entry type to item type mapping contains multiple many-to-one mappings, mapping back from item type to entry type would lose information
* Bad, because citeproc-java converts unsupported entry type to "journal-article"
* Bad, because the repository is not actively maintained.

### Use JabRef's natural BibTeX/BibLaTeX mapping

JabRef's internal behavior to map item type to entry type

* Good, because the mapping follows JabRef's internal behavior.
* Good, because no new dependency is needed.
* Bad, because we have limited control on the mapping
* Bad, because it converts multiple item type to `misc`, which loses much information.

### Use Zotero's natural mapping

Zotero's internal behavior to map entry type to item type

* Good, because the mapping follows Zotero's internal behavior.
* Bad, because some entry types are not supported by Zotero.
* Bad, because we have limited control on the mapping.
* Bad, because round-trip conversion may cause mismatch (electronic -> web page -> online)

### Use Better BibTeX's mapping

1. Converter from entry type to item type can be found via [bibtex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/7b7237e60aad44c47656484cd5eaa40201882449/translators/bibtex/bibtex.ts#L632-L679).
2. Converter from item type to BibTeX entry type can be found via [bibtex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/7b7237e60aad44c47656484cd5eaa40201882449/translators/bibtex/bibtex.ts#L56-L131).
3. Converter from item type to BibLaTeX entry type can be found via [biblatex.ts](https://github.com/retorquere/zotero-better-bibtex/blob/7b7237e60aad44c47656484cd5eaa40201882449/translators/bibtex/biblatex.ts#L47-L127).

* Good, because Better BibTeX supports mapping for both BibTeX and BibLaTeX entry types.
* Good, because Better BibTeX supports more entry types than citeproc-java.
* Good, because Better BibTeX provides mapping for both entry type to item type and item type to entry type.
* Good, because Better BibTeX is designed specifically for Zotero and BibTeX and BibLaTeX interoperability.
* Good, because the repository is actively maintained.
* Bad, because Better BibTeX does not support all JabRef's entry types.
* Bad, because Better BibTeX contains many-to-one mappings when converting entry type to item type, which may lose information.
* Bad, because JabRef needs to track Better BibTeX's updates, or import it as a submodule.
