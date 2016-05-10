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

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.bibtex.BibEntryWriter;
import net.sf.jabref.bibtex.comparator.BibtexStringComparator;
import net.sf.jabref.bibtex.comparator.CrossRefEntryComparator;
import net.sf.jabref.bibtex.comparator.FieldComparator;
import net.sf.jabref.bibtex.comparator.FieldComparatorStack;
import net.sf.jabref.logic.FieldChange;
import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.id.IdComparator;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexString;
import net.sf.jabref.model.entry.CustomEntryType;
import net.sf.jabref.model.entry.EntryType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BibDatabaseWriter {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile("(#[A-Za-z]+#)"); // Used to detect string references in strings
    private static final Log LOGGER = LogFactory.getLog(BibDatabaseWriter.class);
    private static final String STRING_PREFIX = "@String";
    private static final String COMMENT_PREFIX = "@Comment";
    private static final String PREAMBLE_PREFIX = "@Preamble";
    private BibEntry exceptionCause;
    private boolean isFirstStringInType;

    private static List<Comparator<BibEntry>> getSaveComparators(SavePreferences preferences, MetaData metaData) {

        List<Comparator<BibEntry>> comparators = new ArrayList<>();
        Optional<SaveOrderConfig> saveOrder = getSaveOrder(preferences, metaData);

        if (! saveOrder.isPresent()) {
            // Take care, using CrossRefEntry-Comparator, that referred entries occur after referring
            // ones. Apart from crossref requirements, entries will be sorted based on their creation order,
            // utilizing the fact that IDs used for entries are increasing, sortable numbers.
            comparators.add(new CrossRefEntryComparator());
            comparators.add(new IdComparator());
        } else {
            comparators.add(new CrossRefEntryComparator());

            comparators.add(new FieldComparator(saveOrder.get().sortCriteria[0]));
            comparators.add(new FieldComparator(saveOrder.get().sortCriteria[1]));
            comparators.add(new FieldComparator(saveOrder.get().sortCriteria[2]));

            comparators.add(new FieldComparator(BibEntry.KEY_FIELD));
        }

        return comparators;
    }

    /*
     * We have begun to use getSortedEntries() for both database save operations
     * and non-database save operations.  In a non-database save operation
     * (such as the exportDatabase call), we do not wish to use the
     * global preference of saving in standard order.
     */
    public static List<BibEntry> getSortedEntries(BibDatabaseContext bibDatabaseContext, List<BibEntry> entriesToSort,
            SavePreferences preferences) {
        Objects.requireNonNull(bibDatabaseContext);
        Objects.requireNonNull(entriesToSort);

        //if no meta data are present, simply return in original order
        if (bibDatabaseContext.getMetaData() == null) {
            List<BibEntry> result = new LinkedList<>();
            result.addAll(entriesToSort);
            return result;
        }

        List<Comparator<BibEntry>> comparators = BibDatabaseWriter.getSaveComparators(preferences,
                bibDatabaseContext.getMetaData());
        FieldComparatorStack<BibEntry> comparatorStack = new FieldComparatorStack<>(comparators);

        List<BibEntry> sorted = new ArrayList<>();
        sorted.addAll(entriesToSort);

        Collections.sort(sorted, comparatorStack);

        return sorted;
    }

    private static Optional<SaveOrderConfig> getSaveOrder(SavePreferences preferences, MetaData metaData) {
        /* three options:
         * 1. original order
         * 2. order specified in metaData
         * 3. order specified in preferences
         */

        if(preferences.isSaveInOriginalOrder()) {
            return Optional.empty();
        }

        if(preferences.getTakeMetadataSaveOrderInAccount()) {
            return metaData.getSaveOrderConfig();
        }

        return Optional.ofNullable(preferences.getSaveOrder());
    }

    /**
     * Saves the database to file. Two boolean values indicate whether only
     * entries which are marked as search / group hit should be saved. This can be used to
     * let the user save only the results of a search. False and false means all
     * entries are saved.
     */
    public SaveSession saveDatabase(BibDatabaseContext bibDatabaseContext, SavePreferences preferences)
            throws SaveException {
        return savePartOfDatabase(bibDatabaseContext, bibDatabaseContext.getDatabase().getEntries(), preferences);
    }

    public SaveSession savePartOfDatabase(BibDatabaseContext bibDatabaseContext, List<BibEntry> entries,
            SavePreferences preferences) throws SaveException {

        SaveSession session;
        try {
            session = new SaveSession(preferences.getEncoding(), preferences.getMakeBackup());
        } catch (IOException e) {
            throw new SaveException(e.getMessage(), e.getLocalizedMessage());
        }

        exceptionCause = null;
        // Get our data stream. This stream writes only to a temporary file until committed.
        try (VerifyingWriter writer = session.getWriter()) {
            List<FieldChange> saveActionChanges = writePartOfDatabase(writer, bibDatabaseContext, entries, preferences);
            session.addFieldChanges(saveActionChanges);
        } catch (IOException ex) {
            LOGGER.error("Could not write file", ex);
            session.cancel();
            throw new SaveException(ex.getMessage(), ex.getLocalizedMessage(), exceptionCause);
        }

        return session;

    }

    public List<FieldChange> writePartOfDatabase(Writer writer, BibDatabaseContext bibDatabaseContext,
            List<BibEntry> entries, SavePreferences preferences) throws IOException {
        Objects.requireNonNull(writer);

        // Map to collect entry type definitions that we must save along with entries using them.
        Map<String, EntryType> typesToWrite = new TreeMap<>();

        if (preferences.getSaveType() != SavePreferences.DatabaseSaveType.PLAIN_BIBTEX) {
            // Write signature.
            writeBibFileHeader(writer, preferences.getEncoding());
        }

        // Write preamble if there is one.
        writePreamble(writer, bibDatabaseContext.getDatabase().getPreamble());

        // Write strings if there are any.
        writeStrings(writer, bibDatabaseContext.getDatabase(), preferences.isReformatFile());

        // Write database entries.
        List<BibEntry> sortedEntries = BibDatabaseWriter.getSortedEntries(bibDatabaseContext, entries, preferences);
        List<FieldChange> saveActionChanges = BibDatabaseWriter.applySaveActions(sortedEntries, bibDatabaseContext.getMetaData());
        BibEntryWriter bibtexEntryWriter = new BibEntryWriter(new LatexFieldFormatter(), true);
        for (BibEntry entry : sortedEntries) {
            exceptionCause = entry;

            // Check if we must write the type definition for this
            // entry, as well. Our criterion is that all non-standard
            // types (*not* all customized standard types) must be written.
            if (!EntryTypes.getStandardType(entry.getType(), bibDatabaseContext.getMode()).isPresent()) {
                // If user-defined entry type, then add it
                // Otherwise (getType returns empty optional) it is a completely unknown entry type, so ignore it
                EntryTypes.getType(entry.getType(), bibDatabaseContext.getMode()).ifPresent(
                        entryType -> typesToWrite.put(entryType.getName(), entryType));
            }

            bibtexEntryWriter.write(entry, writer, bibDatabaseContext.getMode(), preferences.isReformatFile());

        }

        if (preferences.getSaveType() != SavePreferences.DatabaseSaveType.PLAIN_BIBTEX) {
            // Write meta data.
            writeMetaData(writer, bibDatabaseContext.getMetaData());

            // Write type definitions, if any:
            writeTypeDefinitions(writer, typesToWrite);
        }

        //finally write whatever remains of the file, but at least a concluding newline
        writeEpilogue(writer, bibDatabaseContext.getDatabase());

        return saveActionChanges;
    }

    /**
     * Saves the database to file, including only the entries included in the
     * supplied input array bes.
     */
    public SaveSession savePartOfDatabase(BibDatabaseContext bibDatabaseContext, SavePreferences preferences,
            List<BibEntry> entries) throws SaveException {

        return savePartOfDatabase(bibDatabaseContext, entries, preferences);
    }

    private static List<FieldChange> applySaveActions(List<BibEntry> toChange, MetaData metaData) {
        List<FieldChange> changes = new ArrayList<>();

        Optional<FieldFormatterCleanups> saveActions = metaData.getSaveActions();
        if (saveActions.isPresent()) {
            // save actions defined -> apply for every entry
            for (BibEntry entry : toChange) {
                changes.addAll(saveActions.get().applySaveActions(entry));
            }
        }

        return changes;
    }

    /**
     * Writes the file encoding information.
     *
     * @param encoding String the name of the encoding, which is part of the file header.
     */
    private void writeBibFileHeader(Writer out, Charset encoding) throws IOException {
        if(encoding == null) {
            return;
        }

        out.write("% ");
        out.write(Globals.ENCODING_PREFIX + encoding);
        out.write(Globals.NEWLINE);
    }

    private void writeEpilogue(Writer writer, BibDatabase database) throws IOException {
        if ((database.getEpilog() != null) && !(database.getEpilog().isEmpty())) {
            writer.write(Globals.NEWLINE);
            writer.write(database.getEpilog());
            writer.write(Globals.NEWLINE);
        }
    }

    /**
     * Writes all data to the specified writer, using each object's toString() method.
     */
    private void writeMetaData(Writer out, MetaData metaData) throws IOException {
        if (metaData == null) {
            return;
        }

        Map<String, String> serializedMetaData = metaData.serialize();

        for(Map.Entry<String, String> metaItem : serializedMetaData.entrySet()) {

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Globals.NEWLINE);
            stringBuilder.append(COMMENT_PREFIX + "{").append(MetaData.META_FLAG).append(metaItem.getKey()).append(":");
            stringBuilder.append(metaItem.getValue());
            stringBuilder.append("}");
            stringBuilder.append(Globals.NEWLINE);

            out.write(stringBuilder.toString());
        }
    }

    private void writePreamble(Writer fw, String preamble) throws IOException {
        if (preamble != null) {
            fw.write(Globals.NEWLINE);
            fw.write(PREAMBLE_PREFIX + "{");
            fw.write(preamble);
            fw.write('}' + Globals.NEWLINE);
        }
    }

    private void writeString(Writer fw, BibtexString bs, Map<String, BibtexString> remaining, int maxKeyLength,
            Boolean reformatFile)
            throws IOException {
        // First remove this from the "remaining" list so it can't cause problem with circular refs:
        remaining.remove(bs.getName());

        //if the string has not been modified, write it back as it was
        if (!reformatFile && !bs.hasChanged()) {
            fw.write(bs.getParsedSerialization());
            return;
        }

        if(isFirstStringInType) {
            fw.write(Globals.NEWLINE);
        }

        // Then we go through the string looking for references to other strings. If we find references
        // to strings that we will write, but still haven't, we write those before proceeding. This ensures
        // that the string order will be acceptable for BibTeX.
        String content = bs.getContent();
        Matcher m;
        while ((m = BibDatabaseWriter.REFERENCE_PATTERN.matcher(content)).find()) {
            String foundLabel = m.group(1);
            int restIndex = content.indexOf(foundLabel) + foundLabel.length();
            content = content.substring(restIndex);
            Object referred = remaining.get(foundLabel.substring(1, foundLabel.length() - 1));
            // If the label we found exists as a key in the "remaining" Map, we go on and write it now:
            if (referred != null) {
                writeString(fw, (BibtexString) referred, remaining, maxKeyLength, reformatFile);
            }
        }

        fw.write(STRING_PREFIX + "{" + bs.getName() + StringUtil.repeatSpaces(maxKeyLength - bs.getName().length()) + " = ");
        if (bs.getContent().isEmpty()) {
            fw.write("{}");
        } else {
            try {
                String formatted = new LatexFieldFormatter().format(bs.getContent(), LatexFieldFormatter.BIBTEX_STRING);
                fw.write(formatted);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException(
                        "The # character is not allowed in BibTeX strings unless escaped as in '\\#'.\n"
                                + "Before saving, please edit any strings containing the # character.");
            }
        }

        fw.write("}" + Globals.NEWLINE);
    }

    /**
     * Write all strings in alphabetical order, modified to produce a safe (for
     * BibTeX) order of the strings if they reference each other.
     *
     * @param fw       The Writer to send the output to.
     * @param database The database whose strings we should write.
     * @param reformatFile
     * @throws IOException If anything goes wrong in writing.
     */
    private void writeStrings(Writer fw, BibDatabase database, Boolean reformatFile) throws IOException {
        List<BibtexString> strings = database.getStringKeySet().stream().map(database::getString).collect(
                Collectors.toList());
        strings.sort(new BibtexStringComparator(true));
        // First, make a Map of all entries:
        Map<String, BibtexString> remaining = new HashMap<>();
        int maxKeyLength = 0;
        for (BibtexString string : strings) {
            remaining.put(string.getName(), string);
            maxKeyLength = Math.max(maxKeyLength, string.getName().length());
        }

        for (BibtexString.Type t : BibtexString.Type.values()) {
            isFirstStringInType = true;
            for (BibtexString bs : strings) {
                if (remaining.containsKey(bs.getName()) && (bs.getType() == t)) {
                    writeString(fw, bs, remaining, maxKeyLength, reformatFile);
                    isFirstStringInType = false;
                }
            }
        }
    }

    private void writeTypeDefinitions(Writer writer, Map<String, EntryType> types) throws IOException {
        for (EntryType type : types.values()) {
            if (type instanceof CustomEntryType) {
                CustomEntryType customType = (CustomEntryType) type;
                writer.write(Globals.NEWLINE);
                writer.write(COMMENT_PREFIX + "{");
                writer.write(CustomEntryType.ENTRYTYPE_FLAG);
                writer.write(customType.getName());
                writer.write(": req[");
                writer.write(customType.getRequiredFieldsString());
                writer.write("] opt[");
                writer.write(String.join(";", customType.getOptionalFields()));
                writer.write("]}");
                writer.write(Globals.NEWLINE);
            }
        }
    }

}
