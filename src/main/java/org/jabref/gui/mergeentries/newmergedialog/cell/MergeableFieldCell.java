package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

import de.saxsys.mvvmfx.utils.commands.Command;

public class MergeableFieldCell extends FieldNameCell {
    private Command mergeCommand;
    private Command unmergeCommand;

    private final ObjectProperty<MergeAction> mergeAction = new SimpleObjectProperty<>();

    public MergeableFieldCell(Field field, int rowIndex) {
        super(field.getDisplayName(), rowIndex);
        mergeActionProperty().addListener((obs, old, newValue) -> {
            if (newValue == MergeAction.MERGE) {
                setAction(Localization.lang("Merge Groups"), IconTheme.JabRefIcons.MERGE_GROUPS, mergeCommand);
            } else {
                setAction(Localization.lang("Unmerge Groups"), IconTheme.JabRefIcons.UNDO, unmergeCommand);
            }
        });
    }

    public void setMergeCommand(Command mergeCommand) {
        this.mergeCommand = mergeCommand;
    }

    public void setUnmergeCommand(Command unmergeCommand) {
        this.unmergeCommand = unmergeCommand;
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
