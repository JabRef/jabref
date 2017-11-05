package org.jabref.gui.entryeditor;

import java.util.Collection;
import java.util.List;

import org.jabref.gui.IconTheme;
import org.jabref.gui.autocompleter.SuggestionProviders;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryType;

public class UserDefinedFieldsTab extends FieldsEditorTab {
    private final List<String> fields;

    public UserDefinedFieldsTab(String name, List<String> fields, BibDatabaseContext databaseContext, SuggestionProviders suggestionProviders) {
        super(false, databaseContext, suggestionProviders);
        this.fields = fields;

        setText(name);
        setGraphic(IconTheme.JabRefIcon.OPTIONAL.getGraphicNode());
    }

    @Override
    protected Collection<String> determineFieldsToShow(BibEntry entry, EntryType entryType) {
        return fields;
    }
}
