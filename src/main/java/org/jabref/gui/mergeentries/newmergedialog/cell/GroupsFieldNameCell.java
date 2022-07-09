package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.StandardField;

import de.saxsys.mvvmfx.utils.commands.Command;

public class GroupsFieldNameCell extends FieldNameCell {
    private Command mergeGroupsCommand;
    private Command unmergeGroupsCommand;

    private final ObjectProperty<MergeAction> mergeAction = new SimpleObjectProperty<>();

    public GroupsFieldNameCell(int rowIndex) {
        super(StandardField.GROUPS.getDisplayName(), rowIndex);
        mergeActionProperty().addListener((obs, old, newValue) -> {
            if (newValue == MergeAction.MERGE) {
                setAction(Localization.lang("Merge Groups"), IconTheme.JabRefIcons.MERGE_GROUPS, mergeGroupsCommand);
            } else {
                setAction(Localization.lang("Unmerge Groups"), IconTheme.JabRefIcons.UNDO, unmergeGroupsCommand);
            }
        });
    }

    public void setMergeGroupsCommand(Command mergeGroupsCommand) {
        this.mergeGroupsCommand = mergeGroupsCommand;
    }

    public void setUnmergeGroupsCommand(Command unmergeGroupsCommand) {
        this.unmergeGroupsCommand = unmergeGroupsCommand;
    }

    public ObjectProperty<MergeAction> mergeActionProperty() {
        return mergeAction;
    }

    public void setMergeAction(MergeAction mergeAction) {
        mergeActionProperty().set(mergeAction);
    }

    public enum MergeAction {
        MERGE, UNMERGE
    }
}
