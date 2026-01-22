---
parent: Glossary
---

# Citation key

**Synonyms:** BibTeX key

## Meaning

A **citation key** is a short, unique identifier assigned to a bibliographic
entry. It is used in documents (e.g. LaTeX) and tools (e.g. JabRef) to refer
to that entry in citations, cross-references, and commands.

Example: `LunaOstos2024`

## Delimitation (Scope and Exclusions)

- It is **not** the same as:
  - a database primary key,
  - a persistent identifier (DOI, ISBN, URI),
  - a filename.
- It is not globally unique across all users or projects, only within the scope where the bibliography is used.

## Validity

- Must be unique **within a given bibliography file or project**.
- May change if the user renames it or regeneration rules change.
- Not guaranteed to be stable across different tools or imported/exported files
  unless explicitly preserved.

## Naming and Uniqueness

- Allowed characters and format depend on the target system (e.g. BibTeX vs. BibLaTeX) but are typically ASCII without spaces.
- JabRef and similar tools can **generate** citation keys based on patterns (author, year, title, etc.).
- Uniqueness is usually enforced by the reference manager; collisions must be resolved.

## Open Issues / Uncertainties

- No universal standard across all tools and workflows.
- Migration between tools or key-pattern changes can break existing documents if keys are not updated consistently.

## Related Terms (Cross-References)

- [Bibliography](bibliography.md)
- [Citation](citation.md)
