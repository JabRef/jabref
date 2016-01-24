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
package net.sf.jabref.exporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.bibtex.EntryTypes;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.model.entry.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.bibtex.comparator.BibtexStringComparator;
import net.sf.jabref.bibtex.comparator.CrossRefEntryComparator;
import net.sf.jabref.bibtex.comparator.FieldComparator;
import net.sf.jabref.bibtex.comparator.FieldComparatorStack;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.id.IdComparator;
import net.sf.jabref.model.database.BibDatabase;

public class FileActions {

    public enum DatabaseSaveType {
        DEFAULT, PLAIN_BIBTEX
    }


    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(#[A-Za-z]+#)"); // Used to detect string references in strings
    private static BibtexString.Type previousStringType;

    private static final Log LOGGER = LogFactory.getLog(FileActions.class);


    private static void writePreamble(Writer fw, String preamble) throws IOException {
        if (preamble != null) {
            fw.write("@PREAMBLE{");
            fw.write(preamble);
            fw.write('}' + Globals.NEWLINE + Globals.NEWLINE);
        }
    }

    /**
     * Write all strings in alphabetical order, modified to produce a safe (for
     * BibTeX) order of the strings if they reference each other.
     *
     * @param fw       The Writer to send the output to.
     * @param database The database whose strings we should write.
     * @throws IOException If anthing goes wrong in writing.
     */
    private static void writeStrings(Writer fw, BibDatabase database) throws IOException {
        FileActions.previousStringType = BibtexString.Type.AUTHOR;
        List<BibtexString> strings = new ArrayList<>();
        for (String s : database.getStringKeySet()) {
            strings.add(database.getString(s));
        }
        Collections.sort(strings, new BibtexStringComparator(true));
        // First, make a Map of all entries:
        HashMap<String, BibtexString> remaining = new HashMap<>();
        int maxKeyLength = 0;
        for (BibtexString string : strings) {
            remaining.put(string.getName(), string);
            maxKeyLength = Math.max(maxKeyLength, string.getName().length());
        }

        for (BibtexString.Type t : BibtexString.Type.values()) {
            for (BibtexString bs : strings) {
                if (remaining.containsKey(bs.getName()) && (bs.getType() == t)) {
                    FileActions.writeString(fw, bs, remaining, maxKeyLength);
                }
            }
        }
    }

    private static void writeString(Writer fw, BibtexString bs, Map<String, BibtexString> remaining, int maxKeyLength)
            throws IOException {
        // First remove this from the "remaining" list so it can't cause problem with circular refs:
        remaining.remove(bs.getName());

        //if the string has not been modified, write it back as it was
        if (!bs.hasChanged()) {
            fw.write(bs.getParsedSerialization());
            return;
        }

        // Then we go through the string looking for references to other strings. If we find references
        // to strings that we will write, but still haven't, we write those before proceeding. This ensures
        // that the string order will be acceptable for BibTeX.
        String content = bs.getContent();
        Matcher m;
        while ((m = FileActions.REFERENCE_PATTERN.matcher(content)).find()) {
            String foundLabel = m.group(1);
            int restIndex = content.indexOf(foundLabel) + foundLabel.length();
            content = content.substring(restIndex);
            Object referred = remaining.get(foundLabel.substring(1, foundLabel.length() - 1));
            // If the label we found exists as a key in the "remaining" Map, we go on and write it now:
            if (referred != null) {
                FileActions.writeString(fw, (BibtexString) referred, remaining, maxKeyLength);
            }
        }

        if (FileActions.previousStringType != bs.getType()) {
            fw.write(Globals.NEWLINE);
            FileActions.previousStringType = bs.getType();
        }

        StringBuilder suffixSB = new StringBuilder();
        for (int i = maxKeyLength - bs.getName().length(); i > 0; i--) {
            suffixSB.append(' ');
        }
        String suffix = suffixSB.toString();

        fw.write("@String { " + bs.getName() + suffix + " = ");
        if (!bs.getContent().isEmpty()) {
            try {
                String formatted = new LatexFieldFormatter().format(bs.getContent(), LatexFieldFormatter.BIBTEX_STRING);
                fw.write(formatted);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "The # character is not allowed in BibTeX strings unless escaped as in '\\#'.\n"
                                + "Before saving, please edit any strings containing the # character.");
            }

        } else {
            fw.write("{}");
        }

        fw.write(" }" + Globals.NEWLINE);
    }

    /**
     * Writes the file encoding information.
     *
     * @param encoding String the name of the encoding, which is part of the file header.
     */
    private static void writeBibFileHeader(Writer out, Charset encoding) throws IOException {
        out.write("% ");
        out.write(Globals.encPrefix + encoding);
    }

    /**
     * Saves the database to file. Two boolean values indicate whether only
     * entries with a nonzero Globals.SEARCH value and only entries with a
     * nonzero Globals.GROUPSEARCH value should be saved. This can be used to
     * let the user save only the results of a search. False and false means all
     * entries are saved.
     */
    public static SaveSession saveDatabase(BibDatabase database,
                                           MetaData metaData, File file, JabRefPreferences prefs,
                                           boolean checkSearch, boolean checkGroup, Charset encoding, boolean suppressBackup)
            throws SaveException {

        boolean backup = prefs.getBoolean(JabRefPreferences.BACKUP);
        if (suppressBackup) {
            backup = false;
        }

        SaveSession session;
        BibEntry exceptionCause = null;
        try {
            session = new SaveSession(file, encoding, backup);
        } catch (Throwable e) {
            if (encoding != null) {
                LOGGER.error("Error from encoding: '" + encoding.displayName(), e);
            }
            // we must catch all exceptions to be able notify users that
            // saving failed, no matter what the reason was
            // (and they won't just quit JabRef thinking
            // everything worked and loosing data)
            throw new SaveException(e.getMessage(), e.getLocalizedMessage());
        }

        Map<String, EntryType> types = new TreeMap<>();

        // Get our data stream. This stream writes only to a temporary file,
        // until committed.
        try (VerifyingWriter writer = session.getWriter()) {

            // Write signature.
            FileActions.writeBibFileHeader(writer, encoding);

            // Write preamble if there is one.
            FileActions.writePreamble(writer, database.getPreamble());

            // Write strings if there are any.
            FileActions.writeStrings(writer, database);

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            List<BibEntry> sorter = FileActions.getSortedEntries(database, metaData, null, true);

            BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new LatexFieldFormatter(), true);

            for (BibEntry entry : sorter) {
                exceptionCause = entry;

                // Check if we must write the type definition for this
                // entry, as well. Our criterion is that all non-standard
                // types (*not* customized standard types) must be written.
                EntryType entryType = entry.getType();

                if (EntryTypes.getStandardType(entryType.getName()) == null) {
                    types.put(entryType.getName(), entryType);
                }

                // Check if the entry should be written.
                boolean write = true;

                if (checkSearch && !FileActions.nonZeroField(entry, BibtexFields.SEARCH)) {
                    write = false;
                }

                if (checkGroup && !FileActions.nonZeroField(entry, BibtexFields.GROUPSEARCH)) {
                    write = false;
                }

                if (write) {
                    bibtexEntryWriter.write(entry, writer);
                }
            }

            // Write meta data.
            if (metaData != null) {
                metaData.writeMetaData(writer);
            }

            // Write type definitions, if any:
            if (!types.isEmpty()) {
                for (Map.Entry<String, EntryType> stringBibtexEntryTypeEntry : types.entrySet()) {
                    EntryType type = stringBibtexEntryTypeEntry.getValue();
                    if (type instanceof CustomEntryType) {
                        CustomEntryType tp = (CustomEntryType) type;
                        CustomEntryTypesManager.save(tp, writer);
                    }
                }

            }

            //finally write whatever remains of the file, but at least a concluding newline
            if ((database.getEpilog() != null) && !(database.getEpilog().isEmpty())) {
               writer.write(database.getEpilog());
            } else {
               writer.write(Globals.NEWLINE);
            }
        } catch (IOException ex) {
            LOGGER.error("Could not write file", ex);
            session.cancel();
            // repairAfterError(file, backup, INIT_OK);
            throw new SaveException(ex.getMessage(), ex.getLocalizedMessage(), exceptionCause);
        }

        return session;

    }


    private static class SaveSettings {

        public final String pri;
        public final String sec;
        public final String ter;
        public final boolean priD;
        public final boolean secD;
        public final boolean terD;


        public SaveSettings(boolean isSaveOperation, MetaData metaData) {
            /* three options:
             * 1. original order (saveInOriginalOrder) -- not hit here as SaveSettings is not called in that case
             * 2. current table sort order
             * 3. ordered by specified order
             */

            Vector<String> storedSaveOrderConfig = null;
            if (isSaveOperation) {
                storedSaveOrderConfig = metaData.getData(net.sf.jabref.gui.DatabasePropertiesDialog.SAVE_ORDER_CONFIG);
            }

            // This case should never be hit as SaveSettings() is never called if InOriginalOrder is true
            assert (storedSaveOrderConfig == null) && isSaveOperation && !Globals.prefs.getBoolean(JabRefPreferences.SAVE_IN_ORIGINAL_ORDER);
            assert (storedSaveOrderConfig == null) && !isSaveOperation && !Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER);

            if (storedSaveOrderConfig != null) {
                // follow the metaData
                SaveOrderConfig saveOrderConfig = new SaveOrderConfig(storedSaveOrderConfig);
                assert !saveOrderConfig.saveInOriginalOrder;
                assert saveOrderConfig.saveInSpecifiedOrder;
                pri = saveOrderConfig.sortCriteria[0].field;
                sec = saveOrderConfig.sortCriteria[1].field;
                ter = saveOrderConfig.sortCriteria[2].field;
                priD = saveOrderConfig.sortCriteria[0].descending;
                secD = saveOrderConfig.sortCriteria[1].descending;
                terD = saveOrderConfig.sortCriteria[2].descending;
            } else if (isSaveOperation && Globals.prefs.getBoolean(JabRefPreferences.SAVE_IN_SPECIFIED_ORDER)) {
                pri = Globals.prefs.get(JabRefPreferences.SAVE_PRIMARY_SORT_FIELD);
                sec = Globals.prefs.get(JabRefPreferences.SAVE_SECONDARY_SORT_FIELD);
                ter = Globals.prefs.get(JabRefPreferences.SAVE_TERTIARY_SORT_FIELD);
                priD = Globals.prefs.getBoolean(JabRefPreferences.SAVE_PRIMARY_SORT_DESCENDING);
                secD = Globals.prefs.getBoolean(JabRefPreferences.SAVE_SECONDARY_SORT_DESCENDING);
                terD = Globals.prefs.getBoolean(JabRefPreferences.SAVE_TERTIARY_SORT_DESCENDING);
            } else if (!isSaveOperation && Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER)) {
                pri = Globals.prefs.get(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD);
                sec = Globals.prefs.get(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD);
                ter = Globals.prefs.get(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD);
                priD = Globals.prefs.getBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING);
                secD = Globals.prefs.getBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING);
                terD = Globals.prefs.getBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING);
            } else {
                // The setting is to save according to the current table order.
                pri = Globals.prefs.get(JabRefPreferences.TABLE_PRIMARY_SORT_FIELD);
                sec = Globals.prefs.get(JabRefPreferences.TABLE_SECONDARY_SORT_FIELD);
                ter = Globals.prefs.get(JabRefPreferences.TABLE_TERTIARY_SORT_FIELD);
                priD = Globals.prefs.getBoolean(JabRefPreferences.TABLE_PRIMARY_SORT_DESCENDING);
                secD = Globals.prefs.getBoolean(JabRefPreferences.TABLE_SECONDARY_SORT_DESCENDING);
                terD = Globals.prefs.getBoolean(JabRefPreferences.TABLE_TERTIARY_SORT_DESCENDING);
            }
        }
    }


    private static List<Comparator<BibEntry>> getSaveComparators(boolean isSaveOperation, MetaData metaData) {
        SaveSettings saveSettings = new SaveSettings(isSaveOperation, metaData);

        List<Comparator<BibEntry>> comparators = new ArrayList<>();
        if (isSaveOperation) {
            comparators.add(new CrossRefEntryComparator());
        }
        comparators.add(new FieldComparator(saveSettings.pri, saveSettings.priD));
        comparators.add(new FieldComparator(saveSettings.sec, saveSettings.secD));
        comparators.add(new FieldComparator(saveSettings.ter, saveSettings.terD));
        comparators.add(new FieldComparator(BibEntry.KEY_FIELD));

        return comparators;
    }

    /**
     * Saves the database to file, including only the entries included in the
     * supplied input array bes.
     *
     * @return A List containing warnings, if any.
     */
    public static SaveSession savePartOfDatabase(BibDatabase database, MetaData metaData,
                                                 File file,
                                                 JabRefPreferences prefs, BibEntry[] bes, Charset encoding, DatabaseSaveType saveType)
            throws SaveException {


        BibEntry be = null;
        boolean backup = prefs.getBoolean(JabRefPreferences.BACKUP);

        SaveSession session;
        try {
            session = new SaveSession(file, encoding, backup);
        } catch (IOException e) {
            throw new SaveException(e.getMessage(), e.getLocalizedMessage());
        }

        // Map to collect entry type definitions
        // that we must save along with entries using them.
        Map<String, EntryType> types = new TreeMap<>();

        // Define our data stream.
        try (VerifyingWriter fw = session.getWriter()) {

            if (saveType != DatabaseSaveType.PLAIN_BIBTEX) {
                // Write signature.
                FileActions.writeBibFileHeader(fw, encoding);
            }

            // Write preamble if there is one.
            FileActions.writePreamble(fw, database.getPreamble());

            // Write strings if there are any.
            FileActions.writeStrings(fw, database);

            // Write database entries. Take care, using CrossRefEntry-
            // Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be
            // sorted as they appear on the screen.
            List<Comparator<BibEntry>> comparators = FileActions.getSaveComparators(true, metaData);

            // Use glazed lists to get a sorted view of the entries:
            List<BibEntry> sorter = new ArrayList<>(bes.length);
            Collections.addAll(sorter, bes);
            Collections.sort(sorter, new FieldComparatorStack<>(comparators));

            BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new LatexFieldFormatter(), true);

            for (BibEntry aSorter : sorter) {
                be = aSorter;

                // Check if we must write the type definition for this
                // entry, as well. Our criterion is that all non-standard
                // types (*not* customized standard types) must be written.
                EntryType tp = be.getType();
                if (EntryTypes.getStandardType(tp.getName()) == null) {
                    types.put(tp.getName(), tp);
                }

                bibtexEntryWriter.write(be, fw);
                //only append newline if the entry has changed
                if (!be.hasChanged()) {
                    fw.write(Globals.NEWLINE);
                }
            }

            // Write meta data.
            if ((saveType != DatabaseSaveType.PLAIN_BIBTEX) && (metaData != null)) {
                metaData.writeMetaData(fw);
            }

            // Write type definitions, if any:
            if (!types.isEmpty()) {
                for (Map.Entry<String, EntryType> stringBibtexEntryTypeEntry : types.entrySet()) {
                    CustomEntryType tp = (CustomEntryType) stringBibtexEntryTypeEntry.getValue();
                    CustomEntryTypesManager.save(tp, fw);
                    fw.write(Globals.NEWLINE);
                }

            }
        } catch (Throwable ex) {
            session.cancel();
            //repairAfterError(file, backup, status);
            throw new SaveException(ex.getMessage(), ex.getLocalizedMessage(), be);
        }

        return session;

    }

    /**
     * This method attempts to get a Reader for the file path given, either by
     * loading it as a resource (from within jar), or as a normal file. If
     * unsuccessful (e.g. file not found), an IOException is thrown.
     */
    public static Reader getReader(String name) throws IOException {
        Reader reader;
        // Try loading as a resource first. This works for files inside the jar:
        URL reso = Globals.class.getResource(name);

        // If that didn't work, try loading as a normal file URL:
        if (reso != null) {
            try {
                reader = new InputStreamReader(reso.openStream());
            } catch (FileNotFoundException ex) {
                throw new IOException("Cannot find layout file: '" + name + "'.");
            }
        } else {
            File f = new File(name);
            try {
                reader = new FileReader(f);
            } catch (FileNotFoundException ex) {
                throw new IOException("Cannot find layout file: '" + name + "'.");
            }
        }

        return reader;
    }

    /*
     * We have begun to use getSortedEntries() for both database save operations
     * and non-database save operations.  In a non-database save operation
     * (such as the exportDatabase call), we do not wish to use the
     * global preference of saving in standard order.
     */
    public static List<BibEntry> getSortedEntries(BibDatabase database, MetaData metaData, Set<String> keySet, boolean isSaveOperation) {
        //if no meta data are present, simply return in original order
        if(metaData == null) {
            List<BibEntry> result = new LinkedList();
            result.addAll(database.getEntries());
            return result;
        }

        boolean inOriginalOrder;
        if (isSaveOperation) {
            Vector<String> storedSaveOrderConfig = metaData.getData(net.sf.jabref.gui.DatabasePropertiesDialog.SAVE_ORDER_CONFIG);
            if (storedSaveOrderConfig == null) {
                inOriginalOrder = Globals.prefs.getBoolean(JabRefPreferences.SAVE_IN_ORIGINAL_ORDER);
            } else {
                SaveOrderConfig saveOrderConfig = new SaveOrderConfig(storedSaveOrderConfig);
                inOriginalOrder = saveOrderConfig.saveInOriginalOrder;
            }
        } else {
            inOriginalOrder = Globals.prefs.getBoolean(JabRefPreferences.EXPORT_IN_ORIGINAL_ORDER);
        }
        List<Comparator<BibEntry>> comparators;
        if (inOriginalOrder) {
            // Sort entries based on their creation order, utilizing the fact
            // that IDs used for entries are increasing, sortable numbers.
            comparators = new ArrayList<>();
            comparators.add(new CrossRefEntryComparator());
            comparators.add(new IdComparator());
        } else {
            comparators = FileActions.getSaveComparators(isSaveOperation, metaData);
        }

        FieldComparatorStack<BibEntry> comparatorStack = new FieldComparatorStack<>(comparators);
        List<BibEntry> sorter = new ArrayList<>();

        if (keySet == null) {
            keySet = database.getKeySet();
        }

        for (String id : keySet) {
            sorter.add(database.getEntryById(id));
        }

        Collections.sort(sorter, comparatorStack);

        return sorter;
    }

    /**
     * @return true iff the entry has a nonzero value in its field.
     */
    private static boolean nonZeroField(BibEntry be, String field) {
        String o = be.getField(field);

        return (o != null) && !"0".equals(o);
    }
}
