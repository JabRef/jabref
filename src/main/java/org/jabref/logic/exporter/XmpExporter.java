package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import org.jabref.Globals;
import org.jabref.logic.util.FileType;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * A custom exporter to write bib entries to a .xmp file for further processing
 * in other scenarios and applications. The xmp metadata are written in dublin
 * core format.
 */
public class XmpExporter extends Exporter {

    public XmpExporter() {
        super("XmpBib", FileType.ENDNOTE_XMP.getDescription(), FileType.ENDNOTE_XMP);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(XmpUtilWriter.generateXmpString(entries, Globals.prefs.getXMPPreferences()));
            writer.flush();
        }
    }

}
