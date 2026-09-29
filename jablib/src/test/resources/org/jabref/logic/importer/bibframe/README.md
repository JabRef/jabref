# Converter fixtures

- Sources: `book.marcxml` derives from `MarcXmlParserTestBook.xml` and covers
  edition (`250`), page count (`300`), and series (`490`). `article.marcxml`
  combines `MarcXmlParserTestArticle.xml` with the serial host from
  `DnbMarcXmlParentJournalRecord.xml`; its identifiers are synthetic.
- Inputs: both records have fixed `001` IDs and `008` dates. MARC preprocessing
  is disabled because splitting these standalone records adds Instances.
- LOC coverage: the converter test selects a record with `856` from the pinned
  `dataset/loc_general.xml` to check supplementary URLs and page extents.
- Forward converter: `lcnetdev/marc2bibframe2` at
  `ed9abb038214474e8fc8ba4035d01c42fe0246de`. Regenerate from this directory:

  ```sh
  for name in book article; do
    xsltproc --stringparam baseuri https://example.org/jabref/ \
      --stringparam idfield 001 \
      --stringparam pGenerationDatestamp 2020-01-01T00:00:00Z \
      --stringparam idsource http://id.loc.gov/vocabulary/organizations/dlc \
      /path/to/marc2bibframe2/xsl/marc2bibframe2.xsl "$name.marcxml" > "$name.rdf"
  done
  ```

- Reverse converter: `lcnetdev/bibframe2marc` at
  `36a96c813437ac714e8c9479b2f7be56dc78671d`. Run `make` first; its
  generated stylesheet needs one linked Work/Instance description per input.
  Set `pRecordId` to keep the reverse MARC `001` stable.

<!-- markdownlint-disable-file MD013 MD033 MD041 -->
