---
nav_order: 75
parent: Decision Records
---

# Use StAX with an in-memory tree for BIBFRAME RDF/XML import

## Context and Problem Statement

JabRef needs to import linked BIBFRAME Work and Instance descriptions from the
Library of Congress converter and its own exporter. These documents use a
restricted striped RDF/XML shape, but links may refer to resources declared
later in the document. How should this initial importer parse them?

## Considered Options

* Add a general RDF parser and support arbitrary RDF/XML graph serialization.
* Use the existing StAX XML stack and resolve the supported resource links.
* Use DOM and query the complete XML document.

## Decision Outcome

Chosen option: "Use the existing StAX XML stack and resolve the supported resource links", because it supports the documented subset without adding a runtime dependency.

The importer builds an in-memory XML tree for the documented subset. This
supports forward resource references and alternate namespace prefixes. It
rejects DTDs and external entities. General RDF/XML features, such as
arbitrary blank-node identifiers and property attributes, remain outside the
import contract. The mapping is limited, but document size is not bounded by this parser.
Supporting those features would require more parsing logic or a general
RDF parser.
These are alternate ways to serialize RDF relationships and values, not
additional bibliographic fields. The pinned converter fixtures use child
elements and URI links for the mapped fields, without `rdf:nodeID` or
`rdf:parseType`; other RDF/XML producers may choose different syntax.
