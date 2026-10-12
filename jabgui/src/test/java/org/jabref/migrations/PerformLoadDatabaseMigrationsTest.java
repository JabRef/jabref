package org.jabref.migrations;

import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import javafx.collections.ListChangeListener;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.SaveOrder;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class PerformLoadDatabaseMigrationsTest {

    @Test
    void migrationBatchesChangesAndRestoresEntryObservation() {
        List<BibEntry> entries = IntStream.range(0, 1000)
                                          .mapToObj(index -> new BibEntry().withCitationKey("Entry" + index)
                                                                           .withField(InternalField.MARKED_INTERNAL, "[Owner:6]")
                                                                           .withField(StandardField.KEYWORDS, "topic, printed"))
                                          .toList();
        ParserResult result = new ParserResult(entries);
        List<Integer> updateSizes = new ArrayList<>();
        result.getDatabase().getEntries().addListener((ListChangeListener<BibEntry>) change -> {
            while (change.next()) {
                if (change.wasUpdated()) {
                    updateSizes.add(change.getTo() - change.getFrom());
                }
            }
        });

        PerformLoadDatabaseMigrations.performLoadDatabaseMigrations(result, ',');

        assertEquals(List.of(), updateSizes);
        assertEquals(entries, result.getDatabase().getEntries());
        assertSame(entries.getFirst(), result.getDatabase().getEntries().getFirst());
        assertEquals(Optional.of("Owner:6"), entries.getFirst().getField(StandardField.GROUPS));
        assertEquals(Optional.of("printed"), entries.getLast().getField(SpecialField.PRINTED));
        assertTrue(result.getChangedOnMigration());

        entries.getFirst().setField(StandardField.TITLE, "Changed after migration");

        assertEquals(List.of(1), updateSizes);
    }

    @Test
    void migrationPreservesCitationLinksAndRebuildsReferenceIndex() {
        BibEntry parent = new BibEntry().withCitationKey("Parent");
        BibEntry child = new BibEntry().withCitationKey("Child")
                                       .withField(StandardField.CROSSREF, "Parent")
                                       .withField(InternalField.MARKED_INTERNAL, "[Owner:6]");
        ParserResult result = new ParserResult(List.of(parent, child));

        PerformLoadDatabaseMigrations.performLoadDatabaseMigrations(result, ',');

        assertEquals(Optional.of("Parent"), child.getField(StandardField.CROSSREF));
        assertEquals(Set.of(child), result.getDatabase().getEntriesForCitationKey("Parent"));
        assertSame(parent, result.getDatabase().getEntryByCitationKey("Parent").orElseThrow());

        parent.setCitationKey("Renamed");

        assertEquals(Optional.of("Renamed"), child.getField(StandardField.CROSSREF));
        assertEquals(Set.of(child), result.getDatabase().getEntriesForCitationKey("Renamed"));
    }

    @Test
    void modernLibraryDoesNotTriggerMigrationOrListChanges() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "research, topic")
                                       .withField(StandardField.GROUPS, "Current group");
        ParserResult result = new ParserResult(List.of(entry));
        result.getMetaData().setGroups(GroupTreeNode.fromGroup(new ExplicitGroup("Current group", GroupHierarchyType.INDEPENDENT, ',')));
        List<Integer> changes = new ArrayList<>();
        result.getDatabase().getEntries().addListener((ListChangeListener<BibEntry>) change -> {
            while (change.next()) {
                changes.add(change.getTo() - change.getFrom());
            }
        });

        PerformLoadDatabaseMigrations.performLoadDatabaseMigrations(result, ',');

        assertEquals(List.of(), changes);
        assertFalse(result.getChangedOnMigration());
        assertFalse(new ConvertLegacyExplicitGroups().isMigrationNecessary(result));
        assertFalse(new ConvertMarkingToGroups(',').isMigrationNecessary(result));
        assertFalse(new SpecialFieldsToSeparateFields(',').isMigrationNecessary(result));
    }

    // [utest->req~import.bibtex.legacy-migrations.only-when-needed~1]
    @Test
    void savedAndReopenedLibraryDoesNotNeedAnotherMigration() throws IOException {
        BibEntry entry = new BibEntry().withCitationKey("Entry")
                                       .withField(StandardField.GROUPS, "Existing")
                                       .withField(StandardField.KEYWORDS, "topic; printed; prio1")
                                       .withField(InternalField.MARKED_INTERNAL, "[Owner:6]");
        ParserResult result = new ParserResult(List.of(entry));
        result.getMetaData().setKeywordSeparator(';');
        GroupTreeNode root = GroupTreeNode.fromGroup(new AllEntriesGroup("All entries"));
        ExplicitGroup legacyGroup = new ExplicitGroup("Legacy", GroupHierarchyType.INDEPENDENT, ';');
        legacyGroup.addLegacyEntryKey("Entry");
        root.addSubgroup(legacyGroup);
        result.getMetaData().setGroups(root);

        PerformLoadDatabaseMigrations.performLoadDatabaseMigrations(result, ';');

        StringWriter saved = new StringWriter();
        new BibDatabaseWriter(new BibWriter(saved, "\n"),
                new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false),
                new FieldPreferences(true, List.of(), List.of()),
                mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS),
                new BibEntryTypesManager()).writeDatabase(result.getDatabaseContext());
        ImportFormatPreferences preferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.bibEntryPreferences().getKeywordSeparator()).thenReturn(',');
        ParserResult reopened = new BibtexParser(preferences).parse(Reader.of(saved.toString()));

        PerformLoadDatabaseMigrations.performLoadDatabaseMigrations(reopened, reopened.getDatabaseContext().getKeywordSeparator(','));

        assertFalse(reopened.getChangedOnMigration());
        assertEquals(Optional.of(';'), reopened.getMetaData().getKeywordSeparator());
        assertEquals(Optional.of("Existing; Legacy; Owner:6"), reopened.getDatabase().getEntries().getFirst().getField(StandardField.GROUPS));
        assertEquals(Optional.of("topic"), reopened.getDatabase().getEntries().getFirst().getField(StandardField.KEYWORDS));
        assertFalse(new ConvertLegacyExplicitGroups().isMigrationNecessary(reopened));
        assertFalse(new ConvertMarkingToGroups(';').isMigrationNecessary(reopened));
        assertFalse(new SpecialFieldsToSeparateFields(';').isMigrationNecessary(reopened));
    }
}
