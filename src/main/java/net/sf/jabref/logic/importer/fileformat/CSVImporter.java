package net.sf.jabref.logic.importer.fileformat;

import net.sf.jabref.logic.importer.Importer;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.util.FileExtensions;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Importer para o formato CSV.
 */
public class CSVImporter extends Importer {
    private static final Pattern COPAC_PATTERN = Pattern.compile("^\\s*TI- ");

    @Override
    public String getName() {
        return "CSV";
    }

    @Override
    public FileExtensions getExtensions() {
        return FileExtensions.CSV;
    }

    @Override
    public String getId() {
        return "csv";
    }

    @Override
    public String getDescription() {
        return "Importer para o formato CSV.";
    }

    @Override
    public boolean isRecognizedFormat(BufferedReader reader) throws IOException {
        return true;
    }

    @Override
    public ParserResult importDatabase(BufferedReader reader) throws IOException {
        List<BibEntry> results = new LinkedList<>();
        return new ParserResult(results);
    }
}
