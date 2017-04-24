package org.jabref.logic.journals;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.OS;

/**
 * This class provides handy static methodes to save abbreviations to the file system.
 */
public class AbbreviationWriter {

    private AbbreviationWriter() {
    }

    /**
     * This method will write the list of abbreviations to a file on the file system specified by the given path.
     * If the file already exists its content will be overridden, otherwise a new file will be created.
     *
     * @param path to a file (doesn't have to exist just yet)
     * @param abbreviations as a list specifying which entries should be written
     * @throws IOException
     */
    public static void writeOrCreate(Path path, List<Abbreviation> abbreviations, Charset encoding) throws IOException {
        try (OutputStream outStream = Files.newOutputStream(path);
                OutputStreamWriter writer = new OutputStreamWriter(outStream, encoding)) {
            for (Abbreviation entry : abbreviations) {
                writer.write(entry.getName());
                writer.write(" = ");
                writer.write(entry.getAbbreviation());
                writer.write(OS.NEWLINE);
            }
        }

    }

}
