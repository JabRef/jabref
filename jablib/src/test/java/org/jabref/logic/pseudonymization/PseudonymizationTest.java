package org.jabref.logic.pseudonymization;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.fileformat.BibtexImporter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.util.DummyFileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PseudonymizationTest {

    private BibtexImporter importer;

    private BibDatabaseWriter databaseWriter;
    private StringWriter stringWriter;
    private BibWriter bibWriter;
    private SelfContainedSaveConfiguration saveConfiguration;
    private FieldPreferences fieldPreferences;
    private CitationKeyPatternPreferences citationKeyPatternPreferences;
    private BibEntryTypesManager entryTypesManager;

    @BeforeEach
    void setUp() {
        importer = new BibtexImporter(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS), new DummyFileUpdateMonitor());

        stringWriter = new StringWriter();
        bibWriter = new BibWriter(stringWriter, "\n");
        saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);
        entryTypesManager = new BibEntryTypesManager();

        databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                entryTypesManager);
    }

    @Test
    void pseudonymizeTwoEntries() {
        BibEntry first = new BibEntry("first")
                .withField(StandardField.AUTHOR, "Author One")
                .withField(StandardField.PAGES, "some pages");
        BibEntry second = new BibEntry("second")
                .withField(StandardField.AUTHOR, "Author Two")
                .withField(StandardField.PAGES, "some pages");

        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(first, second)));

        Pseudonymization pseudonymization = new Pseudonymization();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        BibEntry firstPseudo = new BibEntry("citationkey-1")
                .withField(StandardField.AUTHOR, "author-1")
                .withField(StandardField.PAGES, "pages-1");
        BibEntry secondPseudo = new BibEntry("citationkey-2")
                .withField(StandardField.AUTHOR, "author-2")
                .withField(StandardField.PAGES, "pages-1");
        BibDatabaseContext bibDatabaseContextExpected = new BibDatabaseContext(new BibDatabase(List.of(firstPseudo, secondPseudo)));
        bibDatabaseContextExpected.setMode(BibDatabaseMode.BIBLATEX);
        Pseudonymization.Result expected = new Pseudonymization.Result(
                bibDatabaseContextExpected,
                Map.of("author-1", "Author One", "author-2", "Author Two", "pages-1", "some pages", "citationkey-1", "first", "citationkey-2", "second"));

        assertEquals(expected, result);
    }

    @Test
    void pseudonymizeLibrary() throws URISyntaxException, IOException {
        Path path = Path.of(PseudonymizationTest.class.getResource("Chocolate.bib").toURI());
        BibDatabaseContext databaseContext = importer.importDatabase(path).getDatabaseContext();

        Pseudonymization pseudonymization = new Pseudonymization();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);
        databaseWriter.writeDatabase(result.bibDatabaseContext());

        Path expectedPath = Path.of(PseudonymizationTest.class.getResource("Chocolate-pseudnomyized.bib").toURI());
        assertEquals(Files.readString(expectedPath), stringWriter.toString());
    }

    /**
     * This test can be used to anonymize a library.
     */
    @Test
    void pseudonymizeLibraryFile(@TempDir Path tempDir) throws URISyntaxException, IOException {
        // modify path to the file to be anonymized
        Path path = Path.of(PseudonymizationTest.class.getResource("Chocolate.bib").toURI());
        // modify target to the files to be created
        Path target = tempDir.resolve("pseudo.bib");
        Path mappingInfoTarget = target.resolveSibling("pseudo.bib.mapping.csv");

        BibDatabaseContext databaseContext = importer.importDatabase(path).getDatabaseContext();

        Pseudonymization pseudonymization = new Pseudonymization();
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);
        databaseWriter.writeDatabase(result.bibDatabaseContext());

        Files.writeString(target, stringWriter.toString());

        PseudonymizationResultCsvWriter.writeValuesMappingAsCsv(mappingInfoTarget, result);

        assertTrue(Files.exists(target));
    }

    @Test
    void pseudonymizeGroups() {
        // given
        GroupTreeNode root = new GroupTreeNode(new AllEntriesGroup("Root"));
        GroupTreeNode used = root.addSubgroup(new ExplicitGroup("Used", GroupHierarchyType.INDEPENDENT, ','));
        used.addSubgroup(new ExplicitGroup("Sub", GroupHierarchyType.INDEPENDENT, ','));

        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        databaseContext.getMetaData().setGroups(root);

        Pseudonymization pseudonymization = new Pseudonymization();

        // when
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);
        GroupTreeNode newRoot = result.bibDatabaseContext().getMetaData().getGroups().orElseThrow();

        // then
        assertEquals("group-1", newRoot.getName());
        assertTrue(newRoot.getFirstChild().isPresent());

        GroupTreeNode newUsed = newRoot.getFirstChild().orElseThrow();
        assertEquals("group-2", newUsed.getName());
        assertTrue(newUsed.getFirstChild().isPresent());

        GroupTreeNode newSub = newUsed.getFirstChild().orElseThrow();
        assertEquals("group-3", newSub.getName());

        Map<String, String> mapping = result.valueMapping();
        assertEquals("Root", mapping.get("group-1"));
        assertEquals("Used", mapping.get("group-2"));
        assertEquals("Sub", mapping.get("group-3"));
    }

    @Test
    void pseudonymizeEntriesWithGroup() {
        // given
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(
                new BibEntry("first").withField(StandardField.GROUPS, "MyGroup"),
                new BibEntry("second").withField(StandardField.GROUPS, "MyGroup, OtherGroup"),
                new BibEntry("third").withField(StandardField.GROUPS, "OtherGroup")
        )));

        Pseudonymization pseudonymization = new Pseudonymization();

        // when
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        // then
        List<BibEntry> entries = result.bibDatabaseContext().getEntries();
        assertEquals(3, entries.size());

        String myGroup1 = entries.getFirst().getField(StandardField.GROUPS).orElseThrow();
        String myGroup2 = entries.get(1).getField(StandardField.GROUPS).orElseThrow();
        String otherGroup = entries.get(2).getField(StandardField.GROUPS).orElseThrow();

        assertEquals(Optional.of("group-1, group-2"), entries.get(1).getField(StandardField.GROUPS));

        assertEquals(myGroup1, myGroup2);
        assertTrue(myGroup1.startsWith("group-"));
        assertTrue(otherGroup.startsWith("group-"));
        assertNotEquals(myGroup1, otherGroup);

        Map<String, String> mapping = result.valueMapping();
        assertEquals("MyGroup", mapping.get(myGroup1));
        assertEquals("OtherGroup", mapping.get(otherGroup));
    }

    @Test
    void pseudonymizeEntryWithMultipleGroups() {
        // given
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(
                new BibEntry("first").withField(StandardField.GROUPS, "one, two, three")
        )));

        Pseudonymization pseudonymization = new Pseudonymization();

        // when
        Pseudonymization.Result result = pseudonymization.pseudonymizeLibrary(databaseContext);

        // then
        BibEntry pseudonymizedEntry = result.bibDatabaseContext().getEntries().getFirst();
        String pseudonymizedGroups = pseudonymizedEntry.getField(StandardField.GROUPS).orElseThrow();

        String[] groups = pseudonymizedGroups.split(", ");
        assertEquals(3, groups.length);

        assertEquals("group-1", groups[0]);
        assertEquals("group-2", groups[1]);
        assertEquals("group-3", groups[2]);

        Map<String, String> mapping = result.valueMapping();
        assertEquals("one", mapping.get(groups[0]));
        assertEquals("two", mapping.get(groups[1]));
        assertEquals("three", mapping.get(groups[2]));
    }
}
