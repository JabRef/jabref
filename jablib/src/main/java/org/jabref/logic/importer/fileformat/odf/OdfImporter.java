package org.jabref.logic.importer.fileformat.odf;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.importer.util.TikaMetadataParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.util.ListUtil;

/**
 * General importer for Open Document Format files.
 */
public abstract class OdfImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.isZip(input);
    }

    @Override
    protected void extractAdditionalMetadata(BibEntry entry, TikaMetadataParser metadataParser) {
        List<String> authors = ListUtil.concat(metadataParser.getDcCreators(), metadataParser.getDcContributors());

        entry.setField(StandardField.AUTHOR, TikaMetadataParser.formatBibtexAuthors(authors));
    }
}
