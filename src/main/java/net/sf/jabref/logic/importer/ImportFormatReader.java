/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.logic.importer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import net.sf.jabref.logic.importer.fileformat.ImportFormat;
import net.sf.jabref.logic.importer.fileformat.InspecImporter;
import net.sf.jabref.logic.importer.fileformat.IsiImporter;
import net.sf.jabref.logic.importer.fileformat.MedlineImporter;
import net.sf.jabref.logic.importer.fileformat.MedlinePlainImporter;
import net.sf.jabref.logic.importer.fileformat.MsBibImporter;
import net.sf.jabref.logic.importer.fileformat.OvidImporter;
import net.sf.jabref.logic.importer.fileformat.PdfContentImporter;
import net.sf.jabref.logic.importer.fileformat.PdfXmpImporter;
import net.sf.jabref.logic.importer.fileformat.RepecNepImporter;
import net.sf.jabref.logic.importer.fileformat.RisImporter;
import net.sf.jabref.logic.importer.fileformat.SilverPlatterImporter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabases;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ImportFormatReader {

    public static final String BIBTEX_FORMAT = "BibTeX";

    /**
     * All import formats.
     * Sorted accordingly to {@link ImportFormat#compareTo}, which defaults to alphabetically by the name
     */
    private final SortedSet<ImportFormat> formats = new TreeSet<>();

    private static final Log LOGGER = LogFactory.getLog(ImportFormatReader.class);

    private ImportFormatPreferences importFormatPreferences;


    public void resetImportFormats(ImportFormatPreferences newImportFormatPreferences) {
        this.importFormatPreferences = newImportFormatPreferences;

        formats.clear();

        formats.add(new BiblioscapeImporter());
        formats.add(new BibtexImporter(importFormatPreferences));
        formats.add(new BibTeXMLImporter());
        formats.add(new CopacImporter());
        formats.add(new EndnoteImporter());
        formats.add(new FreeCiteImporter(importFormatPreferences));
        formats.add(new InspecImporter());
        formats.add(new IsiImporter());
        formats.add(new MedlineImporter());
        formats.add(new MedlinePlainImporter());
        formats.add(new MsBibImporter());
        formats.add(new OvidImporter());
        formats.add(new PdfContentImporter(importFormatPreferences));
        formats.add(new PdfXmpImporter());
        formats.add(new RepecNepImporter(importFormatPreferences));
        formats.add(new RisImporter());
        formats.add(new SilverPlatterImporter());

        /**
         * Get custom import formats
         */
        for (CustomImporter importer : importFormatPreferences.getCustomImportList()) {
            try {
                ImportFormat imFo = importer.getInstance();
                formats.add(imFo);
            } catch (IOException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                LOGGER.error("Could not instantiate " + importer.getName()
                        + " importer, will ignore it. Please check if the class is still available.", e);
            }
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
    private Optional<ImportFormat> getByCliId(String cliId) {
        for (ImportFormat format : formats) {
            if (format.getId().equals(cliId)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    public ParserResult importFromFile(String format, Path file)
            throws IOException {
        Optional<ImportFormat> importer = getByCliId(format);

        if (!importer.isPresent()) {
            throw new IllegalArgumentException("Unknown import format: " + format);
        }

        return importer.get().importDatabase(file, importFormatPreferences.getEncoding());
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
    public SortedSet<ImportFormat> getImportFormats() {
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

        for (ImportFormat imFo : formats) {
            int pad = Math.max(0, 14 - imFo.getFormatName().length());
            sb.append("  ");
            sb.append(imFo.getFormatName());

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

    public UnknownFormatImport importUnknownFormat(String filename) {
        return importUnknownFormat(Paths.get(filename));
    }

    /**
     * Tries to import a file by iterating through the available import filters,
     * and keeping the import that seems most promising.
     * <p/>
     * If all fails this method attempts to read this file as bibtex.
     *
     * @throws IOException
     */
    public UnknownFormatImport importUnknownFormat(Path filePath) {
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
        for (ImportFormat imFo : getImportFormats()) {
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
                    bestFormatName = imFo.getFormatName();
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

        return null;
    }
}
