package net.sf.jabref.gui.specialfields;

import javax.swing.Icon;
import javax.swing.JLabel;

import net.sf.jabref.gui.IconTheme;

import net.sf.jabref.specialfields.SpecialField;
import net.sf.jabref.specialfields.SpecialFieldValue;

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
        if(SpecialField.PRINTED.equals(value)){
            return IconTheme.JabRefIcon.PRINTED.getSmallIcon();
        } else if(SpecialField.PRIORITY.equals(value)){
            return getPriorityIcon(value);
        } else if(SpecialField.QUALITY.equals(value)){
            return IconTheme.JabRefIcon.QUALITY_ASSURED.getSmallIcon();
        } else if(SpecialField.RANK.equals(value)){
            return getRankIcon(value);
        } else if(SpecialField.READ_STATUS.equals(value)){
            return getReadStatusIcon(value);
        } else {
            return IconTheme.JabRefIcon.RELEVANCE.getSmallIcon();
        }
    }

    private static Icon getPriorityIcon(SpecialFieldValue value){
        if(value.getKeyword().equals("prio1")){
            return IconTheme.JabRefIcon.PRIORITY_HIGH.getSmallIcon();
        } else if(value.getKeyword().equals("prio2")) {
            return IconTheme.JabRefIcon.PRIORITY_MEDIUM.getSmallIcon();
        } else if(value.getKeyword().equals("prio3")) {
            return IconTheme.JabRefIcon.PRIORITY_LOW.getSmallIcon();
        } else {
            return null;
        }
    }

    private static Icon getRankIcon(SpecialFieldValue value){
        if(value.getKeyword().equals("rank1")){
            return IconTheme.JabRefIcon.RANK1.getSmallIcon();
        } else if(value.getKeyword().equals("rank2")) {
            return IconTheme.JabRefIcon.RANK2.getSmallIcon();
        } else if(value.getKeyword().equals("rank3")) {
            return IconTheme.JabRefIcon.RANK3.getSmallIcon();
        } else if(value.getKeyword().equals("rank4")) {
            return IconTheme.JabRefIcon.RANK4.getSmallIcon();
        } else if(value.getKeyword().equals("rank5")) {
            return IconTheme.JabRefIcon.RANK5.getSmallIcon();
        } else {
            return null;
        }
    }

    private static Icon getReadStatusIcon(SpecialFieldValue value){
        if(value.getKeyword().equals("read")){
            return IconTheme.JabRefIcon.READ_STATUS_READ.getSmallIcon();
        } else if (value.getKeyword().equals("skimmed")){
            return IconTheme.JabRefIcon.READ_STATUS_SKIMMED.getSmallIcon();
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