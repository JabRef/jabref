package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibtexString;

public class BibStringDiff {

    private final BibtexString originalString;
    private final BibtexString newString;

    BibStringDiff(BibtexString originalString, BibtexString newString) {
        this.originalString = originalString;
        this.newString = newString;
    }

    public static List<BibStringDiff> compare(BibDatabase originalDatabase, BibDatabase newDatabase) {
        if (originalDatabase.hasNoStrings() && newDatabase.hasNoStrings()) {
            return Collections.emptyList();
        }

        List<BibStringDiff> differences = new ArrayList<>();

        Set<BibtexString> used = new HashSet<>();
        Set<BibtexString> notMatched = new HashSet<>();

        // First try to match by string names.
        for (BibtexString original : originalDatabase.getStringValues()) {
            Optional<BibtexString> match = newDatabase
                    .getStringValues().stream()
                    .filter(test -> test.getName().equals(original.getName()))
                    .findAny();
            if (match.isPresent()) {
                // We have found a string with a matching name.
                if (!Objects.equals(original.getContent(), match.get().getContent())) {
                    // But they have non-matching contents, so we've found a change.
                    differences.add(new BibStringDiff(original, match.get()));
                }
                used.add(match.get());
            } else {
                // No match for this string.
                notMatched.add(original);
            }
        }

        // See if we can detect a name change for those entries that we couldn't match, based on their content
        for (Iterator<BibtexString> iterator = notMatched.iterator(); iterator.hasNext(); ) {
            BibtexString original = iterator.next();

            Optional<BibtexString> match = newDatabase
                    .getStringValues().stream()
                    .filter(test -> test.getName().equals(original.getName()))
                    .findAny();
            if (match.isPresent()) {
                // We have found a string with the same content. It cannot have the same
                // name, or we would have found it above.
                differences.add(new BibStringDiff(original, match.get()));
                iterator.remove();
                used.add(match.get());
            }
        }

        // Strings that are still not found must have been removed.
        for (BibtexString original : notMatched) {
            differences.add(new BibStringDiff(original, null));
        }

        // Finally, see if there are remaining strings in the new database. They must have been added.
        newDatabase.getStringValues().stream()
                   .filter(test -> !used.contains(test))
                   .forEach(newString -> differences.add(new BibStringDiff(null, newString)));

        return differences;
    }

    public BibtexString getOriginalString() {
        return originalString;
    }

    public BibtexString getNewString() {
        return newString;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }

        BibStringDiff that = (BibStringDiff) other;
        return Objects.equals(newString, that.newString) && Objects.equals(originalString, that.originalString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalString, newString);
    }
}
