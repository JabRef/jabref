package net.sf.jabref.gui.maintable;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.EntryUtil;

import javax.swing.*;
import java.util.*;

public class MainTableColumn {

    private final String columnName;

    private final List<String> bibtexFields;

    private final boolean isIconColumn;

    private final Optional<JLabel> iconLabel;

    private final Optional<BibDatabase> database;

    public MainTableColumn(String columnName) {
        this.columnName = columnName;
        this.bibtexFields = Collections.emptyList();
        this.isIconColumn = false;
        this.iconLabel = Optional.empty();
        this.database = Optional.empty();
    }

    public MainTableColumn(String columnName, String[] bibtexFields, BibDatabase database) {
        this.columnName = columnName;
        this.bibtexFields = Collections.unmodifiableList(Arrays.asList(bibtexFields));
        this.isIconColumn = false;
        this.iconLabel = Optional.empty();
        this.database = Optional.of(database);
    }

    public MainTableColumn(String columnName, String[] bibtexFields, JLabel iconLabel) {
        this.columnName = columnName;
        this.bibtexFields = Collections.unmodifiableList(Arrays.asList(bibtexFields));
        this.isIconColumn = true;
        this.iconLabel = Optional.of(iconLabel);
        this.database = Optional.empty();
    }

    /**
     * Get the table column name to be displayed in the UI
     *
     * @return name to be displayed
     */
    public String getDisplayName() {
        if (bibtexFields.isEmpty()) {
            return null;
        }

        StringJoiner joiner = new StringJoiner(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
        for (String field : bibtexFields) {
            joiner.add(EntryUtil.capitalizeFirst(field));
        }
        return joiner.toString();
    }

    /**
     * Checks whether the column should display names
     * Relevant as name value format can be formatted.
     *
     * @return true if the bibtex fields contains author or editor
     */
    public boolean isNameColumn() {
        return bibtexFields.contains("author") || bibtexFields.contains("editor");
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
        if(bibtexFields.isEmpty()) {
            return null;
        }

        String content = null;
        for (String field : bibtexFields) {
            if (field.equals(BibEntry.TYPE_HEADER)) {
                content = entry.getType().getName();
            } else {
                content = entry.getFieldOrAlias(field);
                if (database.isPresent() && "Author".equalsIgnoreCase(columnName) && (content != null)) {
                    content = database.get().resolveForStrings(content);
                }
            }
            if (content != null) {
                break;
            }
        }

        if (isNameColumn()) {
            return MainTableNameFormatter.formatName(content);
        }
        return content;

    }

    public JLabel getHeaderLabel() {
        if(isIconColumn) {
            return iconLabel.get();
        } else {
            return new JLabel(getDisplayName());
        }
    }
}
