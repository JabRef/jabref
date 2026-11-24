---
nav_order: 81
parent: Decision Records
---

# Export ordered BIBO authors and Dublin Core creators
`adr~export-ordered-bibo-authors-and-creators~1`

Needs: impl, utest

## Context and Problem Statement

The BIBO RDF exporter needs to retain author order while remaining compatible with consumers that read `dcterms:creator`.
The [BIBO author list](https://dcmi.github.io/bibo/#http://purl.org/ontology/bibo/authorList) links a document to an ordered collection; it does not itself assert a creator relation to each member.

## Considered Options

* Emit only `dcterms:creator` relations.
* Emit only an ordered `bibo:authorList`.
* Emit both relations and share the author nodes.

## Decision Outcome

Emit both relations and share the author nodes. Each creator is a member of the `rdf:Seq` referenced by `bibo:authorList`, in source order. Shared blank-node identifiers connect the two representations without relying on name matching.

Consumers can use direct creator relations or the ordered list. Blank-node identifiers are generated for each export to avoid collisions between entries, so repeated exports can differ in their serialized identifiers even when their RDF graphs are equivalent.

<!-- markdownlint-disable-file MD022 MD033 -->
