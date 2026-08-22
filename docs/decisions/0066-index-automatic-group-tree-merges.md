---
nav_order: 0066
parent: Decision Records
---
<!-- markdownlint-disable-next-line MD041 -->

# Index Automatic Group Tree Merges

## Context and Problem Statement

Automatic groups create one or more group-tree paths for every entry in the active library. The previous merge collector searched the current sibling list to locate an equivalent node for every generated path. Large libraries with many distinct keywords or person names therefore spent increasing time re-evaluating groups during an active-database change, delaying unrelated background work such as preview rendering.

How should automatic group paths be merged without retaining an additional index after the tree is built?

## Decision Drivers

* Keep active-database changes responsive for large libraries.
* Preserve the existing group equality and resulting tree structure.
* Avoid permanent memory overhead for the constructed group tree.

## Considered Options

* Continue scanning sibling lists for equivalent nodes.
* Build temporary hash indexes for root and child groups during the merge.
* Cache automatic group trees across database activations.

## Decision Outcome

Chosen option: "build temporary hash indexes for root and child groups during the merge", because it finds equivalent siblings in expected constant time while using the same `AbstractGroup` equality semantics as the previous collector.

### Consequences

* Good, because automatic group construction scales with the generated paths instead of repeatedly scanning sibling lists.
* Good, because the resulting group tree and its equality semantics are unchanged.
* Neutral, because hash indexes consume temporary memory proportional to the generated groups while the tree is being built.
* Good, because the indexes are method-local and become eligible for garbage collection after the merge; they do not increase retained memory for an open library.
* Bad, because group implementations must continue to provide compatible `equals` and `hashCode` implementations.

### Confirmation

`TreeCollectorTest` verifies that children from equivalent roots are merged into one tree. Automatic group tests verify date, entry-type, keyword, and person group construction.

## Pros and Cons of the Options

### Scan sibling lists

* Good, because it requires no auxiliary data structures.
* Bad, because merging each generated path can scan a growing list of sibling groups.

### Temporary hash indexes for root and child groups

* Good, because equivalent groups are found in expected constant time.
* Good, because indexes exist only for the duration of the merge.
* Bad, because it temporarily increases peak memory during group-tree construction.

### Cache automatic group trees across database activations

* Good, because repeated activation of an unchanged database could reuse prior work.
* Bad, because cache invalidation must account for every entry and group configuration change.
* Bad, because retained caches would increase memory usage for open libraries.
