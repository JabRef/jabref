package org.jabref.gui.fieldeditors;

import java.util.stream.Collectors;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import com.jfoenix.controls.JFXChip;
import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXDefaultChip;

public class LinkedEntriesEditor extends HBox implements FieldEditorFX {

    @FXML
    private final LinkedEntriesEditorViewModel viewModel;
    @FXML
    private JFXChipView<ParsedEntryLink> chipView;

    public LinkedEntriesEditor(Field field, BibDatabaseContext databaseContext, SuggestionProvider<BibEntry> suggestionProvider, FieldCheckers fieldCheckers) {
        this.viewModel = new LinkedEntriesEditorViewModel(field, suggestionProvider, databaseContext, fieldCheckers);

        ViewLoader.view(this)
                  .root(this)
                  .load();

        chipView.setConverter(viewModel.getStringConverter());
        var autoCompletionItemFactory = new ViewModelListCellFactory<ParsedEntryLink>()
                .withText(ParsedEntryLink::getKey);
        chipView.getAutoCompletePopup().setSuggestionsCellFactory(autoCompletionItemFactory);
        chipView.getAutoCompletePopup().setCellLimit(5);
        chipView.getSuggestions().addAll(suggestionProvider.getPossibleSuggestions().stream().map(ParsedEntryLink::new).collect(Collectors.toList()));

        chipView.setChipFactory((view, item) -> {
            JFXChip<ParsedEntryLink> chip = new JFXDefaultChip<>(view, item);
            chip.setOnMouseClicked(event -> viewModel.jumpToEntry(item));
            return chip;
        });

        Bindings.bindContentBidirectional(chipView.getChips(), viewModel.linkedEntriesProperty());
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
