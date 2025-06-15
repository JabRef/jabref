package org.jabref.logic.importer.fileformat.misc;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

public class TxtImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return true;
    }

    @Override
    public String getId() {
        return "txt";
    }

    @Override
    public String getName() {
        return "TXT";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for plain text files");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.TXT;
    }
}
