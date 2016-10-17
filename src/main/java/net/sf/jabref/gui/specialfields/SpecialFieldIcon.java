package net.sf.jabref.gui.specialfields;

import java.util.Objects;

import javax.swing.Icon;
import javax.swing.JLabel;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.specialfields.SpecialFieldLocalization;
import net.sf.jabref.model.entry.specialfields.SpecialFieldValue;

public class SpecialFieldIcon {

    public static Icon getSpecialFieldValueIcon(SpecialFieldValue value) {
        Objects.requireNonNull(value);

        switch (value) {
            case PRINTED:
                return IconTheme.JabRefIcon.PRINTED.getSmallIcon();
            case CLEAR_PRIORITY:
                return null;
            case PRIO_1:
                return IconTheme.JabRefIcon.PRIORITY_HIGH.getSmallIcon();
            case PRIO_2:
                return IconTheme.JabRefIcon.PRIORITY_MEDIUM.getSmallIcon();
            case PRIO_3:
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


    public static JLabel createSpecialFieldValueLabel(SpecialFieldValue value) {
        JLabel label = new JLabel(getSpecialFieldValueIcon(value));
        label.setToolTipText(SpecialFieldLocalization.getToolTipText(value));
        return label;
    }

}

