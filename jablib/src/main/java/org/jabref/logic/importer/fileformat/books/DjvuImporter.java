package org.jabref.logic.importer.fileformat.books;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

public class DjvuImporter extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        // DJVU start with "AT&TFORM" and then "DJVU" some time after that.

        char[] buffer = new char[64];
        int read = input.read(buffer, 0, buffer.length);
        input.reset();
        String header = new String(buffer, 0, read);
        return header.startsWith("AT&TFORM") && header.contains("DJVU");
    }

    @Override
    public String getId() {
        return "djvu";
    }

    @Override
    public String getName() {
        return "DjVu";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Import DjVu files");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.DJVU;
    }
}
