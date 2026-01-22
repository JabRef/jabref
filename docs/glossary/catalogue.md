---
parent: Glossary
---

# Catalogue

**Synonyms:** Online library, Literature database, Reference source, Data provider

## Meaning

A **catalogue** is an online bibliographic data source that allows searching, retrieving, and importing metadata for scholarly works such as journal articles, conference papers, and books.
In JabRef, a catalogue refers to an **external service or repository** — for example, ACM Digital Library, IEEE Xplore, Google Scholar, CrossRef, or arXiv — from which bibliographic entries can be fetched.

Catalogues provide structured metadata (author, title, DOI, year, etc.) that JabRef imports to enrich or create local entries in a user’s library.

## Delimitation (Scope and Exclusions)

* **Not the same as a library:**
  A *[library](library.md)* is the local `.bib` file managed in JabRef; a *catalogue* is an external source from which data is imported.
* **Not the same as a bibliography:**
  A *[bibliography](bibliography.md)* is a conceptual collection of works; a *catalogue* is a retrieval system that provides access to such works.
* **Not the same as references:**
  *[References](references.md)* are selected works cited in a publication, possibly originating from multiple catalogues.
* **Not a search engine in general:**
  Although some catalogues expose web search interfaces, their primary function is structured metadata access.

## Validity

A catalogue is valid as long as its public interface (API or HTML interface) is operational and accessible.

Different catalogues may use:

* standard interfaces (e.g., **DOI**, **OAI-PMH**, **OpenSearch**),
* custom APIs (e.g., **CrossRef REST API**),
* or web-scraping-based access (e.g., Google Scholar).

## Naming and Uniqueness

Each catalogue is identified by:

* its **provider name** (e.g., “ACM Digital Library”), and
* its **base URL** or **API endpoint**.

In JabRef’s internal configuration, catalogues correspond to **[fetchers](../code-howtos/fetchers.md)**, each implementing a standardized interface for metadata retrieval.

## Open Issues / Uncertainties

* Availability and licensing of catalogue APIs may change without notice.
* Metadata quality and completeness vary by provider.
* Rate limits or CAPTCHA systems can restrict automated access.
* The term “catalogue” is JabRef-specific; other reference managers use “online database” or “data source.”

## Related Terms (Cross-References)

[Library](library.md), [Bibliography](bibliography.md), [Citation](citation.md)
