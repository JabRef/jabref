package org.jabref.gui.maintable;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;

/**
 * A column that displays the text-value of the field
 */
public class FieldColumn extends MainTableColumn<String> {

    private final OrFields bibtexFields;

    private final Optional<BibDatabase> database;

    private final LayoutFormatter toUnicode = new LatexToUnicodeFormatter();

    public FieldColumn(MainTableColumnModel model, OrFields bibtexFields, BibDatabase database) {
        super(model);
        this.bibtexFields = bibtexFields;
        this.database = Optional.of(database);

        setText(getDisplayName());
        setCellValueFactory(param -> getColumnValue(param.getValue()));
    }

    /**
     * Get the table column name to be displayed in the UI
     *
     * @return name to be displayed. null if field is empty.
     */
    @Override
    public String getDisplayName() { return bibtexFields.getDisplayName(); }

    public ObservableValue<String> getColumnValue(BibEntryTableViewModel entry) {
        if (bibtexFields.isEmpty()) {
            return null;
        }

        ObjectBinding[] dependencies = bibtexFields.stream().map(entry::getField).toArray(ObjectBinding[]::new);
        return Bindings.createStringBinding(() -> computeText(entry), dependencies);
    }

    private String computeText(BibEntryTableViewModel entry) {
        boolean isNameColumn = false;

        Optional<String> content = Optional.empty();
        for (Field field : bibtexFields) {
            content = entry.getResolvedFieldOrAlias(field, database.orElse(null));
            if (content.isPresent()) {
                isNameColumn = field.getProperties().contains(FieldProperty.PERSON_NAMES);
                break;
            }
        }

        String result = content.orElse(null);

        if (isNameColumn) {
            result = toUnicode.format(MainTableNameFormatter.formatName(result));
        }

        if ((result != null) && !bibtexFields.contains(InternalField.KEY_FIELD)) {
            result = toUnicode.format(result).trim();
        }
        return result;
    }

    /**
     * Check if the value returned by getColumnValue() is the same as a simple check of the entry's field(s) would give
     * The reasons for being different are (combinations may also happen): - The entry has a crossref where the field
     * content is obtained from - The field has a string in it (which getColumnValue() resolves) - There are some alias
     * fields. For example, if the entry has a date field but no year field, {@link
     * BibEntry#getResolvedFieldOrAlias(Field, BibDatabase)} will return the year value from the date field when
     * queried for year
     *
     * @param entry the BibEntry
     * @return true if the value returned by getColumnValue() is resolved as outlined above
     */
    public boolean isResolved(BibEntry entry) {
        if (bibtexFields.isEmpty()) {
            return false;
        }

        Optional<String> resolvedFieldContent = Optional.empty();
        Optional<String> plainFieldContent = Optional.empty();
        for (Field field : bibtexFields) {
            // entry type or bibtex key will never be resolved
            if (InternalField.TYPE_HEADER.equals(field) || InternalField.OBSOLETE_TYPE_HEADER.equals(field)
                    || InternalField.KEY_FIELD.equals(field)) {
                return false;
            } else {
                plainFieldContent = entry.getField(field);
                resolvedFieldContent = entry.getResolvedFieldOrAlias(field, database.orElse(null));
            }

            if (resolvedFieldContent.isPresent()) {
                break;
            }
        }
        return (!resolvedFieldContent.equals(plainFieldContent));
    }

}
