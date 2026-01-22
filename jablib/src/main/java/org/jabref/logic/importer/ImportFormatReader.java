package org.jabref.logic.importer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.importer.fileformat.BiblioscapeImporter;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.CffImporter;
import org.jabref.logic.importer.fileformat.CitaviXmlImporter;
import org.jabref.logic.importer.fileformat.CopacImporter;
import org.jabref.logic.importer.fileformat.EndnoteImporter;
import org.jabref.logic.importer.fileformat.EndnoteXmlImporter;
import org.jabref.logic.importer.fileformat.InspecImporter;
import org.jabref.logic.importer.fileformat.IsiImporter;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.importer.fileformat.MedlinePlainImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.importer.fileformat.MsBibImporter;
import org.jabref.logic.importer.fileformat.OvidImporter;
import org.jabref.logic.importer.fileformat.ReferImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class ImportFormatReader {
    public static final String BIBTEX_FORMAT = "BibTeX";

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportFormatReader.class);

    /// All import formats.
    /// Sorted accordingly to {@link Importer#compareTo}, which defaults to alphabetically by the name
    private final List<Importer> importers = new ArrayList<>(30);

    private final ImporterPreferences importerPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final CitationKeyPatternPreferences citationKeyPatternPreferences;
    private BibtexImporter bibtexImporter;

    public ImportFormatReader(ImporterPreferences importerPreferences,
                              ImportFormatPreferences importFormatPreferences,
                              CitationKeyPatternPreferences citationKeyPatternPreferences,
                              FileUpdateMonitor fileUpdateMonitor) {
        this.importerPreferences = importerPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
        reset();
    }

    public void reset() {
        importers.add(new CopacImporter());
        importers.add(new EndnoteImporter());
        importers.add(new EndnoteXmlImporter(importFormatPreferences));
        importers.add(new InspecImporter());
        importers.add(new IsiImporter());
        importers.add(new MedlineImporter());
        importers.add(new MedlinePlainImporter(importFormatPreferences));
        importers.add(new ModsImporter(importFormatPreferences));
        importers.add(new MsBibImporter());
        importers.add(new OvidImporter());
        importers.add(new PdfMergeMetadataImporter(importFormatPreferences));
        importers.add(new PdfVerbatimBibtexImporter(importFormatPreferences));
        importers.add(new PdfContentImporter());
        importers.add(new PdfEmbeddedBibFileImporter(importFormatPreferences));
        if (importFormatPreferences.grobidPreferences().isGrobidEnabled()) {
            importers.add(new PdfGrobidImporter(importFormatPreferences));
        }
        importers.add(new PdfXmpImporter(importFormatPreferences.xmpPreferences()));
        importers.add(new RepecNepImporter(importFormatPreferences));
        importers.add(new ReferImporter());
        importers.add(new RisImporter());
        importers.add(new CffImporter(citationKeyPatternPreferences));
        importers.add(new BiblioscapeImporter());
        importers.add(new CitaviXmlImporter());

        // Get user-selected imports
        importers.addAll(importerPreferences.getCustomImporters());

        // BibTeX as last
        bibtexImporter = new BibtexImporter(importFormatPreferences, fileUpdateMonitor);
        importers.add(bibtexImporter);
    }

    /// Format for a given CLI-ID.
    ///
    /// Will return the first format according to the default-order of
    /// format that matches the given ID.
    ///
    /// @param cliId CLI-Id
    /// @return Import Format or `null` if none matches
    private Optional<Importer> getByCliId(String cliId) {
        for (Importer format : importers) {
            if (format.getId().equals(cliId)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    public ParserResult importFromFile(String format, Path file) throws ImportException {
        Optional<Importer> importer = getByCliId(format);

        if (importer.isEmpty()) {
            throw new ImportException(Localization.lang("Unknown import format") + ": " + format);
        }

        try {
            return importer.get().importDatabase(file);
        } catch (IOException e) {
            throw new ImportException(e);
        }
    }

    /// @return All importers, elements are sorted by name
    public SortedSet<Importer> getImporters() {
        return new TreeSet<>(this.importers);
    }

    /// @param format       The name of the format used
    /// @param parserResult The resulting data
    public record ImportResult(String format, ParserResult parserResult) {
    }

    /// Tries to import a file by iterating through the available import filters,
    /// and keeping the import that seems most promising.
    ///
    /// This method last attempts to read this file as bibtex.
    ///
    /// @throws ImportException if the import fails (for example, if no suitable importer is found)
    public ImportResult importWithAutoDetection(Path filePath) throws ImportException {
        ImportResult importResult = importWithAutoDetection(
                importer -> importer.importDatabase(filePath),
                importer -> importer.isRecognizedFormat(filePath),
                () -> OpenDatabase.loadDatabase(filePath, importFormatPreferences, fileUpdateMonitor)
        );
        importResult.parserResult.setPath(filePath);
        return importResult;
    }

    /// Tries to import a stream by iterating through the available import filters,
    /// and keeping the import that seems most promising.
    ///
    /// @throws ImportException if the import fails (for example, if no suitable importer is found)
    public ImportResult importWithAutoDetection(Reader reader) throws ImportException {
        // We try out multiple readers - therefore, streaming does not help to save resources
        Path tempFile;
        try {
            tempFile = Files.createTempFile("JabRef-import", "data");
        } catch (IOException e) {
            throw new ImportException("Could not create temp file", e);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
            reader.transferTo(writer);
        } catch (IOException e) {
            throw new ImportException("Could not add content to temp file", e);
        }

        ImportResult importResult = importWithAutoDetection(tempFile);
        // There is no explicit path at this point - but the called method to Path import set one --> reset
        importResult.parserResult().setPath(null);
        return importResult;
    }

    /// Tries to import a String by iterating through the available import filters,
    /// and keeping the import that seems the most promising
    ///
    /// @param data the string to import
    /// @return an UnknownFormatImport with the imported entries and metadata
    /// @throws ImportException if the import fails (for example, if no suitable importer is found)
    public ImportResult importWithAutoDetection(@NonNull String data) throws ImportException {
        return importWithAutoDetection(
                importer -> importer.importDatabase(data),
                importer -> importer.isRecognizedFormat(data),
                () -> bibtexImporter.importDatabase(data));
    }

    /// Tries to import entries by iterating through the available import filters,
    /// and keeping the import that seems the most promising
    ///
    /// The implementation idea is to be independent of BufferedInputStream vs. File.
    /// Therefore, functions are passed.
    ///
    /// @param importDatabase     the function to import the entries with a formatter
    /// @param isRecognizedFormat the function to check whether the source is in the correct format for an importer
    /// @param importUsingBibtex  used as fallback when the importers did not match
    /// @return an UnknownFormatImport with the imported entries and metadata
    /// @throws ImportException if the import fails (for example, if no suitable importer is found)
    private ImportResult importWithAutoDetection(
            CheckedFunction<Importer, ParserResult> importDatabase,
            CheckedFunction<Importer, Boolean> isRecognizedFormat,
            CheckedSupplier<ParserResult> importUsingBibtex) throws ImportException {
        // stores ref to best result, gets updated at the next loop
        List<BibEntry> bestResult = null;
        int bestResultFieldCount = 0;
        String bestFormatName = null;

        // Cycle through all importers:
        for (Importer importer : importers) {
            if (importer == bibtexImporter) {
                // BibTeX is different enough from other formats.
                // If an importer found something, this is used.
                // Below, we try BibTeX as fallback - if all other importers found nothing
                continue;
            }
            try {
                if (!isRecognizedFormat.apply(importer) || importer instanceof ReferImporter) {
                    // Refer/BibIX should be explicitly chosen by user // TODO: Why - introduced at PR #13118
                    continue;
                }
                ParserResult parserResult = importDatabase.apply(importer);
                List<BibEntry> entries = parserResult.getDatabase().getEntries();
                BibDatabases.purgeEmptyEntries(entries);

                // Sometimes, an importer detects garbage as valid entries. Thus, a simple count of entries is not sufficient.
                // Instead, we count the number of fields in all entries as heuristic.
                // Alternatively, we could also consider the number of characters in all entries.
                int fieldCount = entries.stream().mapToInt(entry -> entry.getFields().size()).sum();

                if (fieldCount > bestResultFieldCount) {
                    bestResult = entries;
                    bestResultFieldCount = fieldCount;
                    bestFormatName = importer.getName();
                }
            } catch (Throwable ex) {
                // We also want to catch NPEs and continue
                LOGGER.trace("Exception during import. Trying next importer.", ex);
                // The import did not succeed. Go on.
            }
        }

        if (bestResult != null) {
            // we found something
            ParserResult parserResult = new ParserResult(bestResult);
            return new ImportResult(bestFormatName, parserResult);
        }

        // If all other importers fail, try to read the file as BibTeX
        try {
            ParserResult parserResult = importUsingBibtex.apply();
            if (parserResult.getDatabase().hasEntries() || !parserResult.getDatabase().hasNoStrings()) {
                return new ImportResult(ImportFormatReader.BIBTEX_FORMAT, parserResult);
            } else {
                throw new ImportException(parserResult.getErrorMessage());
            }
        } catch (IOException ignore) {
            throw new ImportException(Localization.lang("Could not find a suitable import format."));
        }
    }

    @FunctionalInterface
    public interface CheckedSupplier<R> {
        R apply() throws IOException;
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T t) throws IOException;
    }
}
