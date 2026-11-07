---
parent: Requirements
---

# Export

## Export BIBFRAME 2.0 RDF/XML
`req~export.bibframe.rdfxml~1`

JabRef exports each entry as a linked BIBFRAME 2.0 Work and Instance in striped RDF/XML. Resource identifiers are deterministic for unchanged bibliographic content, unique for duplicate entries within one file, and independent of citation keys.

See [ADR 0076](../decisions/0076-support-bibframe-alongside-marc21.md) for the decision to support BIBFRAME alongside MARC 21.

Needs: impl, utest

<!-- markdownlint-disable-file MD013 MD033 MD041 -->
<!-- markdownlint-disable-file MD022 -->
