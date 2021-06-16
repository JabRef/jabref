package org.jabref.gui.specialfields;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.actions.Action;
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
        return switch (value) {
            case PRINTED -> Localization.lang("Toggle print status");
            case CLEAR_PRIORITY -> Localization.lang("No priority information");
            case PRIORITY_HIGH -> Localization.lang("Priority high");
            case PRIORITY_MEDIUM -> Localization.lang("Priority medium");
            case PRIORITY_LOW -> Localization.lang("Priority low");
            case QUALITY_ASSURED -> Localization.lang("Toggle quality assured");
            case CLEAR_RANK -> Localization.lang("No rank information");
            case RANK_1 -> Localization.lang("One star");
            case RANK_2 -> Localization.lang("Two stars");
            case RANK_3 -> Localization.lang("Three stars");
            case RANK_4 -> Localization.lang("Four stars");
            case RANK_5 -> Localization.lang("Five stars");
            case CLEAR_READ_STATUS -> Localization.lang("No read status information");
            case READ -> Localization.lang("Read status read");
            case SKIMMED -> Localization.lang("Read status skimmed");
            case RELEVANT -> Localization.lang("Toggle relevance");
        };
    }

    public Action getAction() {
        return switch (value) {
            case PRINTED -> StandardActions.TOGGLE_PRINTED;
            case CLEAR_PRIORITY -> StandardActions.CLEAR_PRIORITY;
            case PRIORITY_HIGH -> StandardActions.PRIORITY_HIGH;
            case PRIORITY_MEDIUM -> StandardActions.PRIORITY_MEDIUM;
            case PRIORITY_LOW -> StandardActions.PRIORITY_LOW;
            case QUALITY_ASSURED -> StandardActions.QUALITY_ASSURED;
            case CLEAR_RANK -> StandardActions.CLEAR_RANK;
            case RANK_1 -> StandardActions.RANK_1;
            case RANK_2 -> StandardActions.RANK_2;
            case RANK_3 -> StandardActions.RANK_3;
            case RANK_4 -> StandardActions.RANK_4;
            case RANK_5 -> StandardActions.RANK_5;
            case CLEAR_READ_STATUS -> StandardActions.CLEAR_READ_STATUS;
            case READ -> StandardActions.READ;
            case SKIMMED -> StandardActions.SKIMMED;
            case RELEVANT -> StandardActions.RELEVANT;
        };
    }
}
