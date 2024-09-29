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
import javafx.scene.control.MenuItem;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.TransferMode;
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
    private Keyword draggedKeyword;

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
        keywordTagsField.getEditor().setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == KeyCode.V) {
                handlePasteAction();  // Call paste handler
            }
        });
        keywordTagsField.getEditor().setOnContextMenuRequested(event -> {
            ContextMenu contextMenu = new ContextMenu();
            MenuItem pasteItem = new MenuItem("Paste");
            pasteItem.setOnAction(e -> handlePasteAction());
            contextMenu.getItems().add(pasteItem);
            contextMenu.show(keywordTagsField.getEditor(), event.getScreenX(), event.getScreenY());
        });
        Bindings.bindContentBidirectional(keywordTagsField.getTags(), viewModel.keywordListProperty());
    }

    private void handlePasteAction() {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        if (clipboard.hasString()) {
            String pastedText = clipboard.getString().trim();

            if (!pastedText.isEmpty()) {
                // Split keywords by the separator and add them
                String[] keywords = pastedText.split(String.valueOf(viewModel.getKeywordSeparator()));

                for (String keywordText : keywords) {
                    Keyword keyword = viewModel.getStringConverter().fromString(keywordText.trim());

                    // Add keyword if it's valid and not already present
                    if (keyword != null && !keywordTagsField.getTags().contains(keyword)) {
                        keywordTagsField.getTags().add(keyword);
                    }
                }
                // Clear the editor after pasting
                keywordTagsField.getEditor().clear();
            }
        }
    }

    private Node createTag(Keyword keyword) {
        Label tagLabel = new Label();
        tagLabel.setText(keywordTagsField.getConverter().toString(keyword));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(event -> keywordTagsField.removeTags(keyword));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        tagLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {  // Detect double-click
                keywordTagsField.removeTags(keyword);  // Remove the keyword
                keywordTagsField.getEditor().setText(keyword.get());  // Enter edit mode with the keyword text
                keywordTagsField.getEditor().requestFocus();  // Set focus to the editor
            }
        });
        tagLabel.setOnDragDetected(event -> {
            Dragboard db = tagLabel.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(keyword.get());
            db.setContent(content);
            draggedKeyword = keyword;  // Store the dragged keyword
            event.consume();
        });

        tagLabel.setOnDragOver(event -> {
            if (event.getGestureSource() != tagLabel && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        tagLabel.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString() && draggedKeyword != null) {
                int dropIndex = keywordTagsField.getTags().indexOf(keyword);
                keywordTagsField.removeTags(draggedKeyword);
                keywordTagsField.getTags().add(dropIndex, draggedKeyword);
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            draggedKeyword = null;  // Clear dragged keyword
            event.consume();
        });

        tagLabel.setOnDragDone(DragEvent::consume);

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
