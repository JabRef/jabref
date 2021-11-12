package org.jabref.logic.bibtex;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jabref.logic.TypedBibEntry;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.util.OS;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.strings.StringUtil;

public class BibEntryWriter {

    private final BibEntryTypesManager entryTypesManager;
    private final FieldWriter fieldWriter;

    public BibEntryWriter(FieldWriter fieldWriter, BibEntryTypesManager entryTypesManager) {
        this.fieldWriter = fieldWriter;
        this.entryTypesManager = entryTypesManager;
    }

    public String serializeAll(List<BibEntry> entries, BibDatabaseMode databaseMode) throws IOException {
        StringWriter writer = new StringWriter();
        BibWriter bibWriter = new BibWriter(writer, OS.NEWLINE);
        for (BibEntry entry : entries) {
            write(entry, bibWriter, databaseMode);
        }
        return writer.toString();
    }

    public void write(BibEntry entry, BibWriter out, BibDatabaseMode bibDatabaseMode) throws IOException {
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
    public void write(BibEntry entry, BibWriter out, BibDatabaseMode bibDatabaseMode, Boolean reformat) throws IOException {
        // if the entry has not been modified, write it as it was
        if (!reformat && !entry.hasChanged()) {
            out.write(entry.getParsedSerialization());
            out.finishBlock();
            return;
        }

        writeUserComments(entry, out);
        writeRequiredFieldsFirstRemainingFieldsSecond(entry, out, bibDatabaseMode);
        out.finishBlock();
    }

    private void writeUserComments(BibEntry entry, BibWriter out) throws IOException {
        String userComments = entry.getUserComments();

        if (!userComments.isEmpty()) {
            out.write(userComments);
            // ensure that a line break appears after the comment
            out.finishLine();
        }
    }

    /**
     * Writes fields in the order of requiredFields, optionalFields and other fields, but does not sort the fields.
     */
    private void writeRequiredFieldsFirstRemainingFieldsSecond(BibEntry entry, BibWriter out,
                                                               BibDatabaseMode bibDatabaseMode) throws IOException {
        // Write header with type and bibtex-key
        TypedBibEntry typedEntry = new TypedBibEntry(entry, bibDatabaseMode);
        out.write('@' + typedEntry.getTypeForDisplay() + '{');

        writeKeyField(entry, out);

        Set<Field> written = new HashSet<>();
        written.add(InternalField.KEY_FIELD);
        int indentation = getLengthOfLongestFieldName(entry);

        Optional<BibEntryType> type = entryTypesManager.enrich(entry.getType(), bibDatabaseMode);
        if (type.isPresent()) {
            // Write required fields first
            List<Field> requiredFields = type.get()
                                             .getRequiredFields()
                                             .stream()
                                             .flatMap(Collection::stream)
                                             .sorted(Comparator.comparing(Field::getName))
                                             .collect(Collectors.toList());

            for (Field field : requiredFields) {
                writeField(entry, out, field, indentation);
            }

            // Then optional fields
            List<Field> optionalFields = type.get()
                                             .getOptionalFields()
                                             .stream()
                                             .map(BibField::getField)
                                             .sorted(Comparator.comparing(Field::getName))
                                             .collect(Collectors.toList());

            for (Field field : optionalFields) {
                writeField(entry, out, field, indentation);
            }

            written.addAll(requiredFields);
            written.addAll(optionalFields);
        }
        // Then write remaining fields in alphabetic order.
        SortedSet<Field> remainingFields = entry.getFields()
                                                .stream()
                                                .filter(key -> !written.contains(key))
                                                .collect(Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(Field::getName))));

        for (Field field : remainingFields) {
            writeField(entry, out, field, indentation);
        }

        // Finally, end the entry.
        out.writeLine("}");
    }

    private void writeKeyField(BibEntry entry, BibWriter out) throws IOException {
        String keyField = StringUtil.shaveString(entry.getCitationKey().orElse(""));
        out.writeLine(keyField + ',');
    }

    /**
     * Write a single field, if it has any content.
     *
     * @param entry the entry to write
     * @param out   the target of the write
     * @param field the field
     * @throws IOException In case of an IO error
     */
    private void writeField(BibEntry entry, BibWriter out, Field field, int indentation) throws IOException {
        Optional<String> value = entry.getField(field);
        // only write field if it is not empty
        // field.ifPresent does not work as an IOException may be thrown
        if (value.isPresent() && !value.get().trim().isEmpty()) {
            out.write("  " + getFormattedFieldName(field, indentation));
            try {
                out.write(fieldWriter.write(field, value.get()));
            } catch (InvalidFieldValueException ex) {
                throw new IOException("Error in field '" + field + " of entry " + entry.getCitationKey().orElse("") + "': " + ex.getMessage(), ex);
            }
            out.writeLine(",");
        }
    }

    private int getLengthOfLongestFieldName(BibEntry entry) {
        Predicate<Field> isNotCitationKey = field -> !InternalField.KEY_FIELD.equals(field);
        return entry.getFields()
                    .stream()
                    .filter(isNotCitationKey)
                    .mapToInt(field -> field.getName().length())
                    .max()
                    .orElse(0);
    }

    /**
     * Get display version of a entry field.
     * <p>
     * BibTeX is case-insensitive therefore there is no difference between: howpublished, HOWPUBLISHED, HowPublished, etc.
     * <p>
     * There was a long discussion about how JabRef should write the fields. See https://github.com/JabRef/jabref/issues/116
     * <p>
     * The team decided to do the biblatex way and use lower case for the field names.
     *
     * @param field The name of the field.
     * @return The display version of the field name.
     */
    private String getFormattedFieldName(Field field, int intention) {
        String fieldName = field.getName();
        return fieldName.toLowerCase(Locale.ROOT) + StringUtil.repeatSpaces(intention - fieldName.length()) + " = ";
    }
}
