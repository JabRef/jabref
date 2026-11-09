package org.jabref.logic.bibtex.comparator;

import java.text.Collator;
import java.util.Comparator;

import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import org.jspecify.annotations.NullMarked;

/// Compares parsed people rather than storage syntax or display abbreviations.
/// Explicit name-level `useprefix` controls the primary family-name component.
/// Missing or invalid options retain JabRef's family-first sorting policy.
@NullMarked
public class NameComparator implements Comparator<String> {
    private static final RemoveEnclosingBracesFormatter BRACES = new RemoveEnclosingBracesFormatter();

    private final Collator collator = Collator.getInstance();

    public NameComparator() {
        collator.setStrength(Collator.SECONDARY);
        collator.setDecomposition(Collator.CANONICAL_DECOMPOSITION);
    }

    // Collator is mutable and not thread-safe;
    // an instance can be reused by multiple sort operations.
    @Override
    public synchronized int compare(String left, String right) {
        AuthorList leftAuthors = AuthorList.parse(left).latexFree();
        AuthorList rightAuthors = AuthorList.parse(right).latexFree();
        int commonAuthors = Math.min(leftAuthors.getNumberOfAuthors(), rightAuthors.getNumberOfAuthors());
        for (int index = 0; index < commonAuthors; index++) {
            int result = compareAuthor(leftAuthors.getAuthor(index), rightAuthors.getAuthor(index));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(leftAuthors.getNumberOfAuthors(), rightAuthors.getNumberOfAuthors());
    }

    private int compareAuthor(Author left, Author right) {
        int result = collator.compare(primaryName(left), primaryName(right));
        if (result != 0) {
            return result;
        }
        result = collator.compare(left.getNameSuffix().orElse(""), right.getNameSuffix().orElse(""));
        if (result != 0) {
            return result;
        }
        // Use full given names so display initials never determine the order of distinct people.
        result = collator.compare(left.getGivenName().or(left::getExplicitGivenNameInitials).orElse(""),
                right.getGivenName().or(right::getExplicitGivenNameInitials).orElse(""));
        if (result != 0) {
            return result;
        }
        return collator.compare(left.getNamePrefix().orElse(""), right.getNamePrefix().orElse(""));
    }

    private String primaryName(Author author) {
        String family = BRACES.format(author.getFamilyName().orElse(""));
        if (!author.getUsePrefix().orElse(false)) {
            return family;
        }
        return author.getNamePrefix()
                     .map(prefix -> prefix + ((prefix.endsWith("'") || prefix.endsWith("’")) ? "" : " ") + family)
                     .orElse(family);
    }
}
