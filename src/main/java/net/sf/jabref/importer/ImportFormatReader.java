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
package net.sf.jabref.importer;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import net.sf.jabref.importer.fileformat.*;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.database.BibDatabases;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;

public class ImportFormatReader {

    public static final String BIBTEX_FORMAT = "BibTeX";

    /**
     * all import formats, in the default order of import formats
     */
    private final SortedSet<ImportFormat> formats = new TreeSet<>();

    private static final Log LOGGER = LogFactory.getLog(ImportFormatReader.class);


    public void resetImportFormats() {
        formats.clear();

        formats.add(new BiblioscapeImporter());
        formats.add(new BibtexImporter());
        formats.add(new BibTeXMLImporter());
        formats.add(new CopacImporter());
        formats.add(new EndnoteImporter());
        formats.add(new FreeCiteImporter());
        formats.add(new InspecImporter());
        formats.add(new IsiImporter());
        formats.add(new MedlineImporter());
        formats.add(new MedlinePlainImporter());
        formats.add(new MsBibImporter());
        formats.add(new OvidImporter());
        formats.add(new PdfContentImporter());
        formats.add(new PdfXmpImporter());
        formats.add(new RepecNepImporter());
        formats.add(new RisImporter());
        formats.add(new SilverPlatterImporter());

        /**
         * Get custom import formats
         */
        for (CustomImporter importer : Globals.prefs.customImports) {
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
            if (format.getCLIId().equals(cliId)) {
                return Optional.of(format);
            }
        }
        return Optional.empty();
    }

    public List<BibEntry> importFromFile(String format, String filename, OutputPrinter status)
            throws IOException {
        Optional<ImportFormat> importer = getByCliId(format);

        if (!importer.isPresent()) {
            throw new IllegalArgumentException("Unknown import format: " + format);
        }

        return importFromFile(importer.get(), filename, status);
    }

    public List<BibEntry> importFromFile(ImportFormat importer, String filename, OutputPrinter status) throws IOException {
        Objects.requireNonNull(importer);
        Objects.requireNonNull(filename);
        File file = new File(filename);

        try (InputStream stream = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(stream)) {

            bis.mark(Integer.MAX_VALUE);

            if (!importer.isRecognizedFormat(bis)) {
                throw new IOException("Wrong file format");
            }
        }

        try (InputStream stream = new FileInputStream(file);
                BufferedInputStream bis = new BufferedInputStream(stream)) {
            return importer.importEntries(bis, status);
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
            sb.append(imFo.getCLIId());
            sb.append('\n');
        }

        return sb.toString();
    }



    public static InputStreamReader getUTF8Reader(File f) throws IOException {
        return getReader(f, StandardCharsets.UTF_8);
    }

    public static InputStreamReader getUTF16Reader(File f) throws IOException {
        return getReader(f, StandardCharsets.UTF_16);
    }

    public static InputStreamReader getReader(File f, Charset charset)
            throws IOException {
        return new InputStreamReader(new FileInputStream(f), charset);
    }

    public static Reader getReaderDefaultEncoding(InputStream in) {
        InputStreamReader reader;
        reader = new InputStreamReader(in, Globals.prefs.getDefaultEncoding());

        return reader;
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
     * @throws IOException
     */
    public UnknownFormatImport importUnknownFormat(String filename) {
        Objects.requireNonNull(filename);

        // First, see if it is a BibTeX file:
        try {
            ParserResult pr = OpenDatabaseAction.loadDatabase(new File(filename),
                    Globals.prefs.getDefaultEncoding());
            if (pr.getDatabase().hasEntries() || !pr.getDatabase().hasNoStrings()) {
                pr.setFile(new File(filename));
                return new UnknownFormatImport(ImportFormatReader.BIBTEX_FORMAT, pr);
            }
        } catch (IOException ignore) {
            // Ignored
        }

        // we don't use a provided OutputPrinter (such as the JabRef frame),
        // as we don't want to see any outputs from failed importers:
        // we expect failures and do not want to report them to the user
        OutputPrinterToNull nullOutput = new OutputPrinterToNull();

        // stores ref to best result, gets updated at the next loop
        List<BibEntry> bestResult = null;
        int bestResultCount = 0;
        String bestFormatName = null;

        // Cycle through all importers:
        for (ImportFormat imFo : getImportFormats()) {

            try {

                List<BibEntry> entries = importFromFile(imFo, filename, nullOutput);

                int entryCount;
                BibDatabases.purgeEmptyEntries(entries);
                entryCount = entries.size();

                if (entryCount > bestResultCount) {
                    bestResult = entries;
                    bestResultCount = bestResult.size();
                    bestFormatName = imFo.getFormatName();
                }
            } catch (IOException ex) {
                // The import didn't succeed. Go on.
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
