package org.jabref.logic.exporter;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

public class CFFExporter extends Exporter {

    public CFFExporter() {
        super("cff", "CFF Exporter (YAML-based)", FileType.CFF);
    }

    @Override
    public void export(BibDatabaseContext databaseContext, Path file, List<BibEntry> entries) throws Exception {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file.toFile()))) {
            for (BibEntry entry : entries) {
                writer.write("---\n");
                /* writer.write("title: " + entry.getField("title").orElse("") + "\n"); */
                writer.write("authors:\n");
                List<String> authors = entry.getAuthors();
                for (String author : authors) {
                    writer.write("  - " + author + "\n");
                }
                // Add more fields as needed
                writer.write("...\n");
            }
        }
    }

    // You can override other methods if necessary, but for simplicity, we're using the default implementations.
}
