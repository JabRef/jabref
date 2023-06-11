package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

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
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.PdfMergeMetadataImporter;
import org.jabref.logic.importer.fileformat.PdfVerbatimBibTextImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.importer.fileformat.SilverPlatterImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

public class ImportFormatReader {

    public static final String BIBTEX_FORMAT = "BibTeX";

    /**
     * All import formats.
     * Sorted accordingly to {@link Importer#compareTo}, which defaults to alphabetically by the name
     */
    private final List<Importer> formats = new ArrayList<>();

    private final ImporterPreferences importerPreferences;
    private final ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileUpdateMonitor;

    public ImportFormatReader(ImporterPreferences importerPreferences,
                              ImportFormatPreferences importFormatPreferences,
                              FileUpdateMonitor fileUpdateMonitor) {
        this.importerPreferences = importerPreferences;
        this.importFormatPreferences = importFormatPreferences;
        this.fileUpdateMonitor = fileUpdateMonitor;
        reset();
    }

    public void reset() {
        formats.add(new CopacImporter());
        formats.add(new EndnoteImporter(importFormatPreferences));
        formats.add(new EndnoteXmlImporter(importFormatPreferences));
        formats.add(new InspecImporter());
        formats.add(new IsiImporter());
        formats.add(new MedlineImporter());
        formats.add(new MedlinePlainImporter());
        formats.add(new ModsImporter(importFormatPreferences));
        formats.add(new MsBibImporter());
        formats.add(new OvidImporter());
        formats.add(new PdfMergeMetadataImporter(importFormatPreferences));
        formats.add(new PdfVerbatimBibTextImporter(importFormatPreferences));
        formats.add(new PdfContentImporter(importFormatPreferences));
        formats.add(new PdfEmbeddedBibFileImporter(importFormatPreferences));
        if (importFormatPreferences.grobidPreferences().isGrobidEnabled()) {
            formats.add(new PdfGrobidImporter(importFormatPreferences));
        }
        formats.add(new PdfXmpImporter(importFormatPreferences.xmpPreferences()));
        formats.add(new RepecNepImporter(importFormatPreferences));
        formats.add(new RisImporter());
        formats.add(new SilverPlatterImporter());
        formats.add(new CffImporter());
        formats.add(new BiblioscapeImporter());
        formats.add(new BibtexImporter(importFormatPreferences, fileUpdateMonitor));
        formats.add(new CitaviXmlImporter());

        // Get custom import formats
        formats.addAll(importerPreferences.getCustomImporters());
    }

    /**
     * Format for a given CLI-ID.
     * <p>
     * <p>Will return the first format according to the default-order of
     * format that matches the given ID.</p>
     *
     * @param cliId CLI-Id
     * @return Import Format or <code>null</code> if none matches
     */
    private Optional<Importer> getByCliId(String cliId) {
        for (Importer format : formats) {
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

    /**
     * All importers.
     *
     * Elements are sorted by name.
     * </p>
     *
     * @return all custom importers, elements are of type InputFormat
     */
    public SortedSet<Importer> getImportFormats() {
        return new TreeSet<>(this.formats);
    }

    public record UnknownFormatImport(String format, ParserResult parserResult) {
    }

    /**
     * Tries to import a file by iterating through the available import filters,
     * and keeping the import that seems most promising.
     * <p/>
     * This method first attempts to read this file as bibtex.
     *
     * @throws ImportException if the import fails (for example, if no suitable importer is found)
     */
    public UnknownFormatImport importUnknownFormat(Path filePath, FileUpdateMonitor fileMonitor) throws ImportException {
        Objects.requireNonNull(filePath);

        try {
            UnknownFormatImport unknownFormatImport = importUnknownFormat(importer -> importer.importDatabase(filePath), importer -> importer.isRecognizedFormat(filePath));
            unknownFormatImport.parserResult.setPath(filePath);
            return unknownFormatImport;
        } catch (ImportException e) {
            // If all importers fail, try to read the file as BibTeX
            try {
                ParserResult parserResult = OpenDatabase.loadDatabase(filePath, importFormatPreferences, fileMonitor);
                if (parserResult.getDatabase().hasEntries() || !parserResult.getDatabase().hasNoStrings()) {
                    parserResult.setPath(filePath);
                    return new UnknownFormatImport(ImportFormatReader.BIBTEX_FORMAT, parserResult);
                } else {
                    throw new ImportException(parserResult.getErrorMessage());
                }
            } catch (IOException ignore) {
                // Ignored
                throw new ImportException(Localization.lang("Could not find a suitable import format."));
            }
        }
    }

    /**
     * Tries to import entries by iterating through the available import filters,
     * and keeping the import that seems the most promising
     *
     * @param importDatabase     the function to import the entries with a formatter
     * @param isRecognizedFormat the function to check whether the source is in the correct format for an importer
     * @return an UnknownFormatImport with the imported entries and metadata
     * @throws ImportException if the import fails (for example, if no suitable importer is found)
     */
    private UnknownFormatImport importUnknownFormat(CheckedFunction<Importer, ParserResult> importDatabase, CheckedFunction<Importer, Boolean> isRecognizedFormat) throws ImportException {
        // stores ref to best result, gets updated at the next loop
        List<BibEntry> bestResult = null;
        int bestResultCount = 0;
        String bestFormatName = null;

        // Cycle through all importers:
        for (Importer imFo : formats) {
            try {
                if (!isRecognizedFormat.apply(imFo)) {
                    continue;
                }

                ParserResult parserResult = importDatabase.apply(imFo);
                List<BibEntry> entries = parserResult.getDatabase().getEntries();

                BibDatabases.purgeEmptyEntries(entries);
                int entryCount = entries.size();

                if (entryCount > bestResultCount) {
                    bestResult = entries;
                    bestResultCount = entryCount;
                    bestFormatName = imFo.getName();
                }
            } catch (IOException ex) {
                // The import did not succeed. Go on.
            }
        }

        if (bestResult != null) {
            // we found something
            ParserResult parserResult = new ParserResult(bestResult);
            return new UnknownFormatImport(bestFormatName, parserResult);
        }

        throw new ImportException(Localization.lang("Could not find a suitable import format."));
    }

    @FunctionalInterface
    public interface CheckedFunction<T, R> {

        R apply(T t) throws IOException;
    }

    /**
     * Tries to import a String by iterating through the available import filters,
     * and keeping the import that seems the most promising
     *
     * @param data the string to import
     * @return an UnknownFormatImport with the imported entries and metadata
     * @throws ImportException if the import fails (for example, if no suitable importer is found)
     */
    public UnknownFormatImport importUnknownFormat(String data) throws ImportException {
        Objects.requireNonNull(data);

        return importUnknownFormat(importer -> importer.importDatabase(data), importer -> importer.isRecognizedFormat(data));
    }
}
