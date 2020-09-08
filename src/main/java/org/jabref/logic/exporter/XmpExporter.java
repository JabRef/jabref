package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;

/**
 * A custom exporter to write bib entries to a .xmp file for further processing
 * in other scenarios and applications. The xmp metadata are written in dublin
 * core format.
 */
public class XmpExporter extends Exporter {

    public static final String XMP_SPLIT_DIRECTORY_INDICATOR = "split";

    private final XmpPreferences xmpPreferences;

    public XmpExporter(XmpPreferences xmpPreferences) {
        super("xmp", "Plain XMP", StandardFileType.XMP);
        this.xmpPreferences = xmpPreferences;
    }

    /**
     * @param databaseContext the database to export from
     * @param file            the file to write to. If it contains "split", then the output is split into different files
     * @param encoding        the encoding to use
     * @param entries         a list containing all entries that should be exported
     */
    @Override
    public void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception {
        Objects.requireNonNull(databaseContext);
        Objects.requireNonNull(file);
        Objects.requireNonNull(entries);

        if (entries.isEmpty()) {
            return;
        }

        // This is a distinction between writing all entries from the supplied list to a single .xmp file,
        // or write every entry to a separate file.
        if (file.getFileName().toString().trim().equals(XMP_SPLIT_DIRECTORY_INDICATOR)) {
            for (BibEntry entry : entries) {
                // Avoid situations, where two citation keys are null
                Path entryFile;
                String suffix = entry.getId() + "_" + entry.getField(InternalField.KEY_FIELD).orElse("null") + ".xmp";
                if (file.getParent() == null) {
                    entryFile = Path.of(suffix);
                } else {
                    entryFile = Path.of(file.getParent().toString() + "/" + suffix);
                }
                this.writeBibToXmp(entryFile, Collections.singletonList(entry), encoding);
            }
        } else {
            this.writeBibToXmp(file, entries, encoding);
        }
    }

    private void writeBibToXmp(Path file, List<BibEntry> entries, Charset encoding) throws IOException {
        String xmpContent = XmpUtilWriter.generateXmpStringWithoutXmpDeclaration(entries, this.xmpPreferences);
        try (BufferedWriter writer = Files.newBufferedWriter(file, encoding)) {
            writer.write(xmpContent);
            writer.flush();
        }
    }
}
