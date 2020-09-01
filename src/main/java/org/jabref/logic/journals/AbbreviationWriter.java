package org.jabref.logic.journals;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.commons.csv.CSVPrinter;

/**
 * This class provides handy static methods to save abbreviations to the file system.
 */
public final class AbbreviationWriter {

    private AbbreviationWriter() {
    }

    /**
     * This method will write the list of abbreviations to a file on the file system specified by the given path. If the
     * file already exists its content will be overridden, otherwise a new file will be created.
     *
     * @param path          to a file (doesn't have to exist just yet)
     * @param abbreviations as a list specifying which entries should be written
     */
    public static void writeOrCreate(Path path, List<Abbreviation> abbreviations, Charset encoding) throws IOException {
        try (OutputStreamWriter writer = new OutputStreamWriter(Files.newOutputStream(path), encoding);
             CSVPrinter csvPrinter = new CSVPrinter(writer, AbbreviationFormat.getCSVFormat())) {
            for (Abbreviation entry : abbreviations) {
                if (entry.getShortestUniqueAbbreviation().isEmpty()) {
                    csvPrinter.printRecord(entry.getName(), entry.getAbbreviation());
                } else {
                    csvPrinter.printRecord(entry.getName(), entry.getAbbreviation(), entry.getShortestUniqueAbbreviation());
                }
            }
        }
    }
}
