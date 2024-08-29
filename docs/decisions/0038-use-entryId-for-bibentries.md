---
title: Use BibEntry.getId for BibEntry at indexing
nav_order: 38
parent: Decision Records
---

<!-- markdownlint-disable-next-line MD025 -->
# Use `BibEntry.getId` for BibEntries at Indexing

## Context and Problem Statement

The `BibEntry` class has `equals` and `hashCode` implemented on the content of the bib entry.
Thus, if two bib entries have the same type, the same fields, and the same content, they are equal.

This, however, is not useful in the UI, where equal entries are not the same entries.

## Decision Drivers

* Simple code
* Not changing much other JabRef code
* Working Lucene

## Considered Options

* Use `BibEntry.getId` for indexing `BibEntry`
* Use `System.identityHashCode` for indexing `BibEntry`
* Rewrite `BibEntry` logic

## Decision Outcome

Chosen option: "Use `BibEntry.getId` for indexing `BibEntry`", because is the "natural" thing to ensure distinction between two instances of a `BibEntry` object - regardless of equality.
