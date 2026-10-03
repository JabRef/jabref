---
nav_order: 76
parent: Decision Records
---

# Support BIBFRAME alongside MARC 21

## Context and Problem Statement

[BIBFRAME](https://bibframe.org/docs/view/documentation-bf-primer/index.md)
is a linked-data model and vocabulary intended to succeed MARC 21 for
bibliographic description and exchange. The [Library of Congress](https://www.loc.gov/bibframe/)
describes a transition path for MARC 21, with published
[MARC 21-to-BIBFRAME conversion specifications](https://www.loc.gov/bibframe/mtbf/)
and [BIBFRAME-to-MARC 21 specifications](https://www.loc.gov/bibframe/bftm/index.html).
The [German National Library has a BIBFRAME project](https://www.dnb.de/DE/Professionell/ProjekteKooperationen/Projekte/BIBFRAME/bibframe_node.html),
and its [standards guidance](https://wiki.dnb.de/spaces/DINIAGKIM/pages/217535230/MARC%2BEmpfehlung%2B3.0)
describes replacement of MARC 21 as a longer-term prospect, not a completed
change. JabRef must handle both formats during this transition.

Should JabRef offer BIBFRAME exchange now, and if so, what happens to its
existing MARC 21 import support?

## Considered Options

* Keep only MARC 21 import until BIBFRAME replaces it in practice.
* Replace the MARC 21 importer with a BIBFRAME importer.
* Add BIBFRAME import and export alongside the existing MARC 21 importer.

## Decision Outcome

Chosen option: "Add BIBFRAME import and export alongside the existing MARC 21 importer", because both formats remain in use during the transition.

BIBFRAME 2.0 RDF/XML import and export are separate exchange paths. This lets
JabRef exchange bibliographic data with systems adopting BIBFRAME without
removing access to the substantial MARC 21 data still in use. The initial
BIBFRAME mapping is deliberately bounded to supported JabRef entry types and
fields; it is not a promise to preserve every BIBFRAME graph or MARC 21 field.
The current type mapping identifies a serial host as `Article`, otherwise
maps a `bf:Monograph` Work to `Book`, and uses `Misc` when neither signal is
present. Exported articles retain a serial host marker even if the journal
name is missing. The [BIBFRAME ontology](https://github.com/lcnetdev/bibframe-ontology/blob/main/bibframe.rdf)
defines other Work subclasses, such as `bf:Dataset`, that are not mapped yet.
The RDF/XML parser strategy is recorded in
[ADR 0075](0075-use-stax-for-bounded-bibframe-rdfxml.md).

### Consequences

* Users can import supported BIBFRAME records directly and export corresponding
  BIBFRAME descriptions, while continuing to import MARC 21 records.
* Both formats need independent compatibility tests against real-world data and
  the Library of Congress conversion tools as their mappings evolve.
* Conversion between the formats can lose information outside JabRef's supported
  mapping; BIBFRAME support does not imply that MARC 21 has been retired.

<!-- markdownlint-disable-file MD022 MD041 -->
