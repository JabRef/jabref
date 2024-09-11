package org.jabref.gui.fieldeditors;

import java.util.Comparator;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeywordsEditor extends HBox implements FieldEditorFX {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeywordsEditor.class);

    @FXML private TagsField<Keyword> keywordTagsField;

    @Inject private PreferencesService preferencesService;
    @Inject private DialogService dialogService;
    @Inject private UndoManager undoManager;
    @Inject private ClipBoardManager clipBoardManager;

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

        keywordTagsField.setCellFactory(new ViewModelListCellFactory<Keyword>().withText(Keyword::get));
        keywordTagsField.setTagViewFactory(this::createTag);

        keywordTagsField.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        keywordTagsField.setConverter(viewModel.getStringConverter());
        keywordTagsField.setMatcher((keyword, searchText) -> keyword.get().toLowerCase().startsWith(searchText.toLowerCase()));
        keywordTagsField.setComparator(Comparator.comparing(Keyword::get));

        keywordTagsField.setNewItemProducer(searchText -> viewModel.getStringConverter().fromString(searchText));

        keywordTagsField.setShowSearchIcon(false);
        keywordTagsField.getEditor().getStyleClass().clear();
        keywordTagsField.getEditor().getStyleClass().add("tags-field-editor");

        String keywordSeparator = String.valueOf(viewModel.getKeywordSeparator());
        keywordTagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getText().equals(keywordSeparator)) {
                keywordTagsField.commit();
                event.consume();
            }
        });

        Bindings.bindContentBidirectional(keywordTagsField.getTags(), viewModel.keywordListProperty());
    }

    private Node createTag(Keyword keyword) {
        Label tagLabel = new Label();
        tagLabel.setText(keywordTagsField.getConverter().toString(keyword));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> keywordTagsField.removeTags(keyword));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new KeywordsEditor.TagContextAction(StandardActions.COPY, keyword)),
                factory.createMenuItem(StandardActions.CUT, new KeywordsEditor.TagContextAction(StandardActions.CUT, keyword)),
                factory.createMenuItem(StandardActions.DELETE, new KeywordsEditor.TagContextAction(StandardActions.DELETE, keyword))
        );
        tagLabel.setContextMenu(contextMenu);
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

    private class TagContextAction extends SimpleCommand {
        private final StandardActions command;
        private final Keyword keyword;

        public TagContextAction(StandardActions command, Keyword keyword) {
            this.command = command;
            this.keyword = keyword;
        }

        @Override
        public void execute() {
            switch (command) {
                case COPY -> {
                    clipBoardManager.setContent(keyword.get());
                    dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                            JabRefDialogService.shortenDialogMessage(keyword.get())));
                }
                case CUT -> {
                    clipBoardManager.setContent(keyword.get());
                    dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                            JabRefDialogService.shortenDialogMessage(keyword.get())));
                    keywordTagsField.removeTags(keyword);
                }
                case DELETE ->
                        keywordTagsField.removeTags(keyword);
                default ->
                        LOGGER.info("Action {} not defined", command.getText());
            }
        }
    }
}
