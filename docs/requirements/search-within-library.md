---
parent: Requirements
---
# Search within a library

This page collects requirements on the search within a library.
Typically, a user uses the search bar to trigger a search in the current library.
They can also open up a popup to search across all libraries.

> Requirements on search regarding a fetcher are not covered here.
> Requirements on the syntax itself are not covered here, oo.
{: .prompt-note}

## Requirements sources

Indirectly, the requirements are listed at <https://docs.jabref.org/finding-sorting-and-cleaning-entries/search>.
This page tries to collect issues from users as requirements to enable better tracing in the code.

## Search for the name of the first authors
`req~jabgui.search.syntax.author-first-name~1`

Issue: [#10490](https://github.com/JabRef/jabref/issues/10490)

Enable to **quickly** search by first author name.

Example BibTeX entry:

```bibtex
@article{art_1,
  author = {John Demo}
}

@article{art_2,
  author = {John Demoing}
}
```

When searching for "author" "Demo" should match `art_1` only.

It is possible by regular expressions, but the user asked for "quickly".

> Currently, no implementation is linked
{: .prompt-note}

## Search for citation key
`req~jabgui.search.syntax.citation-key~1`

Issue: [#10490](https://github.com/JabRef/jabref/issues/10490)

Enable to quickly search for a citation key.

> Currently, no implementation is linked
{: .prompt-note}

## Full-text search without Postgres
`req~jabgui.search.fulltext.lucene-without-postgres~1`

When linked-file full-text indexing is enabled, users must be able to search the contents of linked files without enabling the experimental Postgres search backend.

Needs: impl, utest

## Safe handling of regex metacharacters when highlighting search results
`req~jabgui.search.highlighting.safe-regex-handling~1`

Issue: [#16539](https://github.com/JabRef/jabref/issues/16539)

A plain-text (non-regex) search term containing regex metacharacters (e.g. `*`, `{`, `[`) must never crash the entry preview. Such terms are highlighted as literal text, never interpreted as regex.

If the user explicitly opts into regex search (the `=~` operator, or the regex toggle) and the given pattern is not a valid regex, the search bar shows its existing "Illegal search expression" indicator instead of silently failing to highlight with no feedback.

Needs: impl, utest

<!-- markdownlint-disable-file MD022 -->
