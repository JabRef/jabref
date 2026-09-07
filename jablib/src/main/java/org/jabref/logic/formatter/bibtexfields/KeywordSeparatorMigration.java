package org.jabref.logic.formatter.bibtexfields;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.undo.UndoableGroupChange;

import org.jspecify.annotations.NullMarked;

/// Migrates keyword-aware data when a library's keyword separator changes.
@NullMarked
public final class KeywordSeparatorMigration {

    private KeywordSeparatorMigration() {
    }

    /// Rewrites `keywords` and `groups` fields using the new separator.
    public static List<FieldChange> migrateEntryFields(BibDatabaseContext databaseContext, Character previousSeparator, Character newSeparator) {
        List<FieldChange> changes = new ArrayList<>();
        for (BibEntry entry : databaseContext.getEntries()) {
            migrateField(entry, StandardField.KEYWORDS, previousSeparator, newSeparator).ifPresent(changes::add);
            migrateField(entry, StandardField.GROUPS, previousSeparator, newSeparator).ifPresent(changes::add);
        }
        return changes;
    }

    /// Updates group definitions that capture a keyword separator.
    public static List<UndoableGroupChange> migrateGroupSeparators(BibDatabaseContext databaseContext, Character newSeparator) {
        List<UndoableGroupChange> changes = new ArrayList<>();
        databaseContext.getMetaData().getGroups().ifPresent(root -> root.iterateOverTree().forEach(node -> {
            AbstractGroup previousGroup = node.getGroup();
            AbstractGroup newGroup = withKeywordSeparator(previousGroup, newSeparator);
            if (previousGroup != newGroup) {
                node.setGroup(newGroup);
                changes.add(new UndoableGroupChange(node, previousGroup, newGroup));
            }
        }));
        return changes;
    }

    private static Optional<FieldChange> migrateField(BibEntry entry,
                                                      StandardField field,
                                                      Character previousSeparator,
                                                      Character newSeparator) {
        return entry.getField(field)
                    .flatMap(value -> entry.setField(field, KeywordList.serializeWithSpaces(
                            KeywordList.parse(value, previousSeparator).stream().toList(), newSeparator)));
    }

    private static AbstractGroup withKeywordSeparator(AbstractGroup group, Character newSeparator) {
        return switch (group) {
            case WordKeywordGroup wordKeywordGroup ->
                    wordKeywordGroup.withKeywordSeparator(newSeparator);
            case AutomaticKeywordGroup automaticKeywordGroup ->
                    automaticKeywordGroup.withKeywordSeparator(newSeparator);
            default ->
                    group;
        };
    }
}
