package net.sf.jabref.gui.specialfields;

import javax.swing.Icon;
import javax.swing.JLabel;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.specialfields.SpecialField;
import net.sf.jabref.model.entry.SpecialFieldValue;


public class SpecialFieldIcon {

    public static Icon getRepresentingIcon(SpecialField field) {
        if (SpecialField.PRINTED.equals(field)) {
            return IconTheme.JabRefIcon.PRINTED.getSmallIcon();
        } else if (SpecialField.PRIORITY.equals(field)) {
            return IconTheme.JabRefIcon.PRIORITY.getSmallIcon();
        } else if (SpecialField.QUALITY.equals(field)) {
            return IconTheme.JabRefIcon.QUALITY.getSmallIcon();
        } else if (SpecialField.RANK.equals(field)) {
            return IconTheme.JabRefIcon.RANKING.getSmallIcon();
        } else if (SpecialField.READ_STATUS.equals(field)) {
            return IconTheme.JabRefIcon.READ_STATUS.getSmallIcon();
        } else if (SpecialField.RELEVANCE.equals(field)) {
            return IconTheme.JabRefIcon.RELEVANCE.getSmallIcon();
        }
        return null;
    }

    public static Icon getSpecialFieldValueIcon(SpecialFieldValue value){
        if("togglePrinted".equals(value.getActionName())){
            return IconTheme.JabRefIcon.PRINTED.getSmallIcon();
        } else if("clearPriority".equals(value.getActionName())){
            return null;
        } else if("setPriority1".equals(value.getActionName())){
            return IconTheme.JabRefIcon.PRIORITY_HIGH.getSmallIcon();
        } else if("setPriority2".equals(value.getActionName())){
            return IconTheme.JabRefIcon.PRIORITY_MEDIUM.getSmallIcon();
        } else if("setPriority3".equals(value.getActionName())){
            return IconTheme.JabRefIcon.PRIORITY_LOW.getSmallIcon();
        } else if("toggleQualityAssured".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.QUALITY_ASSURED.getSmallIcon();
        } else if("clearRank".equals(value.getActionName())) {
            return null;
        } else if("setRank1".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.RANK1.getSmallIcon();
        } else if("setRank2".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.RANK2.getSmallIcon();
        } else if("setRank3".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.RANK3.getSmallIcon();
        } else if("setRank4".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.RANK4.getSmallIcon();
        } else if("setRank5".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.RANK5.getSmallIcon();
        } else if("clearReadStatus".equals(value.getActionName())) {
            return null;
        } else if("setReadStatusToRead".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.READ_STATUS_SKIMMED.getSmallIcon();
        } else if("setReadStatusToSkimmed".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.READ_STATUS_SKIMMED.getSmallIcon();
        } else if("toggleRelevance".equals(value.getActionName())) {
            return IconTheme.JabRefIcon.RELEVANCE.getSmallIcon();
        } else {
            return null;
        }
    }


    public static JLabel createSpecialFieldValueLabel(SpecialFieldValue value) {
        JLabel label = new JLabel(getSpecialFieldValueIcon(value));
        label.setToolTipText(value.getToolTipText());
        return label;
    }

}