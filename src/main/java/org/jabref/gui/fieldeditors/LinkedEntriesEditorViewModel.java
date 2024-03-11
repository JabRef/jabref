package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.util.StringConverter;

import org.jabref.gui.StateManager;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.EntryLinkList;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.Field;

public class LinkedEntriesEditorViewModel extends AbstractEditorViewModel {

    private final BibDatabaseContext databaseContext;
    private final SuggestionProvider<?> suggestionProvider;
    private final ListProperty<ParsedEntryLink> linkedEntries;
    private final StateManager stateManager;

    public LinkedEntriesEditorViewModel(Field field,
                                        SuggestionProvider<?> suggestionProvider,
                                        BibDatabaseContext databaseContext,
                                        FieldCheckers fieldCheckers,
                                        UndoManager undoManager,
                                        StateManager stateManager) {
        super(field, suggestionProvider, fieldCheckers, undoManager);

        this.databaseContext = databaseContext;
        this.suggestionProvider = suggestionProvider;
        this.stateManager = stateManager;

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

    public List<ParsedEntryLink> getSuggestions(String request) {
        List<ParsedEntryLink> suggestions = suggestionProvider
                .getPossibleSuggestions()
                .stream()
                .map(suggestion -> suggestion instanceof BibEntry bibEntry ? bibEntry.getCitationKey().orElse("") : (String) suggestion)
                .filter(suggestion -> suggestion.toLowerCase(Locale.ROOT).contains(request.toLowerCase(Locale.ROOT)))
                .map(suggestion -> new ParsedEntryLink(suggestion, databaseContext.getDatabase()))
                .distinct()
                .collect(Collectors.toList());

        ParsedEntryLink requestedLink = new ParsedEntryLink(request, databaseContext.getDatabase());
        if (!suggestions.contains(requestedLink)) {
            suggestions.addFirst(requestedLink);
        }

        return suggestions;
    }

    public void jumpToEntry(ParsedEntryLink parsedEntryLink) {
        parsedEntryLink.getLinkedEntry().ifPresent(entry ->
                stateManager.activeTabProperty().get().ifPresent(tab -> tab.clearAndSelect(entry)));
    }
}
