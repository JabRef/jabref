package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

import net.sf.jabref.logic.importer.fileformat.BibTeXMLImporter;
import net.sf.jabref.logic.importer.fileformat.BiblioscapeImporter;
import net.sf.jabref.logic.importer.fileformat.BibtexImporter;
import net.sf.jabref.logic.importer.fileformat.CopacImporter;
import net.sf.jabref.logic.importer.fileformat.CustomImporter;
import net.sf.jabref.logic.importer.fileformat.EndnoteImporter;
import net.sf.jabref.logic.importer.fileformat.FreeCiteImporter;
import net.sf.jabref.logic.importer.fileformat.InspecImporter;
import net.sf.jabref.logic.importer.fileformat.IsiImporter;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.logic.importer.fileformat.MedlinePlainImporter;
import net.sf.jabref.logic.importer.fileformat.ModsImporter;
import net.sf.jabref.logic.importer.fileformat.MsBibImporter;
import net.sf.jabref.logic.importer.fileformat.OvidImporter;
import net.sf.jabref.logic.importer.fileformat.PdfContentImporter;
import net.sf.jabref.logic.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.logic.importer.fileformat.RepecNepImporter;
import net.sf.jabref.logic.importer.fileformat.RisImporter;
import net.sf.jabref.logic.importer.fileformat.SilverPlatterImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.xmp.XMPPreferences;
import net.sf.jabref.model.database.BibDatabases;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.strings.StringUtil;

public class ImportFormatReader {

    public static final String BIBTEX_FORMAT = "BibTeX";

    /**
     * All import formats.
     * Sorted accordingly to {@link Importer#compareTo}, which defaults to alphabetically by the name
     */
    private final SortedSet<Importer> formats = new TreeSet<>();

    private ImportFormatPreferences importFormatPreferences;


    public void resetImportFormats(ImportFormatPreferences newImportFormatPreferences, XMPPreferences xmpPreferences) {
        this.importFormatPreferences = newImportFormatPreferences;

        formats.clear();

        formats.add(new BiblioscapeImporter());
        formats.add(new BibtexImporter(importFormatPreferences));
        formats.add(new BibTeXMLImporter());
        formats.add(new CopacImporter());
        formats.add(new EndnoteImporter(importFormatPreferences));
        formats.add(new FreeCiteImporter(importFormatPreferences));
        formats.add(new InspecImporter());
        formats.add(new IsiImporter());
        formats.add(new MedlineImporter());
        formats.add(new MedlinePlainImporter());
        formats.add(new ModsImporter());
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
     * If all fails this method attempts to read this file as bibtex.
     *
     * @throws ImportException if the import fails (for example, if no suitable importer is found)
     */
    public UnknownFormatImport importUnknownFormat(Path filePath) throws ImportException {
        Objects.requireNonNull(filePath);

        // First, see if it is a BibTeX file:
        try {
            ParserResult parserResult = OpenDatabase.loadDatabase(filePath.toFile(), importFormatPreferences);
            if (parserResult.getDatabase().hasEntries() || !parserResult.getDatabase().hasNoStrings()) {
                parserResult.setFile(filePath.toFile());
                return new UnknownFormatImport(ImportFormatReader.BIBTEX_FORMAT, parserResult);
            }
        } catch (IOException ignore) {
            // Ignored
        }

        // stores ref to best result, gets updated at the next loop
        List<BibEntry> bestResult = null;
        int bestResultCount = 0;
        String bestFormatName = null;

        // Cycle through all importers:
        for (Importer imFo : getImportFormats()) {
            try {
                if (!imFo.isRecognizedFormat(filePath, importFormatPreferences.getEncoding())) {
                    continue;
                }

                ParserResult parserResult = imFo.importDatabase(filePath, importFormatPreferences.getEncoding());
                List<BibEntry> entries = parserResult.getDatabase().getEntries();

                BibDatabases.purgeEmptyEntries(entries);
                int entryCount = entries.size();

                if (entryCount > bestResultCount) {
                    bestResult = entries;
                    bestResultCount = bestResult.size();
                    bestFormatName = imFo.getName();
                }
            } catch (IOException ex) {
                // The import did not succeed. Go on.
            }
        }

        if (bestResult != null) {
            // we found something
            ParserResult parserResult = new ParserResult(bestResult);
            parserResult.setFile(filePath.toFile());
            return new UnknownFormatImport(bestFormatName, parserResult);
        }

        throw new ImportException(Localization.lang("Could not find a suitable import format."));
    }
}
