package org.jabref.gui.fieldeditors;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ListProperty;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.injection.Injector;
import com.dlsc.gemsfx.TagsField;
import com.google.common.collect.Comparators;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Abstract base class for tag-based field editors.
/// Provides common functionality for editors that display field values as tags,
/// including tag creation, drag-and-drop reordering, clipboard operations,
/// and sort preservation.
public abstract class TagsEditor extends HBox implements FieldEditorFX {

    private static final Logger LOGGER = LoggerFactory.getLogger(TagsEditor.class);
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");
    private static final PseudoClass DRAG_OVER = PseudoClass.getPseudoClass("drag-over");

    protected final Field field;
    protected final SuggestionProvider<?> suggestionProvider;
    protected final FieldCheckers fieldCheckers;
    protected final UndoManager undoManager;

    protected final TagsField<Keyword> tagsField = new TagsField<>();

    protected final DialogService dialogService;
    protected final ClipBoardManager clipBoardManager;
    protected final KeyBindingRepository keyBindingRepository;

    private boolean isSortedTagsField = false;
    private Optional<Keyword> draggedTag = Optional.empty();

    protected TagsEditor(Field field,
                         SuggestionProvider<?> suggestionProvider,
                         FieldCheckers fieldCheckers,
                         UndoManager undoManager) {
        this.field = field;
        this.suggestionProvider = suggestionProvider;
        this.fieldCheckers = fieldCheckers;
        this.undoManager = undoManager;

        this.dialogService = Injector.instantiateModelOrService(DialogService.class);
        this.clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        this.keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);

        HBox.setHgrow(tagsField, Priority.ALWAYS);
        getChildren().add(tagsField);
    }

    /// Initializes the TagsField with common configuration.
    /// Must be called by subclasses after their ViewModel is created.
    protected void setupTagsField(StringConverter<Keyword> converter,
                                  Character separator,
                                  ListProperty<Keyword> tagListProperty,
                                  Function<String, List<Keyword>> suggestionsProvider) {

        tagsField.setCellFactory(new ViewModelListCellFactory<Keyword>().withText(Keyword::get));
        tagsField.setTagViewFactory(this::createTag);

        tagsField.setSuggestionProvider(request -> suggestionsProvider.apply(request.getUserText()));
        tagsField.setConverter(converter);
        tagsField.setMatcher((tag, searchText) -> tag.get().toLowerCase().startsWith(searchText.toLowerCase()));
        tagsField.setComparator(Comparator.comparing(Keyword::get));

        tagsField.setNewItemProducer(searchText -> converter.fromString(searchText));

        tagsField.setShowSearchIcon(false);
        tagsField.setOnMouseClicked(_ -> tagsField.getEditor().requestFocus());
        tagsField.getEditor().getStyleClass().clear();
        tagsField.getEditor().getStyleClass().add("tags-field-editor");
        tagsField.getEditor().focusedProperty().addListener((_, _, newValue) ->
                tagsField.pseudoClassStateChanged(FOCUSED, newValue));

        String separatorStr = String.valueOf(separator);
        tagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getText().equals(separatorStr)) {
                tagsField.commit();
                event.consume();
            }
        });

        // Preserve alphabetical order when tags are added programmatically,
        // but stop auto-sorting once the user reorders tags manually via drag-and-drop
        tagListProperty.addListener((_, _, _) -> {
            if (tagsField.getTags().size() < 2) {
                isSortedTagsField = false;
            } else if (Comparators.isInOrder(tagsField.getTags(), Comparator.comparing(Keyword::get)) || isSortedTagsField) {
                isSortedTagsField = true;
                tagsField.getTags().sort(Comparator.comparing(Keyword::get));
            }
        });

        tagsField.getEditor().setOnKeyPressed(event -> {
            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.PASTE, event)) {
                String clipboardText = ClipBoardManager.getContents();
                if (!clipboardText.isEmpty()) {
                    KeywordList parsed = KeywordList.parse(clipboardText, separator);
                    parsed.forEach(tag -> tagsField.addTags(tag));
                    tagsField.getEditor().clear();
                    event.consume();
                }
            }
        });

        Bindings.bindContentBidirectional(tagsField.getTags(), tagListProperty);
    }

    private Node createTag(Keyword tag) {
        Label tagLabel = new Label();
        tagLabel.setText(tagsField.getConverter().toString(tag));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(_ -> tagsField.removeTags(tag));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);

        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new TagContextAction(StandardActions.COPY, tag)),
                factory.createMenuItem(StandardActions.CUT, new TagContextAction(StandardActions.CUT, tag)),
                factory.createMenuItem(StandardActions.DELETE, new TagContextAction(StandardActions.DELETE, tag))
        );
        tagLabel.setContextMenu(contextMenu);

        tagLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                tagsField.removeTags(tag);
                tagsField.getEditor().setText(tag.get());
                tagsField.getEditor().positionCaret(tag.get().length());
            }
        });

        tagLabel.setOnDragOver(event -> {
            if (event.getGestureSource() != tagLabel && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        customizeTag(tagLabel, tag);

        tagLabel.setOnDragDetected(event -> {
            Dragboard db = tagLabel.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(tag.get());
            db.setContent(content);
            draggedTag = Optional.of(tag);
            event.consume();
        });
        tagLabel.setOnDragEntered(_ -> tagLabel.pseudoClassStateChanged(DRAG_OVER, true));
        tagLabel.setOnDragExited(_ -> tagLabel.pseudoClassStateChanged(DRAG_OVER, false));
        tagLabel.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString() && draggedTag.isPresent()) {
                int oldIndex = tagsField.getTags().indexOf(draggedTag.get());
                int dropIndex = tagsField.getTags().indexOf(tag);
                if (oldIndex != dropIndex) {
                    tagsField.removeTags(draggedTag.get());
                    tagsField.getTags().add(dropIndex, draggedTag.get());
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            draggedTag = Optional.empty();
            event.consume();
        });
        tagLabel.setOnDragDone(DragEvent::consume);
        return tagLabel;
    }

    /// Hook for subclasses to customize tag labels (e.g., adding tooltips).
    protected void customizeTag(Label tagLabel, Keyword tag) {
    }

    @Override
    public Parent getNode() {
        return this;
    }

    @Override
    public double getWeight() {
        return 2;
    }

    @Override
    public abstract void bindToEntry(BibEntry entry);

    private class TagContextAction extends SimpleCommand {
        private final StandardActions command;
        private final Keyword tag;

        public TagContextAction(StandardActions command, Keyword tag) {
            this.command = command;
            this.tag = tag;
        }

        @Override
        public void execute() {
            switch (command) {
                case COPY -> {
                    clipBoardManager.setContent(tag.get());
                    dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                            JabRefDialogService.shortenDialogMessage(tag.get())));
                }
                case CUT -> {
                    clipBoardManager.setContent(tag.get());
                    dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                            JabRefDialogService.shortenDialogMessage(tag.get())));
                    tagsField.removeTags(tag);
                }
                case DELETE ->
                        tagsField.removeTags(tag);
                default ->
                        LOGGER.info("Action {} not defined", command.getText());
            }
        }
    }
}
