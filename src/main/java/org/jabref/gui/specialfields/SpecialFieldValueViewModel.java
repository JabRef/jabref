package org.jabref.gui.specialfields;

import java.util.Objects;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jabref.gui.JabRefIcon;
import org.jabref.gui.actions.Actions;
import org.jabref.gui.actions.ActionsFX;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldValueViewModel {

    private final SpecialFieldValue value;

    public SpecialFieldValueViewModel(SpecialFieldValue value) {
        Objects.requireNonNull(value);
        this.value = value;
    }

    public SpecialFieldValue getValue() {
        return value;
    }

    public Icon getSpecialFieldValueIcon() {
        return getIcon().map(JabRefIcon::getSmallIcon).orElse(null);
    }

    public Optional<JabRefIcon> getIcon() {
        return getAction().getIcon();
    }

    public JLabel createSpecialFieldValueLabel() {
        JLabel label = new JLabel(getSpecialFieldValueIcon());
        label.setToolTipText(getToolTipText());
        return label;
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
                return Actions.togglePrinted;
            case CLEAR_PRIORITY:
                return Actions.clearPriority;
            case PRIORITY_HIGH:
                return Actions.setPriority1;
            case PRIORITY_MEDIUM:
                return Actions.setPriority2;
            case PRIORITY_LOW:
                return Actions.setPriority3;
            case QUALITY_ASSURED:
                return Actions.toggleQualityAssured;
            case CLEAR_RANK:
                return Actions.clearRank;
            case RANK_1:
                return Actions.setRank1;
            case RANK_2:
                return Actions.setRank2;
            case RANK_3:
                return Actions.setRank3;
            case RANK_4:
                return Actions.setRank4;
            case RANK_5:
                return Actions.setRank5;
            case CLEAR_READ_STATUS:
                return Actions.clearReadStatus;
            case READ:
                return Actions.setReadStatusToRead;
            case SKIMMED:
                return Actions.setReadStatusToSkimmed;
            case RELEVANT:
                return Actions.toggleRelevance;
            default:
                throw new IllegalArgumentException("There is no action name for special field value " + value);
        }
    }

    public ActionsFX getAction() {
        switch (value) {
            case PRINTED:
                return ActionsFX.TOGGLE_PRINTED;
            case CLEAR_PRIORITY:
                return ActionsFX.CLEAR_PRIORITY;
            case PRIORITY_HIGH:
                return ActionsFX.PRIORITY_HIGH;
            case PRIORITY_MEDIUM:
                return ActionsFX.PRIORITY_MEDIUM;
            case PRIORITY_LOW:
                return ActionsFX.PRIORITY_LOW;
            case QUALITY_ASSURED:
                return ActionsFX.QUALITY_ASSURED;
            case CLEAR_RANK:
                return ActionsFX.CLEAR_RANK;
            case RANK_1:
                return ActionsFX.RANK_1;
            case RANK_2:
                return ActionsFX.RANK_2;
            case RANK_3:
                return ActionsFX.RANK_3;
            case RANK_4:
                return ActionsFX.RANK_4;
            case RANK_5:
                return ActionsFX.RANK_5;
            case CLEAR_READ_STATUS:
                return ActionsFX.CLEAR_READ_STATUS;
            case READ:
                return ActionsFX.READ;
            case SKIMMED:
                return ActionsFX.SKIMMED;
            case RELEVANT:
                return ActionsFX.RELEVANT;
            default:
                throw new IllegalArgumentException("There is no tooltip localization for special field value " + value);
        }
    }
}
