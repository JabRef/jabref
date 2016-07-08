package net.sf.jabref.gui.groups;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.logic.groups.AbstractGroup;
import net.sf.jabref.logic.groups.KeywordGroup;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.InternalBibtexFields;

public class WarnAssignmentSideEffects {

    /**
     * Warns the user of undesired side effects of an explicit assignment/removal of entries to/from this group.
     * Currently there are four types of groups: AllEntriesGroup, SearchGroup - do not support explicit assignment.
     * ExplicitGroup and KeywordGroup - this modifies entries upon assignment/removal.
     * Modifications are acceptable unless they affect a standard field (such as "author") besides the "keywords" or "groups' field.
     *
     * @param parent The Component used as a parent when displaying a confirmation dialog.
     * @return true if the assignment has no undesired side effects, or the user chose to perform it anyway. false
     * otherwise (this indicates that the user has aborted the assignment).
     */
    public static boolean warnAssignmentSideEffects(List<AbstractGroup> groups, Component parent) {
        List<String> affectedFields = new ArrayList<>();
        for (AbstractGroup group : groups) {
            if (group instanceof KeywordGroup) {
                KeywordGroup kg = (KeywordGroup) group;
                String field = kg.getSearchField().toLowerCase();
                if ("keywords".equals(field) || "groups".equals(field)) {
                    continue; // this is not undesired
                }
                int len = InternalBibtexFields.numberOfPublicFields();
                for (int i = 0; i < len; ++i) {
                    if (field.equals(InternalBibtexFields.getFieldName(i))) {
                        affectedFields.add(field);
                        break;
                    }
                }
            }
        }
        if (affectedFields.isEmpty()) {
            return true; // no side effects
        }
    
        // show a warning, then return
        StringBuilder message = new StringBuilder(
                Localization.lang("This action will modify the following field(s) in at least one entry each:"))
                        .append('\n');
        for (String affectedField : affectedFields) {
            message.append(affectedField).append('\n');
        }
        message.append(Localization.lang("This could cause undesired changes to your entries.")).append('\n')
                .append("It is recommended that you change the grouping field in your group definition to \"keywords\" or a non-standard name.")
                .append("\n\n").append(Localization.lang("Do you still want to continue?"));
        int choice = JOptionPane.showConfirmDialog(parent, message, Localization.lang("Warning"),
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.NO_OPTION;
    
        // if (groups instanceof KeywordGroup) {
        // KeywordGroup kg = (KeywordGroup) groups;
        // String field = kg.getSearchField().toLowerCase();
        // if (field.equals("keywords"))
        // return true; // this is not undesired
        // for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
        // if (field.equals(GUIGlobals.ALL_FIELDS[i])) {
        // // show a warning, then return
        // String message = Globals ...
        // .lang(
        // "This action will modify the \"%0\" field "
        // + "of your entries.\nThis could cause undesired changes to "
        // + "your entries, so it is\nrecommended that you change the grouping
        // field "
        // + "in your group\ndefinition to \"keywords\" or a non-standard name."
        // + "\n\nDo you still want to continue?",
        // field);
        // int choice = JOptionPane.showConfirmDialog(parent, message,
        // Globals.lang("Warning"), JOptionPane.YES_NO_OPTION,
        // JOptionPane.WARNING_MESSAGE);
        // return choice != JOptionPane.NO_OPTION;
        // }
        // }
        // }
        // return true; // found no side effects
    }

    public static boolean warnAssignmentSideEffects(AbstractGroup group, Component parent) {
        return warnAssignmentSideEffects(Collections.singletonList(group), parent);
    }

}
