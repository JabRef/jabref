package org.jabref.gui.fieldeditors;

import java.util.Comparator;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;

public class LinkedEntriesEditor extends HBox implements FieldEditorFX {

    @FXML private final LinkedEntriesEditorViewModel viewModel;

    private final SuggestionProvider<BibEntry> suggestionProvider;

    public LinkedEntriesEditor(Field field, BibDatabaseContext databaseContext, SuggestionProvider<BibEntry> suggestionProvider, FieldCheckers fieldCheckers) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new LinkedEntriesEditorViewModel(field, suggestionProvider, databaseContext, fieldCheckers);
        this.suggestionProvider = suggestionProvider;

        ParsedEntryLinkField entryLinkField = new ParsedEntryLinkField();
        HBox.setHgrow(entryLinkField, Priority.ALWAYS);
        Bindings.bindContentBidirectional(entryLinkField.getTags(), viewModel.linkedEntriesProperty());

        getChildren().add(entryLinkField);
    }

    public LinkedEntriesEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
    }

    @Override
    public Parent getNode() {
        return this;
    }

    public class ParsedEntryLinkField extends TagsField<ParsedEntryLink> {
        public ParsedEntryLinkField() {
            super();

            setCellFactory(new ViewModelListCellFactory<ParsedEntryLink>().withText(ParsedEntryLink::getKey));
            // Mind the .collect(Collectors.toList()) as the list needs to be mutable
            setSuggestionProvider(request ->
                    suggestionProvider.getPossibleSuggestions().stream()
                                      .filter(suggestion -> suggestion.getCitationKey().orElse("").toLowerCase()
                                                                      .contains(request.getUserText().toLowerCase()))
                                      .map(ParsedEntryLink::new).collect(Collectors.toList()));
            setConverter(viewModel.getStringConverter());
            setMatcher((entryLink, searchText) -> entryLink.getKey().toLowerCase().startsWith(searchText.toLowerCase()));
            setComparator(Comparator.comparing(ParsedEntryLink::getKey));

            setTagViewFactory((entryLink) -> {
                Label tagLabel = new Label();
                tagLabel.setText(getConverter().toString(entryLink));
                tagLabel.setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2) {
                        viewModel.jumpToEntry(entryLink);
                    }
                });
                return tagLabel;
            });
        }
    }
}
