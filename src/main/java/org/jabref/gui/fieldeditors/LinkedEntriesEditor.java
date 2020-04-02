package org.jabref.gui.fieldeditors;

import com.jfoenix.controls.JFXChip;
import com.jfoenix.controls.JFXChipView;
import com.jfoenix.controls.JFXDefaultChip;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.HBox;

import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.ParsedEntryLink;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;

import java.util.Collection;

public class LinkedEntriesEditor extends HBox implements FieldEditorFX {

    @FXML
    private final LinkedEntriesEditorViewModel viewModel;
    @FXML
    private JFXChipView<ParsedEntryLink> chipView;

    public LinkedEntriesEditor(Field field, BibDatabaseContext databaseContext, AutoCompleteSuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers) {
        this.viewModel = new LinkedEntriesEditorViewModel(field, suggestionProvider, databaseContext, fieldCheckers);

        ViewLoader.view(this)
                .root(this)
                .load();

        chipView.setConverter(viewModel.getStringConverter());
        chipView.getSuggestions().addAll((Collection<? extends ParsedEntryLink>) suggestionProvider.getPossibleSuggestions());

            chipView.setChipFactory(
                    (view, item) -> {
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
