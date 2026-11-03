package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class CitedEntryExporterTest {

    @Test
    void exportIncludesCrossrefOnce() {
        BibEntry parentEntry = new BibEntry(StandardEntryType.Book)
                .withCitationKey("Parent2024")
                .withField(StandardField.TITLE, "Parent title");
        BibEntry childEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Child2024")
                .withField(StandardField.TITLE, "Child title")
                .withField(StandardField.CROSSREF, "Parent2024");
        BibDatabase database = new BibDatabase(List.of(childEntry, parentEntry));

        CitedEntryExporter.ExportResult result = CitedEntryExporter.export(
                List.of("Child2024", "Child2024"),
                List.of(database));

        assertEquals(List.of(), result.unresolvedKeys());
        assertEquals(List.of(childEntry, parentEntry), result.database().getEntries().stream()
                                                             .sorted(Comparator.comparing(entry -> entry.getCitationKey().orElseThrow())).toList());
        assertNotSame(childEntry, result.database().getEntryByCitationKey("Child2024").orElseThrow());
        assertSame(parentEntry, result.database().getEntryByCitationKey("Parent2024").orElseThrow());
    }

    @Test
    void exportReportsUnresolvedKeysOnce() {
        BibEntry knownEntry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("Known2024");
        BibDatabase database = new BibDatabase(List.of(knownEntry));

        CitedEntryExporter.ExportResult result = CitedEntryExporter.export(
                List.of("Missing2024", "Missing2024"),
                List.of(database));

        assertEquals(List.of("Missing2024"), result.unresolvedKeys());
        assertEquals(List.of(), result.database().getEntries());
    }

    @Test
    void internalEntryIdsRemainUnresolvedWithoutCitationKeys() {
        BibEntry entryWithoutCitationKey = new BibEntry(StandardEntryType.Article)
                .withField(StandardField.TITLE, "Entry without citation key");
        BibDatabase database = new BibDatabase(List.of(entryWithoutCitationKey));

        CitedEntryExporter.ExportResult result = CitedEntryExporter.export(
                List.of(entryWithoutCitationKey.getId()),
                List.of(database));

        assertEquals(List.of(entryWithoutCitationKey.getId()), result.unresolvedKeys());
        assertEquals(List.of(), result.database().getEntries());
        assertEquals("Entry without citation key", entryWithoutCitationKey.getField(StandardField.TITLE).orElseThrow());
    }

    @Test
    void exportPreservesFirstAppearanceAndPrefersFirstDatabase() {
        BibEntry first = new BibEntry().withCitationKey("first").withField(StandardField.TITLE, "Selected title");
        BibEntry duplicate = new BibEntry().withCitationKey("first").withField(StandardField.TITLE, "Other title");
        BibEntry second = new BibEntry().withCitationKey("second");

        CitedEntryExporter.ExportResult result = CitedEntryExporter.export(
                List.of("second", "missing-second", "first", "missing-first", "second", "missing-second"),
                List.of(new BibDatabase(List.of(first)), new BibDatabase(List.of(duplicate, second))));

        assertEquals(List.of(second, first), result.database().getEntries());
        assertEquals(List.of("missing-second", "missing-first"), result.unresolvedKeys());
        assertNotSame(first, result.database().getEntryByCitationKey("first").orElseThrow());
        assertNotSame(second, result.database().getEntryByCitationKey("second").orElseThrow());
    }

    @Test
    void exportFollowsCrossrefsOnlyOneLevelWithinTheSelectedDatabase() {
        BibEntry child = new BibEntry().withCitationKey("child").withField(StandardField.CROSSREF, "parent");
        BibEntry parent = new BibEntry().withCitationKey("parent").withField(StandardField.CROSSREF, "grandparent");
        BibEntry grandparent = new BibEntry().withCitationKey("grandparent");
        BibEntry otherChild = new BibEntry().withCitationKey("other-child").withField(StandardField.CROSSREF, "other-parent");
        BibEntry otherParent = new BibEntry().withCitationKey("other-parent");

        CitedEntryExporter.ExportResult result = CitedEntryExporter.export(
                List.of("child", "other-child"),
                List.of(new BibDatabase(List.of(child, parent, grandparent, otherChild)), new BibDatabase(List.of(otherParent))));

        assertEquals(List.of(child, otherChild, parent), result.database().getEntries().stream()
                                                               .sorted(Comparator.comparing(entry -> entry.getCitationKey().orElseThrow())).toList());
        assertEquals(List.of(), result.unresolvedKeys());
    }

    @Test
    void exportResultCopiesUnresolvedKeysIntoAnImmutableList() {
        List<String> unresolvedKeys = new ArrayList<>(List.of("missing"));
        CitedEntryExporter.ExportResult result = new CitedEntryExporter.ExportResult(unresolvedKeys, new BibDatabase());

        unresolvedKeys.clear();

        assertEquals(List.of("missing"), result.unresolvedKeys());
        assertThrows(UnsupportedOperationException.class, () -> result.unresolvedKeys().add("other"));
    }
}
