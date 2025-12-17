---
parent: Requirements
---
# Search

This page collects requirements on the search.

Indirectly, the requirements are listed at <https://docs.jabref.org/finding-sorting-and-cleaning-entries/search>.
This page tries to collect issues from users as requirements to enable better tracing in the code.

> ![NOTE]
> Currently, no implementation is linked

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

## Search for citation key
`req~jabgui.search.syntax.citation-key~1`

Issue: [#10490](https://github.com/JabRef/jabref/issues/10490)

Enable to quickly search for a citation key.

<!-- markdownlint-disable-file MD022 -->
