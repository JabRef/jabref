package org.jabref.gui.fieldeditors;

import java.util.Comparator;
import java.util.stream.Collectors;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;
import jakarta.inject.Inject;

public class KeywordsEditor extends HBox implements FieldEditorFX {

    @FXML private TagsField<Keyword> keywordTagsField;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;

    private final KeywordsEditorViewModel viewModel;

    public KeywordsEditor(Field field,
                          SuggestionProvider<?> suggestionProvider,
                          FieldCheckers fieldCheckers) {

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new KeywordsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                preferencesService,
                undoManager);

        keywordTagsField.setCellFactory(new ViewModelListCellFactory<Keyword>().withText(Keyword::toString));
        keywordTagsField.setTagViewFactory(this::createTag);

        keywordTagsField.setSuggestionProvider(request ->
                suggestionProvider.getPossibleSuggestions().stream()
                                  .filter(suggestion -> suggestion.toString()
                                                                  .toLowerCase()
                                                                  .contains(request.getUserText().toLowerCase()))
                                  .map(suggestion -> new Keyword(suggestion.toString()))
                                  .distinct()
                                  .collect(Collectors.toList()));

        keywordTagsField.setConverter(viewModel.getStringConverter());
        keywordTagsField.setMatcher((keyword, searchText) -> keyword.get().toLowerCase().startsWith(searchText.toLowerCase()));
        keywordTagsField.setComparator(Comparator.comparing(Keyword::get));

        keywordTagsField.setNewItemProducer(searchText -> viewModel.getStringConverter().fromString(searchText));

        keywordTagsField.setPlaceholder(null);
        keywordTagsField.setShowSearchIcon(false);
        keywordTagsField.getEditor().getStyleClass().clear();
        keywordTagsField.getEditor().getStyleClass().add("tags-field-editor");

        Bindings.bindContentBidirectional(keywordTagsField.getTags(), viewModel.keywordListProperty());
    }

    private Node createTag(Keyword keyword) {
        Label tagLabel = new Label();
        tagLabel.setText(keyword.get());
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> keywordTagsField.removeTags(keyword));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        return tagLabel;
    }

    public KeywordsEditorViewModel getViewModel() {
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

    @Override
    public void requestFocus() {
        keywordTagsField.requestFocus();
    }

    @Override
    public double getWeight() {
        return 2;
    }
}
