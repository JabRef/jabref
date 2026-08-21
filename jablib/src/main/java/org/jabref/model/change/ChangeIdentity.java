package org.jabref.model.change;

import java.util.List;

/// Identity comparison helpers for change records.
///
/// [org.jabref.model.entry.BibEntry], [org.jabref.model.entry.BibtexString] and
/// [org.jabref.model.database.BibDatabase] all compare by content, but a change targets one
/// particular object: two entries with identical fields are distinct rows the user can edit
/// independently. Records holding them therefore compare and hash by identity, so a change
/// against one row is never considered equal to the same change against its twin.
///
/// Identity also keeps the hash stable. Content-based hashing over a mutable object -- a
/// database's hash covers its entry list -- would change under a record that is already in the
/// undo stack.
final class ChangeIdentity {

    private ChangeIdentity() {
    }

    static boolean same(Object left, Object right) {
        return left == right;
    }

    static boolean sameAll(List<?> left, List<?> right) {
        if (left.size() != right.size()) {
            return false;
        }
        for (int i = 0; i < left.size(); i++) {
            if (left.get(i) != right.get(i)) {
                return false;
            }
        }
        return true;
    }

    static int hash(Object value) {
        return System.identityHashCode(value);
    }

    static int hashAll(List<?> values) {
        int result = 1;
        for (Object value : values) {
            result = (31 * result) + System.identityHashCode(value);
        }
        return result;
    }
}
