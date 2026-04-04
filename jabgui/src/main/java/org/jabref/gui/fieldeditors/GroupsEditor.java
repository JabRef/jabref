package org.jabref.gui.fieldeditors;

import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.field.Field;
import org.jabref.model.groups.GroupTreeNode;

import com.airhacks.afterburner.injection.Injector;

public class GroupsEditor extends TagsEditor {

    private final GroupsEditorViewModel viewModel;
    private Optional<BibEntry> bibEntry = Optional.empty();

    public GroupsEditor(Field field,
                        SuggestionProvider<?> suggestionProvider,
                        FieldCheckers fieldCheckers) {

        super(field, suggestionProvider, fieldCheckers, Injector.instantiateModelOrService(UndoManager.class));

        this.viewModel = new GroupsEditorViewModel(
                field,
                suggestionProvider,
                fieldCheckers,
                Injector.instantiateModelOrService(CliPreferences.class),
                undoManager);

        setupTagsField(
                GroupsEditorViewModel.getStringConverter(),
                viewModel.getGroupSeparator(),
                viewModel.groupListProperty(),
                viewModel::getSuggestions);

        setupGroupTreeDragAndDrop();
    }

    private void setupGroupTreeDragAndDrop() {
        this.setOnDragOver(event -> {
            if (event.getDragboard().hasContent(DragAndDropDataFormats.GROUP)) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        this.setOnDragDropped(event -> {
            boolean success = handleGroupDrop(event.getDragboard());
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean handleGroupDrop(Dragboard dragboard) {
        if (bibEntry.isEmpty()) {
            return false;
        }
        if (!dragboard.hasContent(DragAndDropDataFormats.GROUP)) {
            return false;
        }
        if (!(dragboard.getContent(DragAndDropDataFormats.GROUP) instanceof List<?> rawList) || rawList.isEmpty()) {
            return false;
        }

        boolean added = false;
        for (Object item : rawList) {
            if (item instanceof String path && !path.isBlank()) {
                added |= addGroupByPath(path);
            }
        }
        return added;
    }

    private boolean addGroupByPath(String path) {
        String groupName = extractGroupName(path);
        Keyword newGroup = new Keyword(groupName);
        if (tagsField.getTags().contains(newGroup)) {
            return false;
        }
        tagsField.addTags(newGroup);
        return true;
    }

    static String extractGroupName(String path) {
        int separatorIndex = path.lastIndexOf(GroupTreeNode.PATH_DELIMITER);
        if (separatorIndex >= 0) {
            return path.substring(separatorIndex + GroupTreeNode.PATH_DELIMITER.length());
        }
        return path;
    }

    public GroupsEditorViewModel getViewModel() {
        return viewModel;
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        viewModel.bindToEntry(entry);
        this.bibEntry = Optional.of(entry);
    }
}
