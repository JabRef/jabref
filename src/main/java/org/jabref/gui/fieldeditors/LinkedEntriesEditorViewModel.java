package org.jabref.gui.fieldeditors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.EntryLinkList;
import org.jabref.model.entry.ParsedEntryLink;

public class LinkedEntriesEditorViewModel extends AbstractEditorViewModel {

    private final BibDatabaseContext databaseContext;
    private final ListProperty<ParsedEntryLink> linkedEntries;

    public LinkedEntriesEditorViewModel(BibDatabaseContext databaseContext) {
        this.databaseContext = databaseContext;
        linkedEntries = new SimpleListProperty<>(FXCollections.observableArrayList());
        BindingsHelper.bindContentBidirectional(
                linkedEntries,
                text,
                EntryLinkList::serialize,
                newText -> EntryLinkList.parse(newText, databaseContext.getDatabase()));
    }

    public ListProperty<ParsedEntryLink> linkedEntriesProperty() {
        return linkedEntries;
    }

    public StringConverter<ParsedEntryLink> getStringConverter() {
        return new StringConverter<ParsedEntryLink>() {
            @Override
            public String toString(ParsedEntryLink linkedEntry) {
                if (linkedEntry == null) {
                    return "";
                }
                return linkedEntry.getKey();
            }

            @Override
            public ParsedEntryLink fromString(String key) {
                return databaseContext.getDatabase().getEntryByKey(key).map(ParsedEntryLink::new).orElse(null);
            }
        };
    }

    public void jumpToEntry(ParsedEntryLink parsedEntryLink) {
        // TODO: Implement jump to entry
        //parsedEntryLink.getLinkedEntry().ifPresent(
        //        e -> frame.getCurrentBasePanel().highlightEntry(e)
        //);
    }
}
