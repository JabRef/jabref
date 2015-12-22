package net.sf.jabref.bibtex;

import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

import com.google.common.base.Strings;
import net.sf.jabref.model.entry.EntryType;

public class BibEntryWriter {

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


    public BibEntryWriter(LatexFieldFormatter fieldFormatter, boolean write) {
        this.fieldFormatter = fieldFormatter;
        this.write = write;
    }

    public void write(BibEntry entry, Writer out) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!entry.hasChanged()) {
            out.write(entry.getParsedSerialization());
            return;
        }
        out.write(Globals.NEWLINE + Globals.NEWLINE);

        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out);
    }

    /**
     * Write fields in the order of requiredFields, optionalFields and other fields, but does not sort the fields.
     *
     * @param entry
     * @param out
     * @throws IOException
     */
    private void writeRequiredFieldsFirstRemainingFieldsSecond(BibEntry entry, Writer out) throws IOException {
        // Write header with type and bibtex-key.
        out.write('@' + entry.getType().getName().toUpperCase(Locale.US) + '{');

        writeKeyField(entry, out);

        HashSet<String> written = new HashSet<>();
        written.add(BibEntry.KEY_FIELD);
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
        out.write((hasWritten ? Globals.NEWLINE : "") + '}');
    }

    private void writeKeyField(BibEntry entry, Writer out) throws IOException {
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
    private boolean writeField(BibEntry entry, Writer out, String name, boolean prependWhiteSpace) throws IOException {
        String field = entry.getField(name);
        // only write field if is is not empty or if empty fields should be included
        // the first condition mirrors mirror behavior of com.jgoodies.common.base.Strings.isNotBlank(str)
        if (!Strings.nullToEmpty(field).trim().isEmpty()) {
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
     * howpublished, HOWPUBLISHED, HowPublished, etc.
     * <p>
     * The was a long discussion about how JabRef should write the fields.
     * See https://github.com/JabRef/jabref/issues/116
     * <p>
     * The team decided to do the biber way and use lower case for the field names.
     *
     * @param field The name of the field.
     * @return The display version of the field name.
     */
    private String getFieldDisplayName(String field) {
        if (field.isEmpty()) {
            // hard coded "UNKNOWN" is assigned to a field without any name
            field = "UNKNOWN";
        }

        StringBuilder suffixSB = new StringBuilder();

        for (int i = BibEntryWriter.maxFieldLength - field.length(); i > 0; i--) {
            suffixSB.append(" ");
        }

        String suffix = suffixSB.toString();

        String result;
        result = field.toLowerCase() + suffix;

        return result;
    }
}
