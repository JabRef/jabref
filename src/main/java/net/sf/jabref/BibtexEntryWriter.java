package net.sf.jabref;

import net.sf.jabref.export.FieldFormatter;
import net.sf.jabref.util.StringUtil;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class BibtexEntryWriter {

    /**
     * Display name map for entry field names.
     */
    private static final Map<String, String> tagDisplayNameMap = new HashMap<String, String>();

    static {
        // The field name display map.
        BibtexEntryWriter.tagDisplayNameMap.put("bibtexkey", "BibTeXKey");
        BibtexEntryWriter.tagDisplayNameMap.put("howpublished", "HowPublished");
        BibtexEntryWriter.tagDisplayNameMap.put("lastchecked", "LastChecked");
        BibtexEntryWriter.tagDisplayNameMap.put("isbn", "ISBN");
        BibtexEntryWriter.tagDisplayNameMap.put("issn", "ISSN");
        BibtexEntryWriter.tagDisplayNameMap.put("UNKNOWN", "UNKNOWN");
    }

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
        for (BibtexEntryType t : BibtexEntryType.ALL_TYPES.values()) {
            if (t.getRequiredFields() != null) {
                for (String field : t.getRequiredFields()) {
                    max = Math.max(max, field.length());
                }
            }
            if (t.getOptionalFields() != null) {
                for (String field : t.getOptionalFields()) {
                    max = Math.max(max, field.length());
                }
            }
        }
        maxFieldLength = max;
    }

    private final FieldFormatter fieldFormatter;
    private final boolean write;
    private final boolean writeFieldCameCaseName = Globals.prefs.getBoolean(JabRefPreferences.WRITEFIELD_CAMELCASENAME);
    private final boolean writeFieldAddSpaces = Globals.prefs.getBoolean(JabRefPreferences.WRITEFIELD_ADDSPACES);
    private final boolean includeEmptyFields = Globals.prefs.getBoolean(JabRefPreferences.INCLUDE_EMPTY_FIELDS);
    private final int writeFieldSortStype = Globals.prefs.getInt(JabRefPreferences.WRITEFIELD_SORTSTYLE);


    public BibtexEntryWriter(FieldFormatter fieldFormatter, boolean write) {
        this.fieldFormatter = fieldFormatter;
        this.write = write;
    }

    public void write(BibtexEntry entry, Writer out) throws IOException {
        switch (writeFieldSortStype) {
        case 0:
            writeSorted(entry, out);
            break;
        case 1:
            writeUnsorted(entry, out);
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
    private void writeSorted(BibtexEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName() + '{');

        String str = StringUtil.shaveString(entry.getField(BibtexFields.KEY_FIELD));
        out.write(((str == null) ? "" : str) + ',' + Globals.NEWLINE);
        HashMap<String, String> written = new HashMap<String, String>();
        written.put(BibtexFields.KEY_FIELD, null);
        // Write required fields first.
        // Thereby, write the title field first.
        boolean hasWritten = writeField(entry, out, "title", false, false);
        written.put("title", null);
        String[] s = entry.getRequiredFields();
        if (s != null) {
            Arrays.sort(s); // Sorting in alphabetic order.
            for (String value : s) {
                if (!written.containsKey(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten, false);
                    written.put(value, null);
                }
            }
        }
        // Then optional fields.
        s = entry.getOptionalFields();
        boolean first = true, previous;
        previous = false;
        if (s != null) {
            Arrays.sort(s); // Sorting in alphabetic order.
            for (String value : s) {
                if (!written.containsKey(value)) { // If field appears both in req. and opt. don't repeat.
                    //writeField(s[i], out, fieldFormatter);
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten, hasWritten && first);
                    written.put(value, null);
                    first = false;
                    previous = true;
                }
            }
        }
        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<String>();
        for (String key : entry.getAllFields()) {
            boolean writeIt = (write ? BibtexFields.isWriteableField(key) :
                    BibtexFields.isDisplayableField(key));
            if (!written.containsKey(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        first = previous;
        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten, hasWritten && first);
            first = false;
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}' + Globals.NEWLINE);
    }

    /**
     * old style ver<=2.9.2, write fields in the order of requiredFields, optionalFields and other fields, but does not sort the fields.
     *
     * @param entry
     * @param out
     * @throws IOException
     */
    private void writeUnsorted(BibtexEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName().toUpperCase(Locale.US) + '{');

        String str = StringUtil.shaveString(entry.getField(BibtexFields.KEY_FIELD));
        out.write(((str == null) ? "" : str) + ',' + Globals.NEWLINE);
        HashMap<String, String> written = new HashMap<String, String>();
        written.put(BibtexFields.KEY_FIELD, null);
        boolean hasWritten = false;
        // Write required fields first.
        String[] s = entry.getRequiredFields();
        if (s != null) {
            for (String value : s) {
                hasWritten = hasWritten | writeField(entry, out, value, hasWritten, false);
                written.put(value, null);
            }
        }
        // Then optional fields.
        s = entry.getOptionalFields();
        if (s != null) {
            for (String value : s) {
                if (!written.containsKey(value)) { // If field appears both in req. and opt. don't repeat.
                    //writeField(s[i], out, fieldFormatter);
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten, false);
                    written.put(value, null);
                }
            }
        }
        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<String>();
        for (String key : entry.getAllFields()) {
            boolean writeIt = (write ? BibtexFields.isWriteableField(key) :
                    BibtexFields.isDisplayableField(key));
            if (!written.containsKey(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten, false);
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}' + Globals.NEWLINE);
    }

    private void writeUserDefinedOrder(BibtexEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName() + '{');

        String str = StringUtil.shaveString(entry.getField(BibtexFields.KEY_FIELD));
        out.write(((str == null) ? "" : str) + ',' + Globals.NEWLINE);
        HashMap<String, String> written = new HashMap<String, String>();
        written.put(BibtexFields.KEY_FIELD, null);
        boolean hasWritten = false;

        // Write user defined fields first.
        String[] s = entry.getUserDefinedFields();
        if (s != null) {
            //do not sort, write as it is.
            for (String value : s) {
                if (!written.containsKey(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten, false);
                    written.put(value, null);
                }
            }
        }

        // Then write remaining fields in alphabetic order.
        boolean first, previous;
        previous = false;
        //STA get remaining fields
        TreeSet<String> remainingFields = new TreeSet<String>();
        for (String key : entry.getAllFields()) {
            //iterate through all fields
            boolean writeIt = (write ? BibtexFields.isWriteableField(key) :
                    BibtexFields.isDisplayableField(key));
            //find the ones has not been written.
            if (!written.containsKey(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        //END get remaining fields

        first = previous;
        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten, hasWritten && first);
            first = false;
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}' + Globals.NEWLINE);

    }

    /**
     * Write a single field, if it has any content.
     *
     * @param entry      the entry to write
     * @param out        the target of the write
     * @param name       The field name
     * @param isNotFirst Indicates whether this is the first field written for
     *                   this entry - if not, start by writing a comma and newline   @return true if this field was written, false if it was skipped because
     *                   it was not set
     * @throws IOException In case of an IO error
     */
    private boolean writeField(BibtexEntry entry, Writer out, String name, boolean isNotFirst, boolean isNextGroup) throws IOException {
        String o = entry.getField(name);
        if ((o != null) || includeEmptyFields) {
            if (isNotFirst) {
                out.write(',' + Globals.NEWLINE);
            }
            if (isNextGroup) {
                out.write(Globals.NEWLINE);
            }
            out.write("  " + getFieldDisplayName(name) + " = ");

            try {
                out.write(fieldFormatter.format(o, name));
            } catch (Throwable ex) {
                throw new IOException(Globals.lang("Error in field") + " '" + name + "': " + ex.getMessage());
            }
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get display version of a entry field.
     * <p/>
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

        String res;
        if (writeFieldCameCaseName) {
            if (BibtexEntryWriter.tagDisplayNameMap.containsKey(field.toLowerCase())) {
                res = BibtexEntryWriter.tagDisplayNameMap.get(field.toLowerCase()) + suffix;
            } else {
                res = (field.charAt(0) + "").toUpperCase() + field.substring(1) + suffix;
            }
        } else {
            res = field + suffix;
        }
        return res;
    }
}
