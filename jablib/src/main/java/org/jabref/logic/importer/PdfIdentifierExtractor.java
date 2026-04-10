package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Extracts identifier field values (DOI, ISBN, ISSN, eprint) from a PDF file by parsing its XMP metadata and text content.
///
/// @implNote The supported fields must be consistent with {@link WebFetchers#getIdBasedFetcherForField(Field, ImportFormatPreferences)}.
public class PdfIdentifierExtractor {

    public static final List<Field> SUPPORTED_FIELDS = List.of(
                                                               StandardField.EPRINT,
                                                               StandardField.DOI,
                                                               StandardField.ISBN,
                                                               StandardField.ISSN);

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfIdentifierExtractor.class);

    private final XmpPreferences xmpPreferences;

    public PdfIdentifierExtractor(XmpPreferences xmpPreferences) {
        this.xmpPreferences = xmpPreferences;
    }

    /// Parses the given PDF and returns a map of identifier field to value for every supported identifier found in the file. The first non-empty value found per field wins (XMP metadata takes priority over content).
    public Map<Field, String> extract(Path filePath) {
        return extractIdentifiers(parsePdfForEntries(filePath));
    }

    /// Extracts identifier field values from a list of already-parsed entries. Use this overload when the entries have already been obtained by other importers to avoid re-parsing the PDF.
    public Map<Field, String> extract(List<BibEntry> entries) {
        return extractIdentifiers(entries);
    }

    private List<BibEntry> parsePdfForEntries(Path filePath) {
        List<BibEntry> entries = new ArrayList<>();
        List<Importer> importers = List.of(
                                           new PdfXmpImporter(xmpPreferences),
                                           new PdfContentImporter());
        for (Importer importer : importers) {
            try {
                ParserResult result = importer.importDatabase(filePath);
                if (!result.isInvalid() && !result.isEmpty() && result.getDatabase().hasEntries()) {
                    entries.add(result.getDatabase().getEntries().getFirst());
                }
            } catch (IOException e) {
                LOGGER.warn("Could not parse PDF for identifier lookup", e);
            }
        }
        return entries;
    }

    static Map<Field, String> extractIdentifiers(List<BibEntry> entries) {
        Map<Field, String> identifiers = new HashMap<>();

        for (BibEntry entry : entries) {
            for (Field field : SUPPORTED_FIELDS) {
                if (!identifiers.containsKey(field)) {
                    entry.getField(field)
                         .map(String::trim)
                         .filter(value -> !value.isEmpty())
                         .ifPresent(value -> identifiers.put(field, value));
                }
            }
        }

        return identifiers;
    }
}
