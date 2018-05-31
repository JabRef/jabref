package org.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import org.jabref.logic.importer.fileformat.BibTeXMLImporter;
import org.jabref.logic.importer.fileformat.BiblioscapeImporter;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.logic.importer.fileformat.CopacImporter;
import org.jabref.logic.importer.fileformat.CustomImporter;
import org.jabref.logic.importer.fileformat.EndnoteImporter;
import org.jabref.logic.importer.fileformat.EndnoteXmlImporter;
import org.jabref.logic.importer.fileformat.FreeCiteImporter;
import org.jabref.logic.importer.fileformat.InspecImporter;
import org.jabref.logic.importer.fileformat.IsiImporter;
import org.jabref.logic.importer.fileformat.MedlineImporter;
import org.jabref.logic.importer.fileformat.MedlinePlainImporter;
import org.jabref.logic.importer.fileformat.ModsImporter;
import org.jabref.logic.importer.fileformat.MsBibImporter;
import org.jabref.logic.importer.fileformat.OvidImporter;
import org.jabref.logic.importer.fileformat.PdfContentImporter;
import org.jabref.logic.importer.fileformat.PdfXmpImporter;
import org.jabref.logic.importer.fileformat.RepecNepImporter;
import org.jabref.logic.importer.fileformat.RisImporter;
import org.jabref.logic.importer.fileformat.SilverPlatterImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabases;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

public class ImportFormatReader {

    public static final String BIBTEX_FORMAT = "BibTeX";

    /**
     * All import formats.
     * Sorted accordingly to {@link Importer#compareTo}, which defaults to alphabetically by the name
     */
    private final SortedSet<Importer> formats = new TreeSet<>();

    private ImportFormatPreferences importFormatPreferences;

    public void resetImportFormats(ImportFormatPreferences newImportFormatPreferences, XmpPreferences xmpPreferences, FileUpdateMonitor fileMonitor) {
        this.importFormatPreferences = newImportFormatPreferences;

        formats.clear();

        formats.add(new BiblioscapeImporter());
        formats.add(new BibtexImporter(importFormatPreferences, fileMonitor));
        formats.add(new BibTeXMLImporter());
        formats.add(new CopacImporter());
        formats.add(new EndnoteImporter(importFormatPreferences));
        formats.add(new EndnoteXmlImporter(importFormatPreferences));
        formats.add(new FreeCiteImporter(importFormatPreferences));
        formats.add(new InspecImporter());
        formats.add(new IsiImporter());
        formats.add(new MedlineImporter());
        formats.add(new MedlinePlainImporter());
        formats.add(new ModsImporter(importFormatPreferences));
        formats.add(new MsBibImporter());
        formats.add(new OvidImporter());
        formats.add(new PdfContentImporter(importFormatPreferences));
        formats.add(new PdfXmpImporter(xmpPreferences));
        formats.add(new RepecNepImporter(importFormatPreferences));
        formats.add(new RisImporter());
        formats.add(new SilverPlatterImporter());

        // Get custom import formats
        for (CustomImporter importer : importFormatPreferences.getCustomImportList()) {
            formats.add(importer);
        }
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

        if (!importer.isPresent()) {
            throw new ImportException(Localization.lang("Unknown import format") + ": " + format);
        }

        try {
            return importer.get().importDatabase(file, importFormatPreferences.getEncoding());
        } catch (IOException e) {
            throw new ImportException(e);
        }
    }

    /**
     * All importers.
     * <p>
     * <p>
     * Elements are in default order.
     * </p>
     *
     * @return all custom importers, elements are of type InputFormat
     */
    public SortedSet<Importer> getImportFormats() {
        return this.formats;
    }

    /**
     * Human readable list of all known import formats (name and CLI Id).
     * <p>
     * <p>List is in default-order.</p>
     *
     * @return human readable list of all known import formats
     */
    public String getImportFormatList() {
        StringBuilder sb = new StringBuilder();

        for (Importer imFo : formats) {
            int pad = Math.max(0, 14 - imFo.getName().length());
            sb.append("  ");
            sb.append(imFo.getName());

            sb.append(StringUtil.repeatSpaces(pad));

            sb.append(" : ");
            sb.append(imFo.getId());
            sb.append('\n');
        }

        return sb.toString();
    }

    public static class UnknownFormatImport {

        public final String format;
        public final ParserResult parserResult;

        public UnknownFormatImport(String format, ParserResult parserResult) {
            this.format = format;
            this.parserResult = parserResult;
        }
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

        // First, see if it is a BibTeX file:
        try {
            ParserResult parserResult = OpenDatabase.loadDatabase(filePath.toFile(), importFormatPreferences, fileMonitor);
            if (parserResult.getDatabase().hasEntries() || !parserResult.getDatabase().hasNoStrings()) {
                parserResult.setFile(filePath.toFile());
                return new UnknownFormatImport(ImportFormatReader.BIBTEX_FORMAT, parserResult);
            }
        } catch (IOException ignore) {
            // Ignored
        }

        UnknownFormatImport unknownFormatImport = importUnknownFormat(importer -> importer.importDatabase(filePath, importFormatPreferences.getEncoding()), importer -> importer.isRecognizedFormat(filePath, importFormatPreferences.getEncoding()));
        unknownFormatImport.parserResult.setFile(filePath.toFile());
        return unknownFormatImport;
    }

    /**
     * Tries to import entries by iterating through the available import filters,
     * and keeping the import that seems the most promising
     *
     * @param importDatabase the function to import the entries with a formatter
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
        for (Importer imFo : getImportFormats()) {
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
