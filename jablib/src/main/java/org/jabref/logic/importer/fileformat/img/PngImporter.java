package org.jabref.logic.importer.fileformat.img;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.importer.util.TikaMetadataParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.BiblatexNonStandardTypes;
import org.jabref.model.entry.types.EntryType;

public class PngImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.hasMagicNumber(input, new char[]{0xFFFD, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
    }

    @Override
    public String getId() {
        return "png";
    }

    @Override
    public String getName() {
        return "PNG";
    }

    @Override
    public String getDescription() {
        return Localization.lang("PNG image importer");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.PNG;
    }

    @Override
    protected void extractAdditionalMetadata(BibEntry entry, TikaMetadataParser metadataParser) {
        entry.setType(BiblatexNonStandardTypes.Image);

        metadataParser
                .getPngCreationTime()
                .ifPresent(date -> TikaMetadataParser.addDateCreated(entry, date));
    }
}
