package org.jabref.gui.specialfields;

import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jabref.gui.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldValueViewModel {

    private final SpecialFieldValue value;

    public SpecialFieldValueViewModel(SpecialFieldValue value) {
        Objects.requireNonNull(value);

        this.value = value;
    }

    public Icon getSpecialFieldValueIcon() {

        switch (value) {
            case PRINTED:
                return IconTheme.JabRefIcon.PRINTED.getSmallIcon();
            case CLEAR_PRIORITY:
                return null;
            case PRIORITY_HIGH:
                return IconTheme.JabRefIcon.PRIORITY_HIGH.getSmallIcon();
            case PRIORITY_MEDIUM:
                return IconTheme.JabRefIcon.PRIORITY_MEDIUM.getSmallIcon();
            case PRIORITY_LOW:
                return IconTheme.JabRefIcon.PRIORITY_LOW.getSmallIcon();
            case QUALITY_ASSURED:
                return IconTheme.JabRefIcon.QUALITY_ASSURED.getSmallIcon();
            case CLEAR_RANK:
                return null;
            case RANK_1:
                return IconTheme.JabRefIcon.RANK1.getSmallIcon();
            case RANK_2:
                return IconTheme.JabRefIcon.RANK2.getSmallIcon();
            case RANK_3:
                return IconTheme.JabRefIcon.RANK3.getSmallIcon();
            case RANK_4:
                return IconTheme.JabRefIcon.RANK4.getSmallIcon();
            case RANK_5:
                return IconTheme.JabRefIcon.RANK5.getSmallIcon();
            case CLEAR_READ_STATUS:
                return null;
            case READ:
                return IconTheme.JabRefIcon.READ_STATUS_READ.getSmallIcon();
            case SKIMMED:
                return IconTheme.JabRefIcon.READ_STATUS_SKIMMED.getSmallIcon();
            case RELEVANT:
                return IconTheme.JabRefIcon.RELEVANCE.getSmallIcon();
            default:
                throw new IllegalArgumentException("There is no icon mapping for special field value " + value);
        }
    }

    public JLabel createSpecialFieldValueLabel() {
        JLabel label = new JLabel(getSpecialFieldValueIcon());
        label.setToolTipText(getToolTipText());
        return label;
    }

    public String getMenuString() {

        switch (value) {
            case PRINTED:
                return Localization.lang("Toggle print status");
            case CLEAR_PRIORITY:
                return Localization.lang("Clear priority");
            case PRIORITY_HIGH:
                return Localization.lang("Set priority to high");
            case PRIORITY_MEDIUM:
                return Localization.lang("Set priority to medium");
            case PRIORITY_LOW:
                return Localization.lang("Set priority to low");
            case QUALITY_ASSURED:
                return Localization.lang("Toggle quality assured");
            case CLEAR_RANK:
                return Localization.lang("Clear rank");
            case RANK_1:
                return "";
            case RANK_2:
                return "";
            case RANK_3:
                return "";
            case RANK_4:
                return "";
            case RANK_5:
                return "";
            case CLEAR_READ_STATUS:
                return Localization.lang("Clear read status");
            case READ:
                return Localization.lang("Set read status to read");
            case SKIMMED:
                return Localization.lang("Set read status to skimmed");
            case RELEVANT:
                return Localization.lang("Toggle relevance");
            default:
                throw new IllegalArgumentException("There is no tooltip localization for special field value " + value);
        }
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

    public String getActionName() {

        switch (value) {
            case PRINTED:
                return "togglePrinted";
            case CLEAR_PRIORITY:
                return "clearPriority";
            case PRIORITY_HIGH:
                return "setPriority1";
            case PRIORITY_MEDIUM:
                return "setPriority2";
            case PRIORITY_LOW:
                return "setPriority3";
            case QUALITY_ASSURED:
                return "toggleQualityAssured";
            case CLEAR_RANK:
                return "clearRank";
            case RANK_1:
                return "setRank1";
            case RANK_2:
                return "setRank2";
            case RANK_3:
                return "setRank3";
            case RANK_4:
                return "setRank4";
            case RANK_5:
                return "setRank5";
            case CLEAR_READ_STATUS:
                return "clearReadStatus";
            case READ:
                return "setReadStatusToRead";
            case SKIMMED:
                return "setReadStatusToSkimmed";
            case RELEVANT:
                return "toggleRelevance";
            default:
                throw new IllegalArgumentException("There is no action name for special field value " + value);
        }
    }
}
