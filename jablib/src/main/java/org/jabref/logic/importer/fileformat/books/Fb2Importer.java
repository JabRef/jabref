package org.jabref.logic.importer.fileformat.books;

import java.io.BufferedReader;
import java.io.IOException;

import org.jabref.logic.importer.TikaImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

public class Fb2Importer extends TikaImporter {
    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        return input.lines()
                .map(String::trim)
                .anyMatch(line -> line.startsWith("<?xml")
                        && line.contains("FictionBook")
                        && line.contains("http://www.gribuser.ru/xml/fictionbook/2.0"));
    }

    @Override
    public String getId() {
        return "fb2";
    }

    @Override
    public String getName() {
        return "FB2";
    }

    @Override
    public String getDescription() {
        return Localization.lang("Importer for Fiction Books (FB2) files");
    }

    @Override
    public FileType getFileType() {
        return StandardFileType.FB2;
    }
}
