---
parent: Requirements
---
# Export

## Exports can be defined as Apache Velocity templates
`req~logic.exporter.velocity-template~1`

An export format can be defined as a single [Apache Velocity](https://velocity.apache.org/) template
(instead of a set of per-entry-type `.layout` files).
The template iterates over the sorted entries (`$entries`); each entry offers its type, citation key,
and field values (with field aliases, BibTeX strings, and `crossref` inheritance resolved) as plain strings,
where an unset field is the empty string.
See [ADR 39](../decisions/0039-use-apache-velocity-as-template-engine.md) for the engine choice.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
