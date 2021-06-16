package org.jabref.gui.fieldeditors;

import java.util.Arrays;
import java.util.List;

import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.StringUtil;

public class MonthEditorViewModel extends OptionEditorViewModel<Month> {
    private BibDatabaseMode databaseMode;

    public MonthEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, BibDatabaseMode databaseMode, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);
        this.databaseMode = databaseMode;
    }

    @Override
    public StringConverter<Month> getStringConverter() {
        return new StringConverter<Month>() {
            @Override
            public String toString(Month object) {
                if (object == null) {
                    return null;
                } else {
                    if (databaseMode == BibDatabaseMode.BIBLATEX) {
                        return String.valueOf(object.getNumber());
                    } else {
                        return object.getJabRefFormat();
                    }
                }
            }

            @Override
            public Month fromString(String string) {
                if (StringUtil.isNotBlank(string)) {
                    return Month.parse(string).orElse(null);
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public List<Month> getItems() {
        return Arrays.asList(Month.values());
    }

    @Override
    public String convertToDisplayText(Month object) {
        return object.getFullName();
    }
}
