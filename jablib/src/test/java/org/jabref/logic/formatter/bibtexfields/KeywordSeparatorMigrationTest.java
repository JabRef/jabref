package org.jabref.logic.formatter.bibtexfields;

import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableGroupChange;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeywordSeparatorMigrationTest {

    @Test
    void migratesKeywordAwareFieldsAndGroups() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.KEYWORDS, "topic, subtopic")
                .withField(StandardField.GROUPS, "library, selected");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        root.addSubgroup(new ExplicitGroup("selected", GroupHierarchyType.INDEPENDENT, ','));
        root.addSubgroup(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ',', true));
        root.addSubgroup(new AutomaticKeywordGroup("automatic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '/'));
        databaseContext.getMetaData().setGroups(root);

        List<UndoableFieldChange> fieldChanges = KeywordSeparatorMigration
                .migrateEntryFields(databaseContext, ',', ';')
                .stream()
                .map(UndoableFieldChange::new)
                .toList();
        List<UndoableGroupChange> groupChanges = KeywordSeparatorMigration.migrateGroupSeparators(databaseContext, ';');

        assertEquals("topic; subtopic", entry.getField(StandardField.KEYWORDS).orElseThrow());
        assertEquals("library; selected", entry.getField(StandardField.GROUPS).orElseThrow());
        assertEquals(new ExplicitGroup("selected", GroupHierarchyType.INDEPENDENT, ';'), root.getChildren().getFirst().getGroup());
        assertEquals(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ';', true), root.getChildren().get(1).getGroup());
        assertEquals(new AutomaticKeywordGroup("automatic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ';', '/'), root.getChildren().get(2).getGroup());

        groupChanges.reversed().forEach(change -> change.inverted().apply());
        fieldChanges.reversed().forEach(change -> change.inverted().apply());

        assertEquals("topic, subtopic", entry.getField(StandardField.KEYWORDS).orElseThrow());
        assertEquals("library, selected", entry.getField(StandardField.GROUPS).orElseThrow());
        assertEquals(new ExplicitGroup("selected", GroupHierarchyType.INDEPENDENT, ','), root.getChildren().getFirst().getGroup());
        assertEquals(new WordKeywordGroup("topic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "topic", true, ',', true), root.getChildren().get(1).getGroup());
        assertEquals(new AutomaticKeywordGroup("automatic", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, ',', '/'), root.getChildren().get(2).getGroup());
    }
}
