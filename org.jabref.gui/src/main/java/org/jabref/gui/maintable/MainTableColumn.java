package org.jabref.gui.maintable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import javax.swing.JLabel;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;

public class MainTableColumn {

    private final String columnName;

    private final List<String> bibtexFields;

    private final boolean isIconColumn;

    private final Optional<JLabel> iconLabel;

    private final Optional<BibDatabase> database;

    private final LayoutFormatter toUnicode = new LatexToUnicodeFormatter();

    public MainTableColumn(String columnName) {
        this.columnName = columnName;
        this.bibtexFields = Collections.emptyList();
        this.isIconColumn = false;
        this.iconLabel = Optional.empty();
        this.database = Optional.empty();
    }

    public MainTableColumn(String columnName, List<String> bibtexFields, BibDatabase database) {
        this.columnName = columnName;
        this.bibtexFields = Collections.unmodifiableList(bibtexFields);
        this.isIconColumn = false;
        this.iconLabel = Optional.empty();
        this.database = Optional.of(database);
    }

    public MainTableColumn(String columnName, List<String> bibtexFields, JLabel iconLabel) {
        this.columnName = columnName;
        this.bibtexFields = Collections.unmodifiableList(bibtexFields);
        this.isIconColumn = true;
        this.iconLabel = Optional.of(iconLabel);
        this.database = Optional.empty();
    }

    /**
     * Get the table column name to be displayed in the UI
     *
     * @return name to be displayed. null if field is empty.
     */
    public String getDisplayName() {
        if (bibtexFields.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(FieldName.FIELD_SEPARATOR);
        for (String field : bibtexFields) {
            joiner.add(field);
        }
        return joiner.toString();
    }

    public String getColumnName() {
        return columnName;
    }

    public List<String> getBibtexFields() {
        return bibtexFields;
    }

    public boolean isIconColumn() {
        return isIconColumn;
    }

    public boolean isFileFilter() {
        return false; // Overridden in SpecialMainTableColumns for file filter columns
    }

    public Object getColumnValue(BibEntry entry) {
        if (bibtexFields.isEmpty()) {
            return null;
        }
        boolean isNameColumn = false;

        Optional<String> content = Optional.empty();
        for (String field : bibtexFields) {
            content = entry.getResolvedFieldOrAlias(field, database.orElse(null));
            if (content.isPresent()) {
                isNameColumn = InternalBibtexFields.getFieldProperties(field).contains(FieldProperty.PERSON_NAMES);
                break;
            }
        }

        String result = content.orElse(null);

        if (isNameColumn) {
            result = toUnicode.format(MainTableNameFormatter.formatName(result));
        }

        if (result != null && !BibEntry.KEY_FIELD.equals(columnName)) {
            result = toUnicode.format(result).trim();
        }

        return result;
    }

    public JLabel getHeaderLabel() {
        if (isIconColumn) {
            return iconLabel.get();
        } else {
            return new JLabel(getDisplayName());
        }
    }

    /**
     * Check if the value returned by getColumnValue() is the same as a simple check of the entry's field(s) would give
     * The reasons for being different are (combinations may also happen):
     * - The entry has a crossref where the field content is obtained from
     * - The field has a string in it (which getColumnValue() resolves)
     * - There are some alias fields. For example, if the entry has a date field but no year field,
     *   {@link BibEntry#getResolvedFieldOrAlias(String, BibDatabase)} will return the year value from the date field
     *   when queried for year
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
        for (String field : bibtexFields) {
            // entry type or bibtex key will never be resolved
            if (BibEntry.TYPE_HEADER.equals(field) || BibEntry.OBSOLETE_TYPE_HEADER.equals(field)
                    || BibEntry.KEY_FIELD.equals(field)) {
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
