package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.util.OS;
import org.jabref.model.EntryTypes;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.strings.StringUtil;

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

    /**
     * Writes the given BibEntry using the given writer
     *
     * @param entry           The entry to write
     * @param out             The writer to use
     * @param bibDatabaseMode The database mode (bibtex or biblatex)
     * @param reformat        Should the entry be in any case, even if no change occurred?
     */
    public void write(BibEntry entry, Writer out, BibDatabaseMode bibDatabaseMode, Boolean reformat) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!reformat && !entry.hasChanged()) {
            out.write(entry.getParsedSerialization());
            return;
        }

        writeUserComments(entry, out);
        out.write(OS.NEWLINE);
        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out, bibDatabaseMode);
        out.write(OS.NEWLINE);
    }

    private void writeUserComments(BibEntry entry, Writer out) throws IOException {
        String userComments = entry.getUserComments();

        if (!userComments.isEmpty()) {
            out.write(userComments + OS.NEWLINE);
        }
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
    private void writeRequiredFieldsFirstRemainingFieldsSecond(BibEntry entry, Writer out,
                                                               BibDatabaseMode bibDatabaseMode) throws IOException {
        // Write header with type and bibtex-key.
        TypedBibEntry typedEntry = new TypedBibEntry(entry, bibDatabaseMode);
        out.write('@' + typedEntry.getTypeForDisplay() + '{');

        writeKeyField(entry, out);

        Set<String> written = new HashSet<>();
        written.add(BibEntry.KEY_FIELD);
        int indentation = getLengthOfLongestFieldName(entry);

        EntryType type = EntryTypes.getTypeOrDefault(entry.getType(), bibDatabaseMode);

        // Write required fields first.
        Collection<String> fields = type.getRequiredFieldsFlat();
        if (fields != null) {
            for (String value : fields) {
                writeField(entry, out, value, indentation);
                written.add(value);
            }
        }
        // Then optional fields.
        fields = type.getOptionalFields();
        if (fields != null) {
            for (String value : fields) {
                if (!written.contains(value)) { // If field appears both in req. and opt. don't repeat.
                    writeField(entry, out, value, indentation);
                    written.add(value);
                }
            }
        }
        // Then write remaining fields in alphabetic order.
        Set<String> remainingFields = new TreeSet<>();
        for (String key : entry.getFieldNames()) {
            boolean writeIt = write ? InternalBibtexFields.isWriteableField(key) :
                    InternalBibtexFields.isDisplayableField(key);
            if (!written.contains(key) && writeIt) {
                remainingFields.add(key);
            }
        }
        for (String field : remainingFields) {
            writeField(entry, out, field, indentation);
        }

        // Finally, end the entry.
        out.write('}');
    }

    private void writeKeyField(BibEntry entry, Writer out) throws IOException {
        String keyField = StringUtil.shaveString(entry.getCiteKeyOptional().orElse(""));
        out.write(keyField + ',' + OS.NEWLINE);
    }

    /**
     * Write a single field, if it has any content.
     *
     * @param entry the entry to write
     * @param out   the target of the write
     * @param name  The field name
     * @throws IOException In case of an IO error
     */
    private void writeField(BibEntry entry, Writer out, String name, int indentation) throws IOException {
        Optional<String> field = entry.getField(name);
        // only write field if is is not empty
        // field.ifPresent does not work as an IOException may be thrown
        if (field.isPresent() && !field.get().trim().isEmpty()) {
            out.write("  " + getFieldDisplayName(name, indentation));

            try {
                out.write(fieldFormatter.format(field.get(), name));
                out.write(',' + OS.NEWLINE);
            } catch (InvalidFieldValueException ex) {
                throw new IOException("Error in field '" + name + "': " + ex.getMessage(), ex);
            }
        }
    }

    private int getLengthOfLongestFieldName(BibEntry entry) {
        Predicate<String> isNotBibtexKey = field -> !BibEntry.KEY_FIELD.equals(field);
        return entry.getFieldNames().stream().filter(isNotBibtexKey).mapToInt(String::length).max().orElse(0);
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
     * The team decided to do the biblatex way and use lower case for the field names.
     *
     * @param field The name of the field.
     * @return The display version of the field name.
     */
    private String getFieldDisplayName(String field, int intendation) {
        String actualField = field;
        if (actualField.isEmpty()) {
            // hard coded "UNKNOWN" is assigned to a field without any name
            actualField = "UNKNOWN";
        }

        return actualField.toLowerCase(Locale.ROOT) + StringUtil.repeatSpaces(intendation - actualField.length()) + " = ";
    }
}
