package org.jabref.gui.maintable;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Label;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;

public class NormalTableColumn extends MainTableColumn<String> {

    private final OrFields bibtexFields;

    private final boolean isIconColumn;

    private final Optional<JabRefIcon> iconLabel;

    private final Optional<BibDatabase> database;

    private final LayoutFormatter toUnicode = new LatexToUnicodeFormatter();
    private final String columnName;

    public NormalTableColumn(String columnName, OrFields bibtexFields, BibDatabase database) {
        super(columnName);
        this.columnName = columnName;
        this.bibtexFields = bibtexFields;
        this.isIconColumn = false;
        this.iconLabel = Optional.empty();
        this.database = Optional.of(database);
    }

    /**
     * Get the table column name to be displayed in the UI
     *
     * @return name to be displayed. null if field is empty.
     */
    public String getDisplayName() {
        return bibtexFields.getDisplayName();
    }

    @Override
    public ObservableValue<String> getColumnValue(BibEntryTableViewModel entry) {
        if (bibtexFields.isEmpty()) {
            return null;
        }

        ObjectBinding<Field>[] dependencies = bibtexFields.stream().map(entry::getField).toArray(ObjectBinding[]::new);
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

    public Node getHeaderLabel() {
        if (isIconColumn) {
            return iconLabel.map(JabRefIcon::getGraphicNode).get();
        } else {
            return new Label(getDisplayName());
        }
    }

    /**
     * Check if the value returned by getColumnValue() is the same as a simple check of the entry's field(s) would give
     * The reasons for being different are (combinations may also happen): - The entry has a crossref where the field
     * content is obtained from - The field has a string in it (which getColumnValue() resolves) - There are some alias
     * fields. For example, if the entry has a date field but no year field, {@link
     * BibEntry#getResolvedFieldOrAlias(String, BibDatabase)} will return the year value from the date field when
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

    public String getColumnName() {
        return columnName;
    }
}
