package org.jabref.logic.importer.fileformat.img;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.types.BiblatexNonStandardTypes;
import org.jabref.model.entry.types.EntryType;

public class JpgImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.hasMagicNumber(input, new char[]{(char) 0xFF, (char) 0xD8, (char) 0xFF});
    }

    @Override
    public String getId() {
        return "jpg";
    }

    @Override
    public String getName() {
        return "JPG";
    }

    @Override
    public String getDescription() {
        return Localization.lang("JPG image importer");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.JPG;
    }

    @Override
    protected EntryType getEntryType() {
        return BiblatexNonStandardTypes.Image;
    }
}
