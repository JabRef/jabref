package org.jabref.gui.fieldeditors;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
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
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

import com.airhacks.afterburner.injection.Injector;
import com.airhacks.afterburner.views.ViewLoader;
import com.dlsc.gemsfx.TagsField;
import com.google.common.collect.Comparators;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GroupsEditor extends TagsEditor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupsEditor.class);
    private static final PseudoClass FOCUSED = PseudoClass.getPseudoClass("focused");

    @FXML private GroupsEditorViewModel viewModel;
    @FXML private TagsField<Keyword> groupTagsField;

    @Inject private CliPreferences preferences;
    @Inject private DialogService dialogService;
    @Inject private ClipBoardManager clipBoardManager;

    private boolean isSortedTagsField = false;
    private Optional<Keyword> draggedGroup = Optional.empty();
    private Optional<BibEntry> bibEntry = Optional.empty();

    public GroupsEditor(Field field,
                        SuggestionProvider<?> suggestionProvider,
                        FieldCheckers fieldCheckers) {

        super(field, suggestionProvider, fieldCheckers, Injector.instantiateModelOrService(UndoManager.class));

        ViewLoader.view(this)
                  .root(this)
                  .load();

        this.viewModel = new GroupsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                Injector.instantiateModelOrService(CliPreferences.class),
                undoManager);

        groupTagsField.setCellFactory(new ViewModelListCellFactory<Keyword>().withText(Keyword::get));
        groupTagsField.setTagViewFactory(this::createTag);

        groupTagsField.setSuggestionProvider(request -> viewModel.getSuggestions(request.getUserText()));
        groupTagsField.setConverter(GroupsEditorViewModel.getStringConverter());
        groupTagsField.setMatcher((group, searchText) -> group.get().toLowerCase().startsWith(searchText.toLowerCase()));
        groupTagsField.setComparator(Comparator.comparing(Keyword::get));

        groupTagsField.setNewItemProducer(searchText -> GroupsEditorViewModel.getStringConverter().fromString(searchText));

        groupTagsField.setShowSearchIcon(false);
        groupTagsField.setOnMouseClicked(_ -> groupTagsField.getEditor().requestFocus());
        groupTagsField.getEditor().getStyleClass().clear();
        groupTagsField.getEditor().getStyleClass().add("tags-field-editor");
        groupTagsField.getEditor().focusedProperty().addListener((_, _, newValue) -> groupTagsField.pseudoClassStateChanged(FOCUSED, newValue));

        String groupSeparator = String.valueOf(viewModel.getGroupSeparator());
        groupTagsField.getEditor().setOnKeyReleased(event -> {
            if (event.getText().equals(groupSeparator)) {
                groupTagsField.commit();
                event.consume();
            }
        });

        this.viewModel.groupListProperty().addListener((_, _, _) -> {
            if (groupTagsField.getTags().size() < 2) {
                isSortedTagsField = false;
            } else if ((Comparators.isInOrder(groupTagsField.getTags(), Comparator.comparing(Keyword::get))) || isSortedTagsField) {
                isSortedTagsField = true;
                groupTagsField.getTags().sort(Comparator.comparing(Keyword::get));
            }
        });

        groupTagsField.getEditor().setOnKeyPressed(event -> {
            KeyBindingRepository keyBindingRepository = Injector.instantiateModelOrService(KeyBindingRepository.class);

            if (keyBindingRepository.checkKeyCombinationEquality(KeyBinding.PASTE, event)) {
                String clipboardText = ClipBoardManager.getContents();
                if (!clipboardText.isEmpty()) {
                    KeywordList groupsList = KeywordList.parse(clipboardText, viewModel.getGroupSeparator());
                    groupsList.stream().forEach(group -> groupTagsField.addTags(group));
                    groupTagsField.getEditor().clear();
                    event.consume();
                }
            }
        });

        // Handle drag and drop from group tree
        this.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            boolean success = false;
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                List<String> draggedGroups = (List<String>) event.getDragboard().getContent(DragAndDropDataFormats.GROUP);
                if (bibEntry.isPresent() && draggedGroups.getFirst() != null) {
                    Keyword newGroup = new Keyword(draggedGroups.getFirst());
                    if (!groupTagsField.getTags().contains(newGroup)) {
                        groupTagsField.addTags(newGroup);
                    }
                    success = true;
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });

        Bindings.bindContentBidirectional(groupTagsField.getTags(), viewModel.groupListProperty());
    }

    private Node createTag(Keyword group) {
        Label tagLabel = new Label();
        tagLabel.setText(groupTagsField.getConverter().toString(group));
        tagLabel.setGraphic(IconTheme.JabRefIcons.REMOVE_TAGS.getGraphicNode());
        tagLabel.getGraphic().setOnMouseClicked(_ -> groupTagsField.removeTags(group));
        tagLabel.setContentDisplay(ContentDisplay.RIGHT);
        ContextMenu contextMenu = new ContextMenu();
        ActionFactory factory = new ActionFactory();
        contextMenu.getItems().addAll(
                factory.createMenuItem(StandardActions.COPY, new GroupsEditor.TagContextAction(StandardActions.COPY, group)),
                factory.createMenuItem(StandardActions.CUT, new GroupsEditor.TagContextAction(StandardActions.CUT, group)),
                factory.createMenuItem(StandardActions.DELETE, new GroupsEditor.TagContextAction(StandardActions.DELETE, group))
        );
        tagLabel.setContextMenu(contextMenu);
        tagLabel.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                groupTagsField.removeTags(group);
                groupTagsField.getEditor().setText(group.get());
                groupTagsField.getEditor().positionCaret(group.get().length());
            }
        });
        tagLabel.setOnDragOver(event -> {
            if (event.getGestureSource() != tagLabel && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        tagLabel.setOnDragDetected(event -> {
            Dragboard db = tagLabel.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(group.get());
            db.setContent(content);
            draggedGroup = Optional.of(group);
            event.consume();
        });
        tagLabel.setOnDragEntered(_ -> tagLabel.setStyle("-fx-background-color: lightgrey;"));
        tagLabel.setOnDragExited(_ -> tagLabel.setStyle(""));
        tagLabel.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString() && draggedGroup.isPresent()) {
                int oldIndex = groupTagsField.getTags().indexOf(draggedGroup.get());
                int dropIndex = groupTagsField.getTags().indexOf(group);
                if (oldIndex != dropIndex) {
                    groupTagsField.removeTags(draggedGroup.get());
                    groupTagsField.getTags().add(dropIndex, draggedGroup.get());
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            draggedGroup = Optional.empty();
            event.consume();
        });
        tagLabel.setOnDragDone(DragEvent::consume);
        return tagLabel;
    }

    public GroupsEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
        this.bibEntry = Optional.of(entry);
    }

    private class TagContextAction extends SimpleCommand {
        private final StandardActions command;
        private final Keyword group;

        public TagContextAction(StandardActions command, Keyword group) {
            this.command = command;
            this.group = group;
        }

        @Override
        public void execute() {
            switch (command) {
                case COPY -> {
                    clipBoardManager.setContent(group.get());
                    dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                            JabRefDialogService.shortenDialogMessage(group.get())));
                }
                case CUT -> {
                    clipBoardManager.setContent(group.get());
                    dialogService.notify(Localization.lang("Copied '%0' to clipboard.",
                            JabRefDialogService.shortenDialogMessage(group.get())));
                    groupTagsField.removeTags(group);
                }
                case DELETE ->
                        groupTagsField.removeTags(group);
                default ->
                        LOGGER.info("Action {} not defined", command.getText());
            }
        }
    }
}
