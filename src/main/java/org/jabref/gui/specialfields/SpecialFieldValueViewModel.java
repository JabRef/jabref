package org.jabref.gui.specialfields;

import java.util.Objects;
import java.util.Optional;

import javax.swing.Icon;
import javax.swing.JLabel;

import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefIcon;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldValueViewModel {

    public SpecialFieldValue getValue() {
        return value;
    }

    private final SpecialFieldValue value;

    public SpecialFieldValueViewModel(SpecialFieldValue value) {
        Objects.requireNonNull(value);

        this.value = value;
    }

    public Icon getSpecialFieldValueIcon() {
        return getIcon().map(JabRefIcon::getSmallIcon).orElse(null);
    }

    public Optional<JabRefIcon> getIcon() {
        switch (value) {
            case PRINTED:
                return Optional.of(IconTheme.JabRefIcons.PRINTED);
            case CLEAR_PRIORITY:
                return Optional.empty();
            case PRIORITY_HIGH:
                return Optional.of(IconTheme.JabRefIcons.PRIORITY_HIGH);
            case PRIORITY_MEDIUM:
                return Optional.of(IconTheme.JabRefIcons.PRIORITY_MEDIUM);
            case PRIORITY_LOW:
                return Optional.of(IconTheme.JabRefIcons.PRIORITY_LOW);
            case QUALITY_ASSURED:
                return Optional.of(IconTheme.JabRefIcons.QUALITY_ASSURED);
            case CLEAR_RANK:
                return Optional.empty();
            case RANK_1:
                return Optional.of(IconTheme.JabRefIcons.RANK1);
            case RANK_2:
                return Optional.of(IconTheme.JabRefIcons.RANK2);
            case RANK_3:
                return Optional.of(IconTheme.JabRefIcons.RANK3);
            case RANK_4:
                return Optional.of(IconTheme.JabRefIcons.RANK4);
            case RANK_5:
                return Optional.of(IconTheme.JabRefIcons.RANK5);
            case CLEAR_READ_STATUS:
                return Optional.empty();
            case READ:
                return Optional.of(IconTheme.JabRefIcons.READ_STATUS_READ);
            case SKIMMED:
                return Optional.of(IconTheme.JabRefIcons.READ_STATUS_SKIMMED);
            case RELEVANT:
                return Optional.of(IconTheme.JabRefIcons.RELEVANCE);
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
