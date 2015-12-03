package net.sf.jabref.gui.maintable;

import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.model.entry.EntryUtil;

import java.util.Optional;

public class MainTableColumn {

    private String columnName;

    private Optional<String[]> bibtexFields;

    public MainTableColumn(String columnName) {
        this.columnName = columnName;
        this.bibtexFields = Optional.empty();
    }

    public MainTableColumn(String columnName, String[] bibtexFields) {
        this.columnName = columnName;
        this.bibtexFields = Optional.of(bibtexFields);
    }

    /**
     * Get the table column name to be displayed in the UI
     *
     * TODO: use JLabel to be able to display Iconcols?
     *
     * @return
     */
    public String getDisplayName() {
        if(bibtexFields.isPresent()) {
            String[] fields = bibtexFields.get();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < fields.length; i++) {
                if (i > 0) {
                    builder.append(MainTableFormat.COL_DEFINITION_FIELD_SEPARATOR);
                }
                String fieldDisplayName = BibtexFields.getFieldDisplayName(fields[i]);
                if (fieldDisplayName != null) {
                    builder.append(fieldDisplayName);
                } else {
                    builder.append(EntryUtil.capitalizeFirst(fields[i]));
                }
            }
            return builder.toString();
        } else {
            return columnName;
        }
    }

    /**
     * Checks whether the column should display names
     * Relevant as name value format can be formatted.
     *
     * @return true if the bibtex fields contains author or editor
     */
    public boolean isNameColumn() {
        if(bibtexFields.isPresent()) {
            for(String field : bibtexFields.get()) {
                if("author".equals(field) || "editor".equals(field)) {
                    return true;
                }
            }
        }

        return false;
    }

    public String getColumnName() {
        return columnName;
    }

    public Optional<String[]> getBibtexFields() {
        return bibtexFields;
    }



}
