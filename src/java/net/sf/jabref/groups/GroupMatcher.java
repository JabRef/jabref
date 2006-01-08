package net.sf.jabref.groups;

import ca.odell.glazedlists.matchers.Matcher;
import net.sf.jabref.BibtexEntry;

/**
 * Matcher for filtering or sorting the table according to whether entries
 * are tagged as group matches.
 */
public class GroupMatcher implements Matcher {

    public static GroupMatcher INSTANCE = new GroupMatcher();

    public boolean matches(Object object) {
        BibtexEntry entry = (BibtexEntry)object;
        return entry.isGroupHit();
    }
}

