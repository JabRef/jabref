package net.sf.jabref.bibtex;

import net.sf.jabref.gui.InternalBibtexFields;
import net.sf.jabref.Globals;
import net.sf.jabref.exporter.LatexFieldFormatter;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.EntryTypes;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryType;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.function.Predicate;

import com.google.common.base.Strings;
import net.sf.jabref.model.entry.TypedBibEntry;

public class BibEntryWriter {

    private final LatexFieldFormatter fieldFormatter;
    private final boolean write;


    public BibEntryWriter(LatexFieldFormatter fieldFormatter, boolean write) {
        this.fieldFormatter = fieldFormatter;
        this.write = write;
    }

    public void write(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode) throws IOException {
        write(entry, out, bibDatabaseMode, false);
    }
    public void write(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode, Boolean reformat) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!reformat && !entry.hasChanged()) {
            out.write(entry.getParsedSerialization());
            return;
        }
        out.write(Globals.NEWLINE);
        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out, bibDatabaseMode);
        out.write(Globals.NEWLINE);
    }

    public void writeWithoutPrependedNewlines(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!entry.hasChanged()) {
            out.write(entry.getParsedSerialization().trim());
            return;
        }

        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out, bibDatabaseMode);
    }

    /**
     * Write fields in the order of requiredFields, optionalFields and other fields, but does not sort the fields.
     *
     * @param entry
     * @param out
     * @throws IOException
     */
    private void writeRequiredFieldsFirstRemainingFieldsSecond(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode) throws IOException {
        // Write header with type and bibtex-key.
        TypedBibEntry typedEntry = new TypedBibEntry(entry, Optional.empty(), bibDatabaseMode);
        out.write('@' + typedEntry.getTypeForDisplay() + '{');

        writeKeyField(entry, out);

        HashSet<String> written = new HashSet<>();
        written.add(BibEntry.KEY_FIELD);
        boolean hasWritten = false;
        int indentation = getLengthOfLongestFieldName(entry);

        EntryType type = EntryTypes.getTypeOrDefault(entry.getType(), bibDatabaseMode);

        // Write required fields first.
        List<String> fields = type.getRequiredFieldsFlat();
        if (fields != null) {
            for (String value : fields) {
                hasWritten = hasWritten | writeField(entry, out, value, hasWritten, indentation);
                written.add(value);
            }
        }
        // Then optional fields.
        fields = type.getOptionalFields();
        if (fields != null) {
            for (String value : fields) {
                if (!written.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    hasWritten = hasWritten | writeField(entry, out, value, hasWritten, indentation);
                    written.add(value);
                }
            }
        }
        // Then write remaining fields in alphabetic order.
        TreeSet<String> remainingFields = new TreeSet<>();
        for (String key : entry.getFieldNames()) {
            boolean writeIt = write ? InternalBibtexFields.isWriteableField(key) :
                    InternalBibtexFields.isDisplayableField(key);
            if (!written.contains(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        for (String field : remainingFields) {
            hasWritten = hasWritten | writeField(entry, out, field, hasWritten, indentation);
        }

        // Finally, end the entry.
        out.write((hasWritten ? Globals.NEWLINE : "") + '}');
    }

    private void writeKeyField(BibEntry entry, Writer out) throws IOException {
        String keyField = StringUtil.shaveString(entry.getCiteKey());
        out.write(keyField + ',' + Globals.NEWLINE);
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
    private boolean writeField(BibEntry entry, Writer out, String name, boolean prependWhiteSpace, int indentation) throws IOException {
        String field = entry.getField(name);
        // only write field if is is not empty or if empty fields should be included
        // the first condition mirrors mirror behavior of com.jgoodies.common.base.Strings.isNotBlank(str)
        if (Strings.nullToEmpty(field).trim().isEmpty()) {
            return false;
        } else {
            if (prependWhiteSpace) {
                out.write(',' + Globals.NEWLINE);
            }

            out.write("  " + getFieldDisplayName(name, indentation));

            try {
                out.write(fieldFormatter.format(field, name));
            } catch (IOException ex) {
                throw new IOException("Error in field '" + name + "': " + ex.getMessage());
            }
            return true;
        }
    }

    private int getLengthOfLongestFieldName(BibEntry entry) {
        Predicate<String> isNotBibtexKey = field -> !"bibtexkey".equals(field);
        return entry.getFieldNames().stream().filter(isNotBibtexKey).mapToInt(field -> field.length()).max().orElse(0);
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
    private String getFieldDisplayName(String field, int intendation) {
        if (field.isEmpty()) {
            // hard coded "UNKNOWN" is assigned to a field without any name
            field = "UNKNOWN";
        }

        StringBuilder suffixSB = new StringBuilder();

        for (int i = (intendation - field.length()); i > 0; i--) {
            suffixSB.append(' ');
        }

        String suffix = suffixSB.toString();

        String result;
        result = field.toLowerCase() + " = " + suffix;

        return result;
    }
}
