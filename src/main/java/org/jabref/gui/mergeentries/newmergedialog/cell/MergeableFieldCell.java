package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Button;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class MergeableFieldCell extends FieldNameCell {
    private final ObjectProperty<FieldState> fieldState = new SimpleObjectProperty<>(FieldState.UNMERGED);
    private final BooleanProperty canMerge = new SimpleBooleanProperty(Boolean.TRUE);

    private Button toggleMergeUnmergeButton;

    private final ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());

    private final Field field;

    public MergeableFieldCell(Field field, int rowIndex) {
        super(field.getDisplayName(), rowIndex);
        this.field = field;

        configureMergeButton();
        super.addSideButton(toggleMergeUnmergeButton);
        toggleMergeUnmergeButton.disableProperty().bind(canMergeProperty().not());
    }

    private void configureMergeButton() {
        Action mergeAction = new Action.Builder(Localization.lang("Merge %0", field.getDisplayName()))
                .setIcon(IconTheme.JabRefIcons.MERGE_GROUPS);

        if (toggleMergeUnmergeButton == null) {
            toggleMergeUnmergeButton = actionFactory.createIconButton(mergeAction, new ToggleMergeUnmergeAction());
            toggleMergeUnmergeButton.setMaxHeight(Double.MAX_VALUE);
        }
        actionFactory.configureIconButton(mergeAction, new ToggleMergeUnmergeAction(), toggleMergeUnmergeButton);
    }

    private void configureUnmergeButton() {
        Action unmergeAction = new Action.Builder(Localization.lang("Unmerge %0", field.getDisplayName()))
                .setIcon(IconTheme.JabRefIcons.UNDO);

        if (toggleMergeUnmergeButton == null) {
            toggleMergeUnmergeButton = actionFactory.createIconButton(unmergeAction, new ToggleMergeUnmergeAction());
            toggleMergeUnmergeButton.setMaxHeight(Double.MAX_VALUE);
        }
        actionFactory.configureIconButton(unmergeAction, new ToggleMergeUnmergeAction(), toggleMergeUnmergeButton);
    }

    public ObjectProperty<FieldState> fieldStateProperty() {
        return fieldState;
    }

    private void setFieldState(FieldState fieldState) {
        fieldStateProperty().set(fieldState);
    }

    public FieldState getFieldState() {
        return fieldState.get();
    }

    public BooleanProperty canMergeProperty() {
        return canMerge;
    }

    public boolean canMerge() {
        return canMerge.get();
    }

    /**
     * Setting {@code canMerge} to {@code false} will disable the merge/unmerge button
     * */
    public void setCanMerge(boolean value) {
        canMergeProperty().set(value);
    }

    private class ToggleMergeUnmergeAction extends SimpleCommand {

        @Override
        public void execute() {
            if (fieldStateProperty().get() == FieldState.MERGED) {
                setFieldState(FieldState.UNMERGED);
                configureMergeButton();
            } else {
                setFieldState(FieldState.MERGED);
                configureUnmergeButton();
            }
        }
    }

    public enum FieldState {
        MERGED, UNMERGED
    }
}
