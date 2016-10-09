package net.sf.jabref.model.groups;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.stream.Collectors;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.Author;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.strings.StringUtil;

public class GroupsUtil {

    public static Set<String> findDeliminatedWordsInField(BibDatabase db, String field, String deliminator) {
        Set<String> res = new TreeSet<>();

        for (BibEntry be : db.getEntries()) {
            be.getField(field).ifPresent(fieldValue -> {
                StringTokenizer tok = new StringTokenizer(fieldValue.trim(), deliminator);
                while (tok.hasMoreTokens()) {
                    res.add(StringUtil.capitalizeFirst(tok.nextToken().trim()));
                }
            });
        }
        return res;
    }

    /**
     * Returns a Set containing all words used in the database in the given field type. Characters in
     * <code>remove</code> are not included.
     *
     * @param db a <code>BibDatabase</code> value
     * @param field a <code>String</code> value
     * @param remove a <code>String</code> value
     * @return a <code>Set</code> value
     */
    public static Set<String> findAllWordsInField(BibDatabase db, String field, String remove) {
        Set<String> res = new TreeSet<>();
        for (BibEntry be : db.getEntries()) {
            be.getField(field).ifPresent(o -> {
                StringTokenizer tok = new StringTokenizer(o, remove, false);
                while (tok.hasMoreTokens()) {
                    res.add(StringUtil.capitalizeFirst(tok.nextToken().trim()));
                }
            });
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
        for (BibEntry be : db.getEntries()) {
            for (String field : fields) {
                be.getField(field).ifPresent(val -> {
                    if (!val.isEmpty()) {
                        AuthorList al = AuthorList.parse(val);
                        res.addAll(al.getAuthors().stream().map(Author::getLast).filter(Optional::isPresent)
                                .map(Optional::get).filter(lastName -> !lastName.isEmpty())
                                .collect(Collectors.toList()));
                    }
                });
            }
        }

        return res;
    }


}
