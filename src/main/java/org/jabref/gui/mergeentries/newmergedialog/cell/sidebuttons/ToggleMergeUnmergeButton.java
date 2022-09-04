package org.jabref.gui.mergeentries.newmergedialog.cell.sidebuttons;

import java.util.Optional;

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
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.Field;

public class ToggleMergeUnmergeButton extends Button {
    private final ObjectProperty<FieldState> fieldState = new SimpleObjectProperty<>(FieldState.UNMERGED);
    private final BooleanProperty canMerge = new SimpleBooleanProperty(Boolean.TRUE);

    private final ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());

    private final Field field;

    public ToggleMergeUnmergeButton(Field field) {
        this.field = field;
        setMaxHeight(Double.MAX_VALUE);
        setFocusTraversable(false);

        configureMergeButton();
        this.disableProperty().bind(canMergeProperty().not());
    }

    private void configureMergeButton() {
        ToggleMergeCommand mergeCommand = new ToggleMergeCommand();
        actionFactory.configureIconButton(mergeCommand.mergeAction, mergeCommand, this);
    }

    private void configureUnmergeButton() {
        ToggleMergeCommand unmergeCommand = new ToggleMergeCommand();
        actionFactory.configureIconButton(unmergeCommand.unmergeAction, unmergeCommand, this);
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

    private class ToggleMergeCommand extends SimpleCommand {
        private final Action mergeAction = new Action() {
            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(IconTheme.JabRefIcons.MERGE_GROUPS);
            }

            @Override
            public String getText() {
                return Localization.lang("Merge %0", field.getDisplayName());
            }
        };

        private final Action unmergeAction = new Action() {
            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.of(IconTheme.JabRefIcons.UNDO);
            }

            @Override
            public String getText() {
                return Localization.lang("Unmerge %0", field.getDisplayName());
            }
        };

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
