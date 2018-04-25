package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.util.FileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.logic.xmp.XmpUtilWriter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

/**
 * A custom exporter to write bib entries to a .xmp file for further processing
 * in other scenarios and applications. The xmp metadata are written in dublin
 * core format.
 */
public class XmpExporter extends Exporter {

    private static final String XMP_SPLIT_PATTERN = "split";

    private final XmpPreferences xmpPreferences;

    public XmpExporter(XmpPreferences xmpPreferences) {
        super("xmp", FileType.PLAIN_XMP.getDescription(), FileType.PLAIN_XMP);
        this.xmpPreferences = xmpPreferences;
    }

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
        if (file.getFileName().toString().trim().equals(XMP_SPLIT_PATTERN)) {

            for (BibEntry entry : entries) {
                // Avoid situations, where two cite keys are null
                Path entryFile;
                String suffix = entry.getId() + "_" + entry.getCiteKey() + ".xmp";
                if (file.getParent() == null) {
                    entryFile = Paths.get(suffix);
                } else {
                    entryFile = Paths.get(file.getParent().toString() + "/" + suffix);
                }

                this.writeBibToXmp(entryFile, Arrays.asList(entry), encoding);
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
