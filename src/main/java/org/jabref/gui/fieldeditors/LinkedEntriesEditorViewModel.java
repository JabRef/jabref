package org.jabref.gui.fieldeditors;

import java.util.Collection;
import java.util.stream.Collectors;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryLinkList;
import org.jabref.model.entry.ParsedEntryLink;

import org.controlsfx.control.textfield.AutoCompletionBinding;

public class LinkedEntriesEditorViewModel extends AbstractEditorViewModel {

    private final BibDatabaseContext databaseContext;
    private final ListProperty<ParsedEntryLink> linkedEntries;

    public LinkedEntriesEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, BibDatabaseContext databaseContext, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        this.databaseContext = databaseContext;
        linkedEntries = new SimpleListProperty<>(FXCollections.observableArrayList());
        BindingsHelper.bindContentBidirectional(
                linkedEntries,
                text,
                EntryLinkList::serialize,
                newText -> EntryLinkList.parse(newText, databaseContext.getDatabase()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public Collection<ParsedEntryLink> complete(AutoCompletionBinding.ISuggestionRequest request) {
        //We have to cast the BibEntries from the BibEntrySuggestionProvider to ParsedEntryLink
        Collection<BibEntry> bibEntries = (Collection<BibEntry>) super.complete(request);
        return bibEntries.stream().map(ParsedEntryLink::new).collect(Collectors.toList());
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
        // TODO: Add toolitp for tag: Localization.lang("Jump to entry")
        // This feature was removed while converting the linked entries editor to JavaFX
        // Right now there is no nice way to re-implement it as we have no good interface to control the focus of the main table
        // (except directly using the JabRefFrame class as below)
        //parsedEntryLink.getLinkedEntry().ifPresent(
        //        e -> frame.getCurrentBasePanel().highlightEntry(e)
        //);
    }

}
