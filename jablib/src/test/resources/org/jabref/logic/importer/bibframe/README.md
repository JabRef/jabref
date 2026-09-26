> [!IMPORTANT]
> This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
>
> Read and follow:
> • [AGENTS.md](./AGENTS.md)
> • [CONTRIBUTING.md](./CONTRIBUTING.md)

# Converter fixtures

`book.marcxml` is a small standalone record based on `MarcXmlParserTestBook.xml`.
`article.marcxml` uses the title and author from `MarcXmlParserTestArticle.xml`
and the serial-host pattern from `DnbMarcXmlParentJournalRecord.xml`. Its DOI,
ISSN, and other identifiers are synthetic test values. Both records have fixed
`001` IDs and `008` dates. MARC preprocessing is deliberately disabled: each
input is already one record, and splitting it would add unrelated Instances.

The RDF/XML fixtures were generated with `lcnetdev/marc2bibframe2` commit
`ed9abb038214474e8fc8ba4035d01c42fe0246de`. Run from this directory,
after checking out that exact commit:

```sh
xsltproc --stringparam baseuri https://example.org/jabref/ \
  --stringparam idfield 001 \
  --stringparam pGenerationDatestamp 2020-01-01T00:00:00Z \
  --stringparam idsource http://id.loc.gov/vocabulary/organizations/dlc \
  /path/to/marc2bibframe2/xsl/marc2bibframe2.xsl book.marcxml > book.rdf
xsltproc --stringparam baseuri https://example.org/jabref/ \
  --stringparam idfield 001 \
  --stringparam pGenerationDatestamp 2020-01-01T00:00:00Z \
  --stringparam idsource http://id.loc.gov/vocabulary/organizations/dlc \
  /path/to/marc2bibframe2/xsl/marc2bibframe2.xsl article.marcxml > article.rdf
```

The reverse converter is `lcnetdev/bibframe2marc` commit
`36a96c813437ac714e8c9479b2f7be56dc78671d`. Run `make` in that checkout
before using `bibframe2marc.xsl`; the generated stylesheet is not committed
there. Its input is one linked Work/Instance description per document. Set
`pRecordId` when checking a fixture to keep the reverse MARC `001` stable.

<!-- markdownlint-disable-file MD013 MD033 MD041 -->
