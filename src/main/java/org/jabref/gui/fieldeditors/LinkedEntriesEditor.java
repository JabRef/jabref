package org.jabref.gui.fieldeditors;

import java.util.Comparator;
import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;

public class LinkedEntriesEditor extends HBox implements FieldEditorFX {

    @FXML public TagsField<ParsedEntryLink> entryLinkField;

    private final LinkedEntriesEditorViewModel viewModel;

    public LinkedEntriesEditor(Field field, BibDatabaseContext databaseContext, SuggestionProvider<BibEntry> suggestionProvider, FieldCheckers fieldCheckers) {
        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new LinkedEntriesEditorViewModel(field, suggestionProvider, databaseContext, fieldCheckers);

        entryLinkField.setCellFactory(new ViewModelListCellFactory<ParsedEntryLink>().withText(ParsedEntryLink::getKey));
        // Mind the .collect(Collectors.toList()) as the list needs to be mutable
        entryLinkField.setSuggestionProvider(request ->
                suggestionProvider.getPossibleSuggestions().stream()
                                  .filter(suggestion -> suggestion.getCitationKey().orElse("").toLowerCase()
                                                                  .contains(request.getUserText().toLowerCase()))
                                  .map(ParsedEntryLink::new)
                                  .collect(Collectors.toList()));
        entryLinkField.setTagViewFactory(this::createTag);
        entryLinkField.setConverter(viewModel.getStringConverter());
        entryLinkField.setNewItemProducer(searchText -> viewModel.getStringConverter().fromString(searchText));
        entryLinkField.setMatcher((entryLink, searchText) -> entryLink.getKey().toLowerCase().startsWith(searchText.toLowerCase()));
        entryLinkField.setComparator(Comparator.comparing(ParsedEntryLink::getKey));
        entryLinkField.setShowSearchIcon(false);
        entryLinkField.getEditor().getStyleClass().clear();
        entryLinkField.getEditor().getStyleClass().add("tags-field-editor");

        Bindings.bindContentBidirectional(entryLinkField.getTags(), viewModel.linkedEntriesProperty());
    }

    private Node createTag(ParsedEntryLink entryLink) {
        Label tagLabel = new Label();
        tagLabel.setText(entryLinkField.getConverter().toString(entryLink));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> entryLinkField.removeTags(entryLink));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        tagLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                viewModel.jumpToEntry(entryLink);
            }
        });
        return tagLabel;
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
}
