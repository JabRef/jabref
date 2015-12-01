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
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
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
        formats.add(new BibteXMLImporter());
        formats.add(new BiomailImporter());
        formats.add(new CopacImporter());
        formats.add(new CsaImporter());
        formats.add(new EndnoteImporter());
        formats.add(new FreeCiteImporter());
        formats.add(new InspecImporter());
        formats.add(new IsiImporter());
        formats.add(new JstorImporter());
        formats.add(new MedlineImporter());
        formats.add(new MedlinePlainImporter());
        formats.add(new MsBibImporter());
        formats.add(new OvidImporter());
        formats.add(new PdfContentImporter());
        formats.add(new PdfXmpImporter());
        formats.add(new RepecNepImporter());
        formats.add(new RisImporter());
        formats.add(new ScifinderImporter());
        formats.add(new SilverPlatterImporter());
        formats.add(new SixpackImporter());

        /**
         * Get custom import formats
         */
        for (CustomImportList.Importer importer : Globals.prefs.customImports) {
            try {
                ImportFormat imFo = importer.getInstance();
                formats.add(imFo);
            } catch (Exception e) {
                System.err.println("Could not instantiate " + importer.getName() + " importer, will ignore it. Please check if the class is still available.");
                e.printStackTrace();
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
    private ImportFormat getByCliId(String cliId) {
        for (ImportFormat format : formats) {
            if (format.getCLIId().equals(cliId)) {
                return format;
            }
        }
        return null;
    }

    public List<BibtexEntry> importFromStream(String format, InputStream in, OutputPrinter status)
            throws IOException {
        ImportFormat importer = getByCliId(format);

        if (importer == null) {
            throw new IllegalArgumentException("Unknown import format: " + format);
        }

        List<BibtexEntry> res = importer.importEntries(in, status);

        // Remove all empty entries
        if (res != null) {
            ImportFormatReader.purgeEmptyEntries(res);
        }

        return res;
    }

    public List<BibtexEntry> importFromFile(String format, String filename, OutputPrinter status)
            throws IOException {
        ImportFormat importer = getByCliId(format);

        if (importer == null) {
            throw new IllegalArgumentException("Unknown import format: " + format);
        }

        return importFromFile(importer, filename, status);
    }

    public List<BibtexEntry> importFromFile(ImportFormat importer, String filename, OutputPrinter status) throws IOException {
        File file = new File(filename);

        try (InputStream stream = new FileInputStream(file)) {

            if (!importer.isRecognizedFormat(stream)) {
                throw new IOException("Wrong file format");
            }

            return importer.importEntries(stream, status);
        }
    }

    public static BibtexDatabase createDatabase(Collection<BibtexEntry> bibentries) {
        ImportFormatReader.purgeEmptyEntries(bibentries);

        BibtexDatabase database = new BibtexDatabase();

        for (BibtexEntry entry : bibentries) {

            entry.setId(IdGenerator.next());
            database.insertEntry(entry);
        }

        return database;
    }

    /**
     * All custom importers.
     * <p>
     * <p>Elements are in default order.</p>
     *
     * @return all custom importers, elements are of type InputFormat
     */
    public SortedSet<ImportFormat> getCustomImportFormats() {
        SortedSet<ImportFormat> result = new TreeSet<>();
        for (ImportFormat format : formats) {
            if (format.getIsCustomImporter()) {
                result.add(format);
            }
        }
        return result;
    }

    /**
     * All built-in importers.
     * <p>
     * <p>Elements are in default order.</p>
     *
     * @return all custom importers, elements are of type InputFormat
     */
    public SortedSet<ImportFormat> getBuiltInInputFormats() {
        SortedSet<ImportFormat> result = new TreeSet<>();
        for (ImportFormat format : formats) {
            if (!format.getIsCustomImporter()) {
                result.add(format);
            }
        }
        return result;
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

            for (int j = 0; j < pad; j++) {
                sb.append(" ");
            }

            sb.append(" : ");
            sb.append(imFo.getCLIId());
            sb.append("\n");
        }

        return sb.toString(); //.substring(0, res.length()-1);
    }

    /**
     * Expand initials, e.g. EH Wissler -> E. H. Wissler or Wissler, EH -> Wissler, E. H.
     *
     * @param name
     * @return The name after expanding initials.
     */
    public static String expandAuthorInitials(String name) {
        String[] authors = name.split(" and ");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < authors.length; i++) {
            if (authors[i].contains(", ")) {
                String[] names = authors[i].split(", ");
                if (names.length > 0) {
                    sb.append(names[0]);
                    if (names.length > 1) {
                        sb.append(", ");
                    }
                }
                for (int j = 1; j < names.length; j++) {
                    if (j == 1) {
                        sb.append(ImportFormatReader.expandAll(names[j]));
                    } else {
                        sb.append(names[j]);
                    }
                    if (j < (names.length - 1)) {
                        sb.append(", ");
                    }
                }

            } else {
                String[] names = authors[i].split(" ");
                if (names.length > 0) {
                    sb.append(ImportFormatReader.expandAll(names[0]));
                }
                for (int j = 1; j < names.length; j++) {
                    sb.append(" ");
                    sb.append(names[j]);
                }
            }
            if (i < (authors.length - 1)) {
                sb.append(" and ");
            }
        }

        return sb.toString().trim();
    }

    //------------------------------------------------------------------------------

    private static String expandAll(String s) {
        //System.out.println("'"+s+"'");
        // Avoid arrayindexoutof.... :
        if (s.isEmpty()) {
            return s;
        }
        // If only one character (uppercase letter), add a dot and return immediately:
        if ((s.length() == 1) && Character.isLetter(s.charAt(0)) &&
                Character.isUpperCase(s.charAt(0))) {
            return s + ".";
        }
        StringBuilder sb = new StringBuilder();
        char c = s.charAt(0);
        char d = 0;
        for (int i = 1; i < s.length(); i++) {
            d = s.charAt(i);
            if (Character.isLetter(c) && Character.isUpperCase(c) &&
                    Character.isLetter(d) && Character.isUpperCase(d)) {
                sb.append(c);
                sb.append(". ");
            } else {
                sb.append(c);
            }
            c = d;
        }
        if (Character.isLetter(c) && Character.isUpperCase(c) &&
                Character.isLetter(d) && Character.isUpperCase(d)) {
            sb.append(c);
            sb.append(". ");
        } else {
            sb.append(c);
        }
        return sb.toString().trim();
    }

    static File checkAndCreateFile(String filename) {
        File f = new File(filename);

        if (!f.exists() && !f.canRead() && !f.isFile()) {

            LOGGER.info("Error " + filename + " is not a valid file and|or is not readable.");
            return null;

        } else {
            return f;
        }
    }

    //==================================================
    // Set a field, unless the string to set is empty.
    //==================================================
    public static void setIfNecessary(BibtexEntry be, String field, String content) {
        if (!"".equals(content)) {
            be.setField(field, content);
        }
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

    public static Reader getReaderDefaultEncoding(InputStream in)
            throws IOException {
        InputStreamReader reader;
        reader = new InputStreamReader(in, Globals.prefs.getDefaultEncoding());

        return reader;
    }

    /**
     * Receives an ArrayList of BibtexEntry instances, iterates through them, and
     * removes all entries that have no fields set. This is useful for rooting out
     * an unsucessful import (wrong format) that returns a number of empty entries.
     */
    private static void purgeEmptyEntries(Collection<BibtexEntry> entries) {
        for (Iterator<BibtexEntry> i = entries.iterator(); i.hasNext(); ) {
            BibtexEntry entry = i.next();

            // If there are no fields, remove the entry:
            if (entry.getFieldNames().isEmpty()) {
                i.remove();
            }
        }
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
     * <p>
     * If all fails this method attempts to read this file as bibtex.
     *
     * @throws IOException
     */
    public UnknownFormatImport importUnknownFormat(String filename) {

        // we don't use a provided OutputPrinter (such as the JabRef frame),
        // as we don't want to see any outputs from failed importers:
        // we expect failures and do not want to report them to the user
        OutputPrinterToNull nullOutput = new OutputPrinterToNull();

        // stores ref to best result, gets updated at the next loop
        List<BibtexEntry> bestResult = null;
        int bestResultCount = 0;
        String bestFormatName = null;

        // Cycle through all importers:
        for (ImportFormat imFo : getImportFormats()) {

            try {

                List<BibtexEntry> entries = importFromFile(imFo, filename, nullOutput);

                int entryCount;
                if (entries == null) {
                    entryCount = 0;
                } else {
                    ImportFormatReader.purgeEmptyEntries(entries);
                    entryCount = entries.size();
                }

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

        // Finally, if all else fails, see if it is a BibTeX file:
        try {
            ParserResult pr = OpenDatabaseAction.loadDatabase(new File(filename),
 Globals.prefs.getDefaultEncoding());
            if ((pr.getDatabase().getEntryCount() > 0)
                    || (pr.getDatabase().getStringCount() > 0)) {
                pr.setFile(new File(filename));
                return new UnknownFormatImport(ImportFormatReader.BIBTEX_FORMAT, pr);
            }
        } catch (Throwable ex) {
            return null;
        }

        return null;
    }
}
