package org.jabref.gui.maintable;

import java.util.Optional;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.value.ObservableValue;

import org.jabref.Globals;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.OrFields;

/**
 * A column that displays the text-value of the field
 */
public class FieldColumn extends MainTableColumn<String> {

    private final OrFields bibtexFields;

    private final Optional<BibDatabase> database;

    private final MainTableNameFormatter nameFormatter;

    public FieldColumn(MainTableColumnModel model, OrFields bibtexFields, BibDatabase database) {
        super(model);
        this.bibtexFields = bibtexFields;
        this.database = Optional.of(database);
        this.nameFormatter = new MainTableNameFormatter(Globals.prefs);

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

    private ObservableValue<String> getColumnValue(BibEntryTableViewModel entry) {
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
            return nameFormatter.formatName(result);
        } else {
            return result;
        }
    }
}
