/*  Copyright (C) -2015 JabRef contributors.
    Copyright (C) 2015 Oliver Kopp

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
package net.sf.jabref.bibtex;

import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibtexEntry;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import com.google.common.base.Strings;
import net.sf.jabref.model.entry.EntryType;

public class BibtexEntryWriter {

    /**
     * Display name map for entry field names.
     */
    private static final Map<String, String> tagDisplayNameMap = new HashMap<>();


    static {
        // The field name display map.
        BibtexEntryWriter.tagDisplayNameMap.put("bibtexkey", "BibTeXKey");
        BibtexEntryWriter.tagDisplayNameMap.put("doi", "DOI");
        BibtexEntryWriter.tagDisplayNameMap.put("ee", "EE");
        BibtexEntryWriter.tagDisplayNameMap.put("howpublished", "HowPublished");
        BibtexEntryWriter.tagDisplayNameMap.put("lastchecked", "LastChecked");
        BibtexEntryWriter.tagDisplayNameMap.put("isbn", "ISBN");
        BibtexEntryWriter.tagDisplayNameMap.put("issn", "ISSN");
        BibtexEntryWriter.tagDisplayNameMap.put("UNKNOWN", "UNKNOWN");
        BibtexEntryWriter.tagDisplayNameMap.put("url", "URL");
    }

    private static final Map<String, List<String>> requiredFieldsSorted = new HashMap<>();

    private static final Map<String, List<String>> optionalFieldsSorted = new HashMap<>();


    /**
     * The maximum length of a field name to properly make the alignment of the
     * equal sign.
     */
    private static final int maxFieldLength;

    static {
        // Looking for the longest field name.
        // XXX JK: Look for all used field names not only defined once, since
        //         there may be some unofficial field name used.
        int max = 0;
        for (EntryType type : EntryTypes.getAllValues()) {
            for (String field : type.getRequiredFieldsFlat()) {
                max = Math.max(max, field.length());
            }

            if (type.getOptionalFields() != null) {
                for (String field : type.getOptionalFields()) {
                    max = Math.max(max, field.length());
                }
            }
        }
        maxFieldLength = max;
    }

    private final LatexFieldFormatter fieldFormatter;
    private final boolean write;
    private final boolean writeFieldCameCaseName = Globals.prefs.getBoolean(JabRefPreferences.WRITEFIELD_CAMELCASENAME);
    private final boolean writeFieldAddSpaces = Globals.prefs.getBoolean(JabRefPreferences.WRITEFIELD_ADDSPACES);
    private final boolean includeEmptyFields = Globals.prefs.getBoolean(JabRefPreferences.INCLUDE_EMPTY_FIELDS);
    private final int writeFieldSortStyle = Globals.prefs.getInt(JabRefPreferences.WRITEFIELD_SORTSTYLE);


    public BibtexEntryWriter(LatexFieldFormatter fieldFormatter, boolean write) {
        this.fieldFormatter = fieldFormatter;
        this.write = write;
    }

    public void write(BibtexEntry entry, Writer out) throws IOException {
        switch (writeFieldSortStyle) {
            case 0:
                writeRequiredFieldsFirstOptionalFieldsSecondRemainingFieldsThird(entry, out);
                break;
            case 1:
                writeRequiredFieldsFirstRemainingFieldsSecond(entry, out);
                break;
            case 2:
                writeUserDefinedOrder(entry, out);
                break;
        }
    }

    /**
     * new style ver>=2.10, sort the field for requiredFields, optionalFields and other fields separately
     *
     * @param entry
     * @param out
     * @throws IOException
     */
    private void writeRequiredFieldsFirstOptionalFieldsSecondRemainingFieldsThird(BibtexEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName() + '{');

        HashSet<String> writtenFields = new HashSet<>();

        writeKeyField(entry, out);
        writtenFields.add(BibtexEntry.KEY_FIELD);

        // Write required fields first.
        // Thereby, write the title field first.
        boolean hasWritten = writeField(entry, out, "title", false);
        writtenFields.add("title");

        if (entry.getRequiredFieldsFlat() != null) {
            List<String> requiredFields = getRequiredFieldsSorted(entry);
            for (String value : requiredFields) {
                if (!writtenFields.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                    writtenFields.add(value);
                }
            }
        }

        // Then optional fields
        if (entry.getOptionalFields() != null) {
            List<String> optionalFields = getOptionalFieldsSorted(entry);
            for (String value : optionalFields) {
                if (!writtenFields.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                    writtenFields.add(value);
                }
            }
        }

        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<>();
        for (String key : entry.getFieldNames()) {
            boolean writeIt = write ? BibtexFields.isWriteableField(key) :
                    BibtexFields.isDisplayableField(key);
            if (!writtenFields.contains(key) && writeIt) {
                remainingFields.add(key);
            }
        }

        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten);
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}' + Globals.NEWLINE);
    }

    private List<String> getRequiredFieldsSorted(BibtexEntry entry) {
        String entryTypeName = entry.getType().getName();
        List<String> sortedFields = requiredFieldsSorted.get(entryTypeName);

        // put into cache if necessary
        if (sortedFields == null) {
            sortedFields = new ArrayList<>(entry.getRequiredFieldsFlat());
            Collections.sort(sortedFields);
            requiredFieldsSorted.put(entryTypeName, sortedFields);
        }

        return sortedFields;
    }

    private List<String> getOptionalFieldsSorted(BibtexEntry entry) {
        String entryTypeName = entry.getType().getName();
        List<String> sortedFields = optionalFieldsSorted.get(entryTypeName);

        // put into chache if necessary
        if (sortedFields == null) {
            sortedFields = new ArrayList<>(entry.getOptionalFields());
            Collections.sort(sortedFields);
            optionalFieldsSorted.put(entryTypeName, sortedFields);
        }

        return sortedFields;
    }

    /**
     * old style ver<=2.9.2, write fields in the order of requiredFields, optionalFields and other fields, but does not sort the fields.
     *
     * @param entry
     * @param out
     * @throws IOException
     */
    private void writeRequiredFieldsFirstRemainingFieldsSecond(BibtexEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName().toUpperCase(Locale.US) + '{');

        writeKeyField(entry, out);

        HashSet<String> written = new HashSet<>();
        written.add(BibtexEntry.KEY_FIELD);
        boolean hasWritten = false;
        // Write required fields first.
        List<String> fields = entry.getRequiredFieldsFlat();
        if (fields != null) {
            for (String value : fields) {
                hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                written.add(value);
            }
        }
        // Then optional fields.
        fields = entry.getOptionalFields();
        if (fields != null) {
            for (String value : fields) {
                if (!written.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                    written.add(value);
                }
            }
        }
        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<>();
        for (String key : entry.getFieldNames()) {
            boolean writeIt = write ? BibtexFields.isWriteableField(key) :
                    BibtexFields.isDisplayableField(key);
            if (!written.contains(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten);
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}' + Globals.NEWLINE);
    }

    private void writeUserDefinedOrder(BibtexEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName() + '{');

        writeKeyField(entry, out);
        HashMap<String, String> written = new HashMap<>();
        written.put(BibtexEntry.KEY_FIELD, null);
        boolean hasWritten = false;

        // Write user defined fields first.
        String[] fields = Globals.prefs.getStringArray(JabRefPreferences.WRITEFIELD_USERDEFINEDORDER);
        if (fields != null) {
            //do not sort, write as it is.
            for (String value : fields) {
                if (!written.containsKey(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                    written.put(value, null);
                }
            }
        }

        // Then write remaining fields in alphabetic order.

        //STA get remaining fields
        TreeSet<String> remainingFields = new TreeSet<>();
        for (String key : entry.getFieldNames()) {
            //iterate through all fields
            boolean writeIt = write ? BibtexFields.isWriteableField(key) :
                    BibtexFields.isDisplayableField(key);
            //find the ones has not been written.
            if (!written.containsKey(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        //END get remaining fields


        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten);
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}' + Globals.NEWLINE);

    }

    private void writeKeyField(BibtexEntry entry, Writer out) throws IOException {
        String keyField = StringUtil.shaveString(entry.getCiteKey());
        out.write((keyField == null ? "" : keyField) + ',' + Globals.NEWLINE);
    }

    /**
     * Write a single field, if it has any content.
     *
     * @param entry             the entry to write
     * @param out               the target of the write
     * @param name              The field name
     * @param prependWhiteSpace Indicates whether this is the first field written for
     *                          this entry - if not, start by writing a comma and newline   @return true if this field was written, false if it was skipped because
     *                          it was not set
     * @throws IOException In case of an IO error
     */
    private boolean writeField(BibtexEntry entry, Writer out, String name, boolean prependWhiteSpace) throws IOException {
        String field = entry.getField(name);
        // only write field if is is not empty or if empty fields should be included
        // the first condition mirrors mirror behavior of com.jgoodies.common.base.Strings.isNotBlank(str)
        if (!Strings.nullToEmpty(field).trim().isEmpty() || includeEmptyFields) {
            if (prependWhiteSpace) {
                out.write(',' + Globals.NEWLINE);
            }

            out.write("  " + getFieldDisplayName(name) + " = ");

            try {
                out.write(fieldFormatter.format(field, name));
            } catch (IOException ex) {
                throw new IOException("Error in field '" + name + "': " + ex.getMessage());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get display version of a entry field.
     * <p>
     * BibTeX is case-insensitive therefore there is no difference between:
     * howpublished, HOWPUBLISHED, HowPublished, etc. Since the camel case
     * version is the most easy to read this should be the one written in the
     * *.bib file. Since there is no way how do detect multi-word strings by
     * default the first character will be made uppercase. In other characters
     * case needs to be changed the {@link #tagDisplayNameMap} will be used.
     *
     * @param field The name of the field.
     * @return The display version of the field name.
     */
    private String getFieldDisplayName(String field) {
        if (field.isEmpty()) {
            // hard coded "UNKNOWN" is assigned to a field without any name
            field = "UNKNOWN";
        }

        String suffix = "";
        if (writeFieldAddSpaces) {
            for (int i = BibtexEntryWriter.maxFieldLength - field.length(); i > 0; i--) {
                suffix += " ";
            }
        }

        String result;
        if (writeFieldCameCaseName) {
            if (BibtexEntryWriter.tagDisplayNameMap.containsKey(field.toLowerCase())) {
                result = BibtexEntryWriter.tagDisplayNameMap.get(field.toLowerCase()) + suffix;
            } else {
                result = (field.charAt(0) + "").toUpperCase() + field.substring(1) + suffix;
            }
        } else {
            result = field + suffix;
        }
        return result;
    }
}
