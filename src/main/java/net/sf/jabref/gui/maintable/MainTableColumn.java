package net.sf.jabref.gui.maintable;

import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.model.entry.EntryUtil;

import javax.swing.*;
import java.util.*;

public class MainTableColumn {

    private final String columnName;

    private final List<String> bibtexFields;

    private final boolean isIconColumn;

    private final JLabel iconLabel;

    private final BibtexDatabase database;

    public MainTableColumn(String columnName) {
        this.columnName = columnName;
        this.bibtexFields = Collections.emptyList();
        this.isIconColumn = false;
        this.iconLabel = null;
        this.database = null;
    }

    public MainTableColumn(String columnName, String[] bibtexFields, BibtexDatabase database) {
        this.columnName = columnName;
        this.bibtexFields = Collections.unmodifiableList(Arrays.asList(bibtexFields));
        this.isIconColumn = false;
        this.iconLabel = null;
        this.database = database;
    }

    public MainTableColumn(String columnName, String[] bibtexFields, JLabel iconLabel) {
        this.columnName = columnName;
        this.bibtexFields = Collections.unmodifiableList(Arrays.asList(bibtexFields));
        this.isIconColumn = true;
        this.iconLabel = iconLabel;
        this.database = null;
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
        for(String field : bibtexFields) {
            String fieldDisplayName = BibtexFields.getFieldDisplayName(field);
            if (fieldDisplayName != null) {
                joiner.add(fieldDisplayName);
            } else {
                joiner.add(EntryUtil.capitalizeFirst(field));
            }
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

    public Object getColumnValue(BibtexEntry entry) {
        if(bibtexFields.isEmpty()) {
            return null;
        }

        String content = null;
        for (String field : bibtexFields) {
            if (field.equals(BibtexEntry.TYPE_HEADER)) {
                content = entry.getType().getName();
            } else {
                content = entry.getFieldOrAlias(field);
                if ((database != null) && "Author".equalsIgnoreCase(columnName) && (content != null)) {
                    content = database.resolveForStrings(content);
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
            return iconLabel;
        } else {
            return new JLabel(getDisplayName());
        }
    }
}
