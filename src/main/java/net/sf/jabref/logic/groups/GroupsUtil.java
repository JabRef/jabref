package net.sf.jabref.logic.groups;

import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;

public class GroupsUtil {

    public static TreeSet<String> findDeliminatedWordsInField(BibDatabase db, String field, String deliminator) {
        TreeSet<String> res = new TreeSet<>();

        for (String s : db.getKeySet()) {
            BibEntry be = db.getEntryById(s);
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
     * @param db a <code>BibDatabase</code> value
     * @param field a <code>String</code> value
     * @param remove a <code>String</code> value
     * @return a <code>HashSet</code> value
     */
    public static TreeSet<String> findAllWordsInField(BibDatabase db, String field, String remove) {
        TreeSet<String> res = new TreeSet<>();
        StringTokenizer tok;
        for (String s : db.getKeySet()) {
            BibEntry be = db.getEntryById(s);
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
    public static Set<String> findAuthorLastNames(BibDatabase db, List<String> fields) {
        Set<String> res = new TreeSet<>();
        for (String s : db.getKeySet()) {
            BibEntry be = db.getEntryById(s);
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


}
