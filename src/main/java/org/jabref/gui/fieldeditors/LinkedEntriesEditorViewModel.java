package org.jabref.gui.fieldeditors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.EntryLinkList;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.Field;

public class LinkedEntriesEditorViewModel extends AbstractEditorViewModel {

    private final BibDatabaseContext databaseContext;
    private final ListProperty<ParsedEntryLink> linkedEntries;

    public LinkedEntriesEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, BibDatabaseContext databaseContext, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);

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
        return new StringConverter<>() {

            @Override
            public String toString(ParsedEntryLink linkedEntry) {
                if (linkedEntry == null) {
                    return "";
                }
                return linkedEntry.getKey();
            }

            @Override
            public ParsedEntryLink fromString(String key) {
                return new ParsedEntryLink(key, databaseContext.getDatabase());
            }
        };
    }

    public void jumpToEntry(ParsedEntryLink parsedEntryLink) {
        // TODO: Implement jump to entry
        // TODO: Add toolitp for tag: Localization.lang("Jump to entry")
        // This feature was removed while converting the linked entries editor to JavaFX
        // Right now there is no nice way to re-implement it as we have no good interface to control the focus of the main table
        // (except directly using the JabRefFrame class as below)
        // parsedEntryLink.getLinkedEntry().ifPresent(
        //        e -> frame.getCurrentBasePanel().highlightEntry(e)
        // );
    }
}
