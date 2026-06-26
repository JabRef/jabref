---
nav_order: 64
parent: Decision Records
---
# Use citation-js's mapping for CSL item type/field <-> BibLaTeX type/field conversion

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
4. Use citation-js's mapping

## Decision Outcome

Chosen option: Use citation-js's mapping, because it supports more types and the conversion makes more sense.

### Consequences

* Good, because citation-js provides mapping between CSL JSON and Bib(La)TeX.
* Good, because citation-js provides mapping for both BibTeX and BibLaTeX.
* Good, because citation-js supports more CSL item types/fields than Better BibTeX.
* Good, because the repository is actively maintained.
* Good, because citation-js provides mapping logic to deal with different CSL JSON.
* Good, because some mapping in citation-js make more sense than Better BibTeX.
* Good, because citation-js is designed specifically for CSL and BibTeX and BibLaTeX interoperability.
* Good, because citation-js also supports other conversions. e.g. BibLaTeX to RIS.
* Bad, because JabRef needs to track citation-js's updates, or import it as a submodule.

## Pros and Cons of the Options

### Use citeproc-java's mapping

Converter can be found via [BibTeXConverter#toType](https://github.com/michel-kraemer/citeproc-java/blob/09d31a49090e06e6ab062016012b593897b3cb26/citeproc-java/src/main/java/de/undercouch/citeproc/bibtex/BibTeXConverter.java#L420-L461)

* Good, because citeproc-java is already a JabRef's submodule, no new dependency is needed
* Bad, because citeproc-java only provides mappings from entry type to item type.
* Bad, because citeproc-java maps unsupported entry type to `journal-article`.
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

* Good, because Better BibTeX provides mapping between Zotero item type and BibTeX/BibLaTeX entry type.
* Good, because Better BibTeX supports mapping for both BibTeX and BibLaTeX entry types.
* Good, because Better BibTeX supports more entry types than citeproc-java.
* Good, because Better BibTeX is designed specifically for Zotero and BibTeX and BibLaTeX interoperability.
* Good, because the repository is actively maintained.
* Bad, because Better BibTeX does not support all JabRef's entry types.
* Bad, because JabRef needs to track Better BibTeX's updates, or import it as a submodule.
* Bad, because some mappings in Better BibTeX do not make sense. For example, CSL item type `chapter` is mapped to `Incollection`, while `Inbook` is more suitable.

### Use citation-js's mapping

1. Mapping from CSL item type to BibTeX can be found via [biblatex.js](https://github.com/citation-js/citation-js/blob/main/packages/plugin-bibtex/src/mapping/biblatex.js).
2. Mapping from CSL item type to BibLaTeX can be found via [bibtex.js](https://github.com/citation-js/citation-js/blob/main/packages/plugin-bibtex/src/mapping/bibtex.js).

* Good, because citation-js provides mapping between CSL JSON and Bib(La)TeX.
* Good, because citation-js provides mapping for both BibTeX and BibLaTeX.
* Good, because citation-js supports more CSL item types/fields than Better BibTeX.
* Good, because the repository is actively maintained.
* Good, because citation-js provides mapping logic to deal with different CSL JSON.
* Good, because some mapping in citation-js make more sense than Better BibTeX. For example, citation-js maps CSL item type `chapter` to `Inbook`, Better BibTeX maps it to `Incollection`
* Good, because citation-js is designed specifically for CSL and BibTeX and BibLaTeX interoperability.
* Good, because citation-js also supports other mappings. e.g. BibLaTeX to RIS.
* Bad, because JabRef needs to track citation-js's updates, or import it as a submodule.

### More information

* Implementation PR: [#15946](https://github.com/JabRef/jabref/pull/15946)
* Implemented mapping can be found via [CSLItemTypeDefinitions.java](https://github.com/JabRef/jabref/blob/main/jablib/src/main/java/org/jabref/logic/openoffice/CSLItemTypeDefinitions.java)
