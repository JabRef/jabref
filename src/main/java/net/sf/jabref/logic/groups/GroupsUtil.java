package net.sf.jabref.logic.groups;

import java.awt.Component;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.Vector;

import javax.swing.JOptionPane;

import net.sf.jabref.groups.structure.AbstractGroup;
import net.sf.jabref.groups.structure.KeywordGroup;
import net.sf.jabref.gui.BibtexFields;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibtexEntry;

public class GroupsUtil {

    public static TreeSet<String> findDeliminatedWordsInField(BibtexDatabase db, String field, String deliminator) {
        TreeSet<String> res = new TreeSet<>();

        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            Object o = be.getField(field);
            if (o != null) {
                String fieldValue = o.toString().trim();
                StringTokenizer tok = new StringTokenizer(fieldValue, deliminator);
                while (tok.hasMoreTokens()) {
                    res.add(net.sf.jabref.model.entry.EntryUtil.capitalizeFirst(tok.nextToken().trim()));
                }
            }
        }
        return res;
    }

    /**
     * Returns a HashMap containing all words used in the database in the given field type. Characters in
     * <code>remove</code> are not included.
     *
     * @param db a <code>BibtexDatabase</code> value
     * @param field a <code>String</code> value
     * @param remove a <code>String</code> value
     * @return a <code>HashSet</code> value
     */
    public static TreeSet<String> findAllWordsInField(BibtexDatabase db, String field, String remove) {
        TreeSet<String> res = new TreeSet<>();
        StringTokenizer tok;
        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            Object o = be.getField(field);
            if (o != null) {
                tok = new StringTokenizer(o.toString(), remove, false);
                while (tok.hasMoreTokens()) {
                    res.add(net.sf.jabref.model.entry.EntryUtil.capitalizeFirst(tok.nextToken().trim()));
                }
            }
        }
        return res;
    }

    /**
     * Finds all authors' last names in all the given fields for the given database.
     *
     * @param db The database.
     * @param fields The fields to look in.
     * @return a set containing the names.
     */
    public static Set<String> findAuthorLastNames(BibtexDatabase db, List<String> fields) {
        Set<String> res = new TreeSet<>();
        for (String s : db.getKeySet()) {
            BibtexEntry be = db.getEntryById(s);
            for (String field : fields) {
                String val = be.getField(field);
                if ((val != null) && !val.isEmpty()) {
                    AuthorList al = AuthorList.getAuthorList(val);
                    for (int i = 0; i < al.size(); i++) {
                        AuthorList.Author a = al.getAuthor(i);
                        String lastName = a.getLast();
                        if ((lastName != null) && !lastName.isEmpty()) {
                            res.add(lastName);
                        }
                    }
                }

            }
        }

        return res;
    }

    /**
     * Warns the user of undesired side effects of an explicit assignment/removal of entries to/from this group.
     * Currently there are four types of groups: AllEntriesGroup, SearchGroup - do not support explicit assignment.
     * ExplicitGroup - never modifies entries. KeywordGroup - only this modifies entries upon assignment/removal.
     * Modifications are acceptable unless they affect a standard field (such as "author") besides the "keywords" field.
     *
     * @param parent The Component used as a parent when displaying a confirmation dialog.
     * @return true if the assignment has no undesired side effects, or the user chose to perform it anyway. false
     *         otherwise (this indicates that the user has aborted the assignment).
     */
    public static boolean warnAssignmentSideEffects(AbstractGroup[] groups, BibtexEntry[] entries, BibtexDatabase db, Component parent) {
        Vector<String> affectedFields = new Vector<>();
        for (AbstractGroup group : groups) {
            if (group instanceof KeywordGroup) {
                KeywordGroup kg = (KeywordGroup) group;
                String field = kg.getSearchField().toLowerCase();
                if ("keywords".equals(field)) {
                    continue; // this is not undesired
                }
                for (int i = 0, len = BibtexFields.numberOfPublicFields(); i < len; ++i) {
                    if (field.equals(BibtexFields.getFieldName(i))) {
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
        StringBuffer message = // JZTODO lyrics...
                new StringBuffer("This action will modify the following field(s)\n" + "in at least one entry each:\n");
        for (int i = 0; i < affectedFields.size(); ++i) {
            message.append(affectedFields.elementAt(i)).append("\n");
        }
        message.append("This could cause undesired changes to " + "your entries, so it is\nrecommended that you change the grouping field " + "in your group\ndefinition to \"keywords\" or a non-standard name." + "\n\nDo you still want to continue?");
        int choice = JOptionPane.showConfirmDialog(parent, message, Localization.lang("Warning"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return choice != JOptionPane.NO_OPTION;
    
        // if (groups instanceof KeywordGroup) {
        // KeywordGroup kg = (KeywordGroup) groups;
        // String field = kg.getSearchField().toLowerCase();
        // if (field.equals("keywords"))
        // return true; // this is not undesired
        // for (int i = 0; i < GUIGlobals.ALL_FIELDS.length; ++i) {
        // if (field.equals(GUIGlobals.ALL_FIELDS[i])) {
        // // show a warning, then return
        // String message = Globals // JZTODO lyrics...
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


}
