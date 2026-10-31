package org.jabref.logic.git.diff;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;

import org.jspecify.annotations.NullMarked;

/// This class is to generate a commit message that summarizes how the library has
/// changed since the last commit
/// Only entry changes are described. If only the group tree,
/// the preamble or `@String`s changed, there is no message
///
/// The message is always in English
@NullMarked
public final class GitCommitMessageGenerator {
    private GitCommitMessageGenerator() {
    }

    // [impl->req~git.commit.generated-message~1]
    // [impl->adr~english-only-git-commit-messages~1]

    /// Compare two libraries
    /// If there is exactly one difference, try the specific bonus message
    /// Otherwise, do the count of added, deleted, modified entries
    /// If nothing changed, return empty.
    public static Optional<String> generate(BibDatabaseContext headDatabase, BibDatabaseContext workingTreeDatabase) {
        List<BibEntryDiff> entryDiffs = BibDatabaseDiff.compare(headDatabase, workingTreeDatabase).getEntryDifferences();
        Optional<String> singleFieldMessage = entryDiffs.size() == 1
                                              ? singleFieldMessage(entryDiffs.getFirst())
                                              : Optional.empty();
        return singleFieldMessage.or(() -> countMessage(entryDiffs));
    }

    /// Check all changes entries to create message
    private static Optional<String> countMessage(List<BibEntryDiff> entryDiffs) {
        long added = entryDiffs.stream().filter(GitCommitMessageGenerator::isAddition).count();
        long deleted = entryDiffs.stream().filter(GitCommitMessageGenerator::isDeletion).count();
        long modified = entryDiffs.size() - added - deleted;

        List<String> parts = new ArrayList<>();
        if (added > 0) {
            parts.add("add " + entries(added));
        }
        if (modified > 0) {
            parts.add("modify " + entries(modified));
        }
        if (deleted > 0) {
            parts.add("delete " + entries(deleted));
        }
        if (parts.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(StringUtil.capitalizeFirst(String.join(", ", parts)));
    }

    private static boolean isAddition(BibEntryDiff diff) {
        return Optional.ofNullable(diff.originalEntry()).isEmpty();
    }

    private static boolean isDeletion(BibEntryDiff diff) {
        return Optional.ofNullable(diff.newEntry()).isEmpty();
    }

    private static String entries(long count) {
        return count == 1 ? "1 entry" : count + " entries";
    }

    /// Message for single field of single entry changed
    /// For example: Modify author of Smith2024
    private static Optional<String> singleFieldMessage(BibEntryDiff diff) {
        // Check whether this is modification entry
        return Optional.ofNullable(diff.originalEntry())
                       .flatMap(original -> Optional.ofNullable(diff.newEntry())
                                                    .flatMap(changed -> singleFieldMessage(original, changed)));
    }

    private static Optional<String> singleFieldMessage(BibEntry original, BibEntry changed) {
        LinkedHashSet<Field> changedFields = changedFields(original, changed);
        boolean onlyOneFieldChanged = changedFields.size() == 1
                && original.getType().equals(changed.getType());
        if (!onlyOneFieldChanged) {
            return Optional.empty();
        }
        Field field = changedFields.getFirst();
        if (InternalField.KEY_FIELD == field) {
            return citationKeyMessage(original, changed);
        }
        return changed.getCitationKey()
                      .map(citationKey -> "Modify " + field.getName() + " of " + citationKey);
    }

    private static Optional<String> citationKeyMessage(BibEntry original, BibEntry changed) {
        return original.getCitationKey()
                       .flatMap(oldKey -> changed.getCitationKey()
                                                 .map(newKey -> "Rename citation key " + oldKey + " to " + newKey));
    }

    private static LinkedHashSet<Field> changedFields(BibEntry original, BibEntry changed) {
        LinkedHashSet<Field> fields = new LinkedHashSet<>(original.getFields());
        fields.addAll(changed.getFields());
        fields.removeIf(field -> original.getField(field).equals(changed.getField(field)));
        return fields;
    }
}

