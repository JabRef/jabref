---
parent: Requirements
---
# Language server

## Hover on citation keys
`req~jabls.hover.citation-key~1`

Hovering over a citation key in a Markdown or LaTeX document shows the citation key, author, title, and year of every matching entry.
This also holds for each key of a citation containing several keys, such as `[@key1; @key2]`.

Needs: impl

## Bibliographies from Markdown front matter
`req~jabls.markdown.front-matter-bibliography~1`

When a Markdown document is opened or saved, the language server loads the `.bib` files listed in the `bibliography` key of its YAML front matter.
Both a single file and a list of files are supported.
Relative paths are resolved against the directory of the Markdown document.
Thus, citation keys can be resolved without opening the `.bib` file in the editor.

Needs: impl

<!-- markdownlint-disable-file MD022 -->
