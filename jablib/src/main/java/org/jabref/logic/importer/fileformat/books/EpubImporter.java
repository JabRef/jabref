package org.jabref.logic.importer.fileformat.books;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

public class EpubImporter extends TikaImporter {
    private static final char[] EPUB_HEADER_MAGIC_NUMBER = {0x50, 0x4b, 0x03, 0x04};

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.hasMagicNumber(input, EPUB_HEADER_MAGIC_NUMBER);
    }

    @Override
    public String getId() {
        return "epub";
    }

    @Override
    public String getName() {
        return "ePUB";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import the popular e-book file format ePUB");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.EPUB;
    }
}
