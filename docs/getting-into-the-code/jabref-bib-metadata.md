---
title: JabRef-specific metadata in .bib files
---

## Overview

In addition to standard BibTeX or biblatex fields, JabRef stores application-specific
metadata directly inside `.bib` files. This metadata is required to persist information
that is not part of the BibTeX/biblatex specification, such as group definitions and
other JabRef-specific state.

This is especially relevant for contributors and for users processing `.bib` files with
tools outside of JabRef.

## Storage format

JabRef-specific metadata is stored using BibTeX comment entries. These entries are ignored
by BibTeX and biblatex processors and therefore do not affect bibliography compilation.

The metadata is typically stored in `@Comment` blocks with a `jabref-meta` prefix.

Example:
```bibtex
@Comment{jabref-meta: groupstree:
0 AllEntriesGroup:;
1 KeywordGroup:Science\;0\;keywords\;science\;;
}
```

Such comment blocks may appear anywhere in the .bib file and contain structured
information used by JabRef to restore its internal application state.

## JabRef 4 and JabRef 5 compatibility

There is no explicit version marker stored in .bib files that allows reliably
distinguishing between JabRef 4 and JabRef 5.

Both versions rely on comment-based metadata blocks. JabRef determines compatibility by
parsing the available metadata rather than checking a version identifier.

As a result, it is not possible to reliably detect the JabRef version used to create a
.bib file solely based on the file contents.

## Limitations and interoperability

Since JabRef-specific metadata is stored as comments, other BibTeX tools will safely
ignore this information.

However, removing these comment blocks will also remove JabRef-specific data such as
group definitions or other UI-related state. Users who rely on these features should
ensure that the corresponding comment entries are preserved.