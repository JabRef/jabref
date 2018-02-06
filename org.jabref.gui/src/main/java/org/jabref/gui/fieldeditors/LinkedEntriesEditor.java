package org.jabref.gui.fieldeditors;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.autocompleter.AutoCompletionTextInputBinding;
import org.jabref.gui.util.ControlHelper;
import org.jabref.gui.util.component.TagBar;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedEntryLink;

public class LinkedEntriesEditor extends HBox implements FieldEditorFX {

    @FXML private final LinkedEntriesEditorViewModel viewModel;
    @FXML private TagBar<ParsedEntryLink> linkedEntriesBar;

    public LinkedEntriesEditor(String fieldName, BibDatabaseContext databaseContext, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        this.viewModel = new LinkedEntriesEditorViewModel(fieldName, suggestionProvider, databaseContext, fieldCheckers);

        ControlHelper.loadFXMLForControl(this);

        linkedEntriesBar.setStringConverter(viewModel.getStringConverter());
        linkedEntriesBar.setOnTagClicked((parsedEntryLink, mouseEvent) -> viewModel.jumpToEntry(parsedEntryLink));

        AutoCompletionTextInputBinding.autoComplete(linkedEntriesBar.getInputTextField(), viewModel::complete, viewModel.getStringConverter());
        Bindings.bindContentBidirectional(linkedEntriesBar.tagsProperty(), viewModel.linkedEntriesProperty());
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
