# Keep study as a DTO

## Context and Problem Statement

The study holds query and library entries that could be replaced respectively with complex query and fetcher instances.
This poses the question: should the study remain a pure DTO object or should it contain direct object instances?

## Considered Options

* Keep study as DTO and use transformers
* Replace entries with instances

## Decision Outcome

Chosen option: "Keep study as DTO and use transformators", because comes out best (see below).

## Pros and Cons of the Options

### Keep study as DTO and use transformers

* Good, because no need for custom serialization
* Good, because deactivated fetchers can be documented (important for traceable Searching (SLRs))
* Bad, because Entries for databases and queries needed

### Replace entries with instances

* Good, because no need for database and query entries
* Bad, because custom de-/serializers for fetchers and complex queries needed
* Bad, because harder to maintain than using "vanilla" jackson de-/serialization
* … <!-- numbers of pros and cons can vary -->
