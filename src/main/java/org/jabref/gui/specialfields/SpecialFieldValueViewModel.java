package org.jabref.gui.specialfields;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.field.SpecialFieldValue;

public class SpecialFieldValueViewModel {

    private final SpecialFieldValue value;

    public SpecialFieldValueViewModel(SpecialFieldValue value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    public SpecialFieldValue getValue() {
        return value;
    }

    public Optional<JabRefIcon> getIcon() {
        return getAction().getIcon();
    }

    public String getMenuString() {
        return getAction().getText();
    }

    public String getToolTipText() {

        switch (value) {
            case PRINTED:
                return Localization.lang("Toggle print status");
            case CLEAR_PRIORITY:
                return Localization.lang("No priority information");
            case PRIORITY_HIGH:
                return Localization.lang("Priority high");
            case PRIORITY_MEDIUM:
                return Localization.lang("Priority medium");
            case PRIORITY_LOW:
                return Localization.lang("Priority low");
            case QUALITY_ASSURED:
                return Localization.lang("Toggle quality assured");
            case CLEAR_RANK:
                return Localization.lang("No rank information");
            case RANK_1:
                return Localization.lang("One star");
            case RANK_2:
                return Localization.lang("Two stars");
            case RANK_3:
                return Localization.lang("Three stars");
            case RANK_4:
                return Localization.lang("Four stars");
            case RANK_5:
                return Localization.lang("Five stars");
            case CLEAR_READ_STATUS:
                return Localization.lang("No read status information");
            case READ:
                return Localization.lang("Read status read");
            case SKIMMED:
                return Localization.lang("Read status skimmed");
            case RELEVANT:
                return Localization.lang("Toggle relevance");
            default:
                throw new IllegalArgumentException("There is no tooltip localization for special field value " + value);
        }
    }

    public Actions getCommand() {
        switch (value) {
            case PRINTED:
                return Actions.TOGGLE_PRINTED;
            case CLEAR_PRIORITY:
                return Actions.CLEAR_PRIORITY;
            case PRIORITY_HIGH:
                return Actions.SET_PRIORITY_1;
            case PRIORITY_MEDIUM:
                return Actions.SET_PRIORITY_2;
            case PRIORITY_LOW:
                return Actions.SET_PRIORITY_3;
            case QUALITY_ASSURED:
                return Actions.TOGGLE_QUALITY_ASSURED;
            case CLEAR_RANK:
                return Actions.CLEAR_RANK;
            case RANK_1:
                return Actions.SET_RANK_1;
            case RANK_2:
                return Actions.SET_RANK_2;
            case RANK_3:
                return Actions.SET_RANK_3;
            case RANK_4:
                return Actions.SET_RANK_4;
            case RANK_5:
                return Actions.SET_RANK_5;
            case CLEAR_READ_STATUS:
                return Actions.CLEAR_READ_STATUS;
            case READ:
                return Actions.SET_READ_STATUS_TO_READ;
            case SKIMMED:
                return Actions.SET_READ_STATUS_TO_SKIMMED;
            case RELEVANT:
                return Actions.TOGGLE_RELEVANCE;
            default:
                throw new IllegalArgumentException("There is no action name for special field value " + value);
        }
    }

    public Action getAction() {
        switch (value) {
            case PRINTED:
                return StandardActions.TOGGLE_PRINTED;
            case CLEAR_PRIORITY:
                return StandardActions.CLEAR_PRIORITY;
            case PRIORITY_HIGH:
                return StandardActions.PRIORITY_HIGH;
            case PRIORITY_MEDIUM:
                return StandardActions.PRIORITY_MEDIUM;
            case PRIORITY_LOW:
                return StandardActions.PRIORITY_LOW;
            case QUALITY_ASSURED:
                return StandardActions.QUALITY_ASSURED;
            case CLEAR_RANK:
                return StandardActions.CLEAR_RANK;
            case RANK_1:
                return StandardActions.RANK_1;
            case RANK_2:
                return StandardActions.RANK_2;
            case RANK_3:
                return StandardActions.RANK_3;
            case RANK_4:
                return StandardActions.RANK_4;
            case RANK_5:
                return StandardActions.RANK_5;
            case CLEAR_READ_STATUS:
                return StandardActions.CLEAR_READ_STATUS;
            case READ:
                return StandardActions.READ;
            case SKIMMED:
                return StandardActions.SKIMMED;
            case RELEVANT:
                return StandardActions.RELEVANT;
            default:
                throw new IllegalArgumentException("There is no tooltip localization for special field value " + value);
        }
    }
}
