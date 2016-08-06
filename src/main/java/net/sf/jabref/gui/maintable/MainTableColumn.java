package net.sf.jabref.gui.maintable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import javax.swing.JLabel;

import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.layout.LayoutFormatter;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FieldProperties;
import net.sf.jabref.model.entry.InternalBibtexFields;

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

    /**
     * Checks whether the column should display names
     * Relevant as name value format can be formatted.
     *
     * @return true if the bibtex fields contains author or editor
     */
    private boolean isNameColumn() {
        for (String field : bibtexFields) {
            if (InternalBibtexFields.getFieldExtras(field).contains(FieldProperties.PERSON_NAMES)) {
                return true;
            }
        }
        return false;
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

        String content = null;
        for (String field : bibtexFields) {
            if (field.equals(BibEntry.TYPE_HEADER)) {
                // Fancy typesetting
                TypedBibEntry typedEntry = new TypedBibEntry(entry, BibDatabaseMode.BIBLATEX);
                content = typedEntry.getTypeForDisplay();
            } else {
                if (database.isPresent()) {
                    content = BibDatabase.getResolvedField(field, entry, database.get()).orElse(null);
                } else {
                    content = entry.getFieldOrAlias(field).orElse(null);
                }
            }
            if (content != null) {
                break;
            }
        }

        if (isNameColumn()) {
            content = MainTableNameFormatter.formatName(content);
        }

        if (content != null) {
            content = toUnicode.format(content).trim();
        }

        return content;

    }

    public JLabel getHeaderLabel() {
        if (isIconColumn) {
            return iconLabel.get();
        } else {
            return new JLabel(getDisplayName());
        }
    }
}
