package net.sf.jabref.logic.bibtex;

import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.FieldFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.BibtexEntryType;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

public class BibtexEntryWriter {

    /**
     * Display name map for entry field names.
     */
    private static final Map<String, String> tagDisplayNameMap = new HashMap<>();

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
        for (BibtexEntryType type : BibtexEntryType.getAllValues()) {
            if (type.getRequiredFields() != null) {
                for (String field : type.getRequiredFields()) {
                    max = Math.max(max, field.length());
                }
            }
            if (type.getOptionalFields() != null) {
                for (String field : type.getOptionalFields()) {
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
    private final int writeFieldSortStyle = Globals.prefs.getInt(JabRefPreferences.WRITEFIELD_SORTSTYLE);


    public BibtexEntryWriter(FieldFormatter fieldFormatter, boolean write) {
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
        String[] requiredFields = entry.getRequiredFields();
        if (requiredFields != null) {
            Arrays.sort(requiredFields); // Sorting in alphabetic order.
            for (String value : requiredFields) {
                if (!writtenFields.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                    writtenFields.add(value);
                }
            }
        }
        // Then optional fields.
        requiredFields = entry.getOptionalFields();

        if (requiredFields != null) {
            Arrays.sort(requiredFields); // Sorting in alphabetic order.
            for (String value : requiredFields) {
                if (!writtenFields.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten);
                    writtenFields.add(value);

                }
            }
        }
        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<>();
        for (String key : entry.getAllFields()) {
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
        String[] fields = entry.getRequiredFields();
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
        for (String key : entry.getAllFields()) {
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
        String[] fields = entry.getUserDefinedFields();
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
        for (String key : entry.getAllFields()) {
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
        String keyField = StringUtil.shaveString(entry.getField(BibtexEntry.KEY_FIELD));
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
        if (field != null || includeEmptyFields) {
            if (prependWhiteSpace) {
                out.write(',' + Globals.NEWLINE);
            }

            out.write("  " + getFieldDisplayName(name) + " = ");

            try {
                out.write(fieldFormatter.format(field, name));
            } catch (IOException ex) {
                throw new IOException(Localization.lang("Error in field") + " '" + name + "': " + ex.getMessage());
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
