---
parent: Code Howtos
---
> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

# BIBFRAME 2.0 import and export

The `bibframe` format reads and writes BIBFRAME 2.0 striped RDF/XML. Its
resource graph has one top-level `bf:Work` and one top-level `bf:Instance` for
each entry, linked by `bf:hasInstance` and `bf:instanceOf`. A document may
contain several such pairs. The importer resolves nested resources and
`rdf:resource` references, as well as typed `rdf:Description` elements.
It does not claim general RDF/XML graph support, such as arbitrary blank-node
identifiers or property attributes. Those are alternate RDF/XML syntax forms,
not additional bibliographic fields, and are not needed for the pinned converter
fixtures. A general RDF parser library was considered;
the bounded striped subset and the official reverse converter's input shape
allow this implementation to use the existing XML stack without another runtime
dependency. The importer uses StAX to read XML and resolves the supported
resource links from its parsed descriptions.

| JabRef field | BIBFRAME path | Owner |
| --- | --- | --- |
| Title, subtitle | `bf:title/bf:Title/bf:mainTitle`, `bf:subtitle` | Instance, then Work |
| Author, editor | `bf:contribution/bf:Contribution/bf:agent/rdfs:label` with `bf:role` (`aut`, `edt`) | Work |
| Address, publisher, year | `bf:provisionActivity/bf:ProvisionActivity/bflc:simplePlace`, `bflc:simpleAgent`, `bflc:simpleDate` | Instance |
| ISBN, DOI | `bf:identifiedBy/bf:Isbn` or `bf:Doi`/`rdf:value` | Instance |
| ISSN | `bf:identifiedBy/bf:Issn/rdf:value` | Instance or journal host Work |
| Language, abstract | `bf:language`, `bf:summary/bf:Summary/rdfs:label` | Work |
| URL | `bf:electronicLocator` | Instance, then Work |
| Journal, pages | `bf:relation` with `partof` host Work and an ISSN; host Instance `bf:part` | Work relation |

A Work with an identified serial host becomes an Article; other linked Works
become Books. A host title alone is not enough to identify a journal. Data
outside this table, such as MARC administrative fields, edition, series,
physical description, subject headings, and additional Instances, is lost.
Citation keys are never inferred from resource URIs. The exporter hashes the
entry type and sorted bibliographic fields with SHA-256 to mint stable Work
and Instance URIs. Citation keys are excluded. Identical entries in one export
receive numbered suffixes so their resources remain separate. ISBN, ISSN, and
DOI values are preserved as bibliographic identifiers.

The fixture commands and pinned official converter commits are recorded beside
the fixtures in `jablib/src/test/resources/org/jabref/logic/importer/bibframe/README.md`.
The reverse converter accepts one description per invocation, so split a
multi-entry export into linked Work/Instance pairs before passing it to that
converter.

The pinned reverse converter emits an article host as MARC `773` without
subfield `7`. The unchanged `MarcXmlParser` uses that subfield to recognize an
Article and its journal, so those two values are lost at the final MARC import
stage. It also emits the language only in MARC `008`, which that parser does
not read. A generic URL becomes MARC `856` without the `Volltext` label that
the parser requires for a linked file. The reverse converter may add ISBD
punctuation to title and publication fields.

<!-- markdownlint-disable-file MD013 MD033 MD041 -->
