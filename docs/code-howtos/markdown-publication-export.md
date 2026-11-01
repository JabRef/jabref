---
parent: Code Howtos
---

# Markdown publication export

`MarkdownPublicationExporter` renders entries through
`resource/layout/markdown-publications/publication.layout`.
The exporter resolves bibliographic fields before rendering and passes a
validated DOI or URL to the layout. Only HTTP, HTTPS, FTP, and FTPS destinations
with an authority are accepted; `www.` destinations receive an HTTPS scheme.
Invalid DOIs fall back to a valid URL, otherwise the title remains plain text.
The original library entries are not modified.

Group descriptions and fallback names are escaped as literal heading text.
Configured attachment directories supplement directories supplied by the caller;
supplied directories retain precedence.
Supplied invalid BibTeX month or day values make a date unusable for sorting,
rather than silently reducing its precision. A missing month or day still permits
a year-only or year-and-month date. An explicit `date` field takes precedence.

## Standard output

The CLI writes exports to a temporary file before copying them to standard
output. That temporary location is not the destination of shell redirection.
`Exporter.exportToStandardOutput` lets destination-sensitive exporters handle
this distinction without guessing a destination from a temporary filename.

Markdown standard-output exports omit local PDF links and retain usable online
PDF links. Use an explicit output filename when local PDF links are needed;
regular file exports calculate these links relative to the destination file.
