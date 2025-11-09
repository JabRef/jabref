# Library

**Synonyms:** BibTeX file, `.bib` file, Database file, Bibliographic library

## Meaning

A **library** is a data file that stores a collection of bibliographic entries in a structured format (**BibTeX** or **BibLaTeX**).
In JabRef, a library is the **working data source** containing all references managed by the user — it is the digital equivalent of a physical card catalog.

Each library file typically has the extension `.bib` and can contain hundreds or thousands of bibliographic entries, along with metadata such as groups, keywords, and linked files.

## Delimitation (Scope and Exclusions)

* **Not the same as a bibliography:**
  A *[bibliography](bibliography.md)* is the conceptual list of relevant works; a *library* is the file storing the corresponding data.
* **Not the same as references**:
  [References](references.md) are the specific works cited in a publication, often a subset of a larger library.
* **Not the same as a database:**
  While technically similar, JabRef libraries are *file-based*; databases (e.g., SQL, Zotero, or shared repositories) provide multi-user or networked storage.
* **Not the same as a citation:**
  A *[citation](citation.md)* refers to an entry in the library but is not part of the data container itself.

## Validity

A library is valid as long as it can be parsed by JabRef or other BibTeX-compatible tools.

It may represent:

* a user’s complete reference collection,
* a project-specific subset,
* or a shared file under version control.

## Naming and Uniqueness

* Identified by its **file path** and **name** (e.g., `references.bib`).
* Each entry inside a library must have a **unique [citation key](citation-key]**.
* Libraries may be linked or merged to form larger collections.

## Open Issues / Uncertainties

* Historically, this was called `database` in JabRef and there is still work to do to rename it.
* Encoding inconsistencies (e.g., UTF-8 vs. legacy encodings) can affect portability.
* Some tools extend the BibTeX syntax (e.g., BibLaTeX fields), leading to partial incompatibility.
* Synchronization between multiple libraries can cause key collisions.

## Related Terms (Cross-References)

[Bibliography](bibliography.md), [Citation](citation.md), [Citation key](citation-key.md)
