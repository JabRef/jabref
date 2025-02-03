package org.jabref.logic.importer.fileformat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.logic.util.StandardFileType;

import org.apache.commons.io.input.ReaderInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EpubImporter extends Importer {
    private static final Logger LOGGER = LoggerFactory.getLogger(EpubImporter.class);
    private static final char[] EPUB_HEADER_MAGIC_NUMBER = {0x50, 0x4b, 0x03, 0x04};

    @Override
    public boolean isRecognizedFormat(BufferedReader input) throws IOException {
        char[] header = new char[EPUB_HEADER_MAGIC_NUMBER.length];
        int nRead = input.read(header);
        return nRead == EPUB_HEADER_MAGIC_NUMBER.length && Arrays.equals(header, EPUB_HEADER_MAGIC_NUMBER);
    }

    @Override
    public ParserResult importDatabase(Path filePath) throws IOException {

    }

    @Override
    public ParserResult importDatabase(BufferedReader input) throws IOException {
        throw new UnsupportedOperationException("EpubImporter does not support importDatabase(BufferedReader reader). "
                + "Instead use importDatabase(Path filePath).");
    }

    @Override
    public String getId() {
        return "epub";
    }

    @Override
    public String getName() {
        return Localization.lang("ePUB Importer");
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
