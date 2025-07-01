package org.jabref.logic.importer.fileformat.microsoft;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.importer.util.Constants;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

public class RtfImporter extends TikaImporter {
    private static final char[] RTF_MAGIC_NUMBER = new char[]{'{', '\\', 'r', 't', 'f', '1'};

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return Constants.hasMagicNumber(input, RTF_MAGIC_NUMBER);
    }

    @Override
    public String getId() {
        return "rtf";
    }

    @Override
    public String getName() {
        return "RTF";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Rich Text File importer");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.RTF;
    }
}
