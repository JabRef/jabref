package org.jabref.logic.exporter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.GroupTreeNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownPublicationExporterTest {

    // [utest->feat~export.markdown-publications~1]
    // [utest->req~export.markdown-publications.date-order~1]
    @Test
    void exportsFiveMostRecentPublicationsFirst(@TempDir Path tempDir)
            throws Exception {
        List<BibEntry> entries = List.of(
                publication(2022),
                publication(2020),
                publication(2025),
                publication(2021),
                publication(2024),
                publication(2023)
        );

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                entries
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025
                * Paper 2024. 2024
                * Paper 2023. 2023
                * Paper 2022. 2022
                * Paper 2021. 2021

                """, recentSection(output));
    }

    @Test
    void exportsAllAvailablePublicationsWhenFewerThanFive(
            @TempDir Path tempDir) throws Exception {
        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(publication(2023), publication(2025))
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025
                * Paper 2023. 2023

                """, recentSection(output));
    }

    @Test
    void retainsOlderPublicationsInFullList(@TempDir Path tempDir)
            throws Exception {
        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(
                        publication(2022),
                        publication(2020),
                        publication(2025),
                        publication(2021),
                        publication(2024),
                        publication(2023)
                )
        );

        String markdown = Files.readString(output).replace("\r\n", "\n");

        String fullList = markdown.substring(
                markdown.indexOf("## All publications\n")
        );

        assertEquals("""
                ## All publications

                * Paper 2025. 2025
                * Paper 2024. 2024
                * Paper 2023. 2023
                * Paper 2022. 2022
                * Paper 2021. 2021
                * Paper 2020. 2020
                """, fullList);
    }

    @Test
    void sortsSameYearByMonthAndDay(@TempDir Path tempDir)
            throws Exception {
        BibEntry january = publication(2025)
                .withCitationKey("january")
                .withField(StandardField.TITLE, "January paper")
                .withField(StandardField.MONTH, "jan")
                .withField(StandardField.DAY, "20");

        BibEntry earlyJune = publication(2025)
                .withCitationKey("earlyJune")
                .withField(StandardField.TITLE, "Early June paper")
                .withField(StandardField.MONTH, "jun")
                .withField(StandardField.DAY, "5");

        BibEntry lateJune = publication(2025)
                .withCitationKey("lateJune")
                .withField(StandardField.TITLE, "Late June paper")
                .withField(StandardField.MONTH, "jun")
                .withField(StandardField.DAY, "25");

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(january, earlyJune, lateJune)
        );

        assertEquals("""
                ## Recent publications

                * Late June paper. 2025
                * Early June paper. 2025
                * January paper. 2025

                """, recentSection(output));
    }

    @Test
    void sortsUsingBiblatexDateField(@TempDir Path tempDir)
            throws Exception {
        BibEntry older = publication(2025)
                .withCitationKey("older")
                .withField(StandardField.TITLE, "Older paper")
                .withField(StandardField.DATE, "2025-02-10");

        BibEntry newer = publication(2025)
                .withCitationKey("newer")
                .withField(StandardField.TITLE, "Newer paper")
                .withField(StandardField.DATE, "2025-09-15");

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(older, newer)
        );

        assertEquals("""
                ## Recent publications

                * Newer paper. 2025
                * Older paper. 2025

                """, recentSection(output));
    }

    @ParameterizedTest
    @CsvSource({
            "10.1000/182, '', https://doi.org/10.1000/182",
            "'', https://example.org/paper, https://example.org/paper",
            "10.1000/182, https://example.org/paper, https://doi.org/10.1000/182"
    })
    void linksTitleUsingDoiBeforeUrl(
            String doi,
            String url,
            String expectedLink,
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication(2025);

        if (!doi.isBlank()) {
            entry.setField(StandardField.DOI, doi);
        }

        if (!url.isBlank()) {
            entry.setField(StandardField.URL, url);
        }

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * [Paper 2025](<%s>). 2025

                """.formatted(expectedLink), recentSection(output));
    }

    @Test
    void addsLinkToPdfAttachment(@TempDir Path tempDir)
            throws Exception {
        BibEntry entry = publication(2025)
                .withFiles(List.of(
                        new LinkedFile(
                                "Full text",
                                "https://example.org/paper.pdf",
                                "PDF"
                        )
                ));

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<https://example.org/paper.pdf>)

                """, recentSection(output));
    }

    @ParameterizedTest
    @CsvSource({
            "paper.pdf",
            "paper with spaces.pdf"
    })
    void linksToExistingAbsoluteLocalPdf(
            String fileName,
            @TempDir Path tempDir) throws Exception {
        Path pdf = Files.createFile(tempDir.resolve(fileName));

        BibEntry entry = publication(2025)
                .withFiles(List.of(
                        new LinkedFile(
                                "Local full text",
                                pdf.toAbsolutePath().toString(),
                                "PDF"
                        )
                ));

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<%s>)

                """.formatted(fileName.replace(" ", "%20")), recentSection(output));
    }

    @Test
    void resolvesRelativePdfUsingProvidedFileDirectory(
            @TempDir Path tempDir) throws Exception {
        Path attachmentDirectory =
                Files.createDirectory(tempDir.resolve("attachments"));
        Path pdf = Files.createFile(
                attachmentDirectory.resolve("relative paper.pdf")
        );

        BibEntry entry = publication(2025)
                .withFiles(List.of(
                        new LinkedFile(
                                "Relative full text",
                                "relative paper.pdf",
                                "PDF"
                        )
                ));

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry),
                List.of(attachmentDirectory),
                JournalAbbreviationLoader.loadBuiltInRepository()
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<%s>)

                """.formatted("attachments/relative%20paper.pdf"), recentSection(output));
    }

    @Test
    void omitsLinkWhenLocalPdfDoesNotExist(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication(2025)
                .withFiles(List.of(
                        new LinkedFile(
                                "Missing full text",
                                tempDir.resolve("missing.pdf").toString(),
                                "PDF"
                        )
                ));

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025

                """, recentSection(output));
    }

    @Test
    void ignoresNonPdfAttachments(@TempDir Path tempDir)
            throws Exception {
        BibEntry entry = publication(2025)
                .withFiles(List.of(
                        new LinkedFile(
                                "Supplement",
                                "https://example.org/supplement.txt",
                                "TXT"
                        )
                ));

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025

                """, recentSection(output));
    }

    @Test
    void skipsMissingPdfAndUsesNextAvailablePdf(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication(2025)
                .withFiles(List.of(
                        new LinkedFile(
                                "Missing local file",
                                tempDir.resolve("missing.pdf").toString(),
                                "PDF"
                        ),
                        new LinkedFile(
                                "Available online file",
                                "https://example.org/available.pdf",
                                "PDF"
                        )
                ));

        Path output = tempDir.resolve("publications.md");

        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<https://example.org/available.pdf>)

                """, recentSection(output));
    }

    private MarkdownPublicationExporter createExporter() {
        LayoutFormatterPreferences preferences = LayoutFormatterPreferences.getDefault();

        return new MarkdownPublicationExporter(preferences);
    }

    private BibEntry publication(int year) {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey("paper" + year)
                .withField(StandardField.TITLE, "Paper " + year)
                .withField(StandardField.YEAR, Integer.toString(year));
    }

    private String recentSection(Path output) throws Exception {
        String markdown = Files.readString(output).replace("\r\n", "\n");

        return markdown.substring(
                markdown.indexOf("## Recent publications\n"),
                markdown.indexOf("## All publications\n")
        );
    }

    // [utest->req~export.markdown-publications.groups~1]
    @Test
    void usesDescriptionAndIncludesOnlyMatchingEntriesInDateOrder(
            @TempDir Path tempDir) throws Exception {
        GroupTreeNode root =
                new GroupTreeNode(new AllEntriesGroup("All entries"));
        GroupTreeNode exportGroup = root.addSubgroup(group("Export"));

        ExplicitGroup articles = group("Articles");
        articles.setDescription("Journal publications");
        GroupTreeNode articleNode = exportGroup.addSubgroup(articles);

        BibEntry older = publication("Older paper", 2023);
        BibEntry newer = publication("Newer paper", 2025);
        BibEntry unrelated = publication("Unrelated paper", 2024);

        articleNode.addEntriesToGroup(List.of(older, newer));

        String markdown = export(
                tempDir,
                root,
                List.of(older, unrelated, newer)
        );

        String section = markdown.substring(
                markdown.indexOf("### Journal publications\n")
        );

        assertEquals("""
                ### Journal publications

                * Newer paper. 2025
                * Older paper. 2023
                """, section);
    }

    @Test
    void fallsBackToGroupNameWhenDescriptionIsBlank(
            @TempDir Path tempDir) throws Exception {
        GroupTreeNode root =
                new GroupTreeNode(new AllEntriesGroup("All entries"));
        GroupTreeNode exportGroup = root.addSubgroup(group("Export"));

        ExplicitGroup books = group("Books");
        books.setDescription("   ");
        GroupTreeNode bookNode = exportGroup.addSubgroup(books);

        BibEntry entry = publication("Example book", 2025);
        bookNode.addEntriesToGroup(List.of(entry));

        String markdown = export(tempDir, root, List.of(entry));

        assertEquals("""
                ### Books

                * Example book. 2025
                """, markdown.substring(markdown.indexOf("### Books\n")));
    }

    @Test
    void exportsNestedSubgroupsWithNestedHeadings(
            @TempDir Path tempDir) throws Exception {
        GroupTreeNode root =
                new GroupTreeNode(new AllEntriesGroup("All entries"));
        GroupTreeNode exportGroup = root.addSubgroup(group("Export"));

        GroupTreeNode articles = exportGroup.addSubgroup(group("Articles"));
        GroupTreeNode selected = articles.addSubgroup(group("Selected"));

        BibEntry parentEntry = publication("General paper", 2024);
        BibEntry childEntry = publication("Selected paper", 2025);

        articles.addEntriesToGroup(List.of(parentEntry));
        selected.addEntriesToGroup(List.of(childEntry));

        String markdown = export(
                tempDir,
                root,
                List.of(parentEntry, childEntry)
        );

        assertEquals("""
                ### Articles

                * General paper. 2024

                #### Selected

                * Selected paper. 2025
                """, markdown.substring(markdown.indexOf("### Articles\n")));
    }

    @Test
    void ignoresGroupsOutsideExport(
            @TempDir Path tempDir) throws Exception {
        GroupTreeNode root =
                new GroupTreeNode(new AllEntriesGroup("All entries"));
        root.addSubgroup(group("Export"));

        GroupTreeNode privateGroup =
                root.addSubgroup(group("Private notes"));
        BibEntry entry = publication("Example paper", 2025);
        privateGroup.addEntriesToGroup(List.of(entry));

        String markdown = export(tempDir, root, List.of(entry));

        assertFalse(markdown.contains("### Private notes"));
        assertFalse(markdown.contains("### "));
    }

    private ExplicitGroup group(String name) {
        return new ExplicitGroup(
                name,
                GroupHierarchyType.INDEPENDENT,
                ';'
        );
    }

    private BibEntry publication(String title, int year) {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey(title.replace(" ", ""))
                .withField(StandardField.TITLE, title)
                .withField(StandardField.YEAR, Integer.toString(year));
    }

    private String export(
            Path tempDir,
            GroupTreeNode root,
            List<BibEntry> entries) throws Exception {
        BibDatabaseContext context = new BibDatabaseContext();
        context.getMetaData().setGroups(root);

        LayoutFormatterPreferences preferences = LayoutFormatterPreferences.getDefault();

        Path output = tempDir.resolve("grouped-publications.md");

        new MarkdownPublicationExporter(preferences).export(
                context,
                output,
                entries
        );

        return Files.readString(output).replace("\r\n", "\n");
    }

    @Test
    void displaysYearFromBiblatexDateWithoutYearField(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article)
                .withCitationKey("dateOnly")
                .withField(StandardField.TITLE, "Date-only paper")
                .withField(StandardField.DATE, "2025-09-15");

        assertEquals("""
                ## Recent publications

                * Date-only paper. 2025

                """, exportRecentSection(tempDir, entry));
    }

    @Test
    void escapesMarkdownCharactersInLinkedTitle(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("Paper *star* _under_ [open")
                .withField(StandardField.URL, "https://example.org/paper");

        assertEquals("""
                ## Recent publications

                * [Paper \\*star\\* \\_under\\_ \\[open](<https://example.org/paper>). 2025

                """, exportRecentSection(tempDir, entry));
    }

    @Test
    void avoidsDuplicatePeriodAfterLinkedTitle(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("Example paper.")
                .withField(StandardField.URL, "https://example.org/paper");

        assertEquals("""
                ## Recent publications

                * [Example paper](<https://example.org/paper>). 2025

                """, exportRecentSection(tempDir, entry));
    }

    @Test
    void convertsLatexAccentsToUnicode(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("M{\\\"o}nch");

        assertEquals("""
                ## Recent publications

                * Mönch. 2025

                """, exportRecentSection(tempDir, entry));
    }

    @Test
    void includesAuthorAndJournal(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("Example paper")
                .withField(StandardField.AUTHOR, "Alice Smith")
                .withField(StandardField.JOURNAL, "Example Journal");

        assertEquals("""
                ## Recent publications

                * Alice Smith. Example paper. Example Journal. 2025

                """, exportRecentSection(tempDir, entry));
    }

    @Test
    void usesConferenceVenueWhenJournalIsAbsent(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("Conference paper")
                .withField(StandardField.BOOKTITLE, "Example Conference");

        assertEquals("""
                ## Recent publications

                * Conference paper. Example Conference. 2025

                """, exportRecentSection(tempDir, entry));
    }

    @Test
    void prefersJournalWhenBothVenueFieldsExist(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("Example paper")
                .withField(StandardField.JOURNAL, "Example Journal")
                .withField(StandardField.BOOKTITLE, "Other Venue");

        assertEquals("""
                ## Recent publications

                * Example paper. Example Journal. 2025

                """, exportRecentSection(tempDir, entry));
    }

    private BibEntry publication(String title) {
        return new BibEntry(StandardEntryType.Article)
                .withCitationKey("example")
                .withField(StandardField.TITLE, title)
                .withField(StandardField.YEAR, "2025");
    }

    private String exportRecentSection(
            Path tempDir,
            BibEntry entry) throws Exception {
        Path output = tempDir.resolve("edge-cases.md");

        new MarkdownPublicationExporter(
                LayoutFormatterPreferences.getDefault()
        ).export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        String markdown = Files.readString(output)
                               .replace("\r\n", "\n");

        return markdown.substring(
                markdown.indexOf("## Recent publications\n"),
                markdown.indexOf("## All publications\n")
        );
    }

    @Test
    void omitsEmptySubgroupSections(@TempDir Path tempDir)
            throws Exception {
        GroupTreeNode root =
                new GroupTreeNode(new AllEntriesGroup("All entries"));
        GroupTreeNode exportGroup = root.addSubgroup(group("Export"));
        exportGroup.addSubgroup(group("Empty section"));

        String markdown = export(
                tempDir,
                root,
                List.of(publication("Example paper", 2025))
        );

        assertFalse(markdown.contains("### Empty section"));
    }

    @Test
    void retainsParentHeadingForNonemptyNestedSubgroup(
            @TempDir Path tempDir) throws Exception {
        GroupTreeNode root =
                new GroupTreeNode(new AllEntriesGroup("All entries"));
        GroupTreeNode exportGroup = root.addSubgroup(group("Export"));
        GroupTreeNode parent = exportGroup.addSubgroup(group("Parent"));
        GroupTreeNode child = parent.addSubgroup(group("Child"));

        BibEntry entry = publication("Nested paper", 2025);
        child.addEntriesToGroup(List.of(entry));

        String markdown = export(tempDir, root, List.of(entry));

        assertEquals("""
                ### Parent

                #### Child

                * Nested paper. 2025
                """, markdown.substring(markdown.indexOf("### Parent\n")));
    }

    @ParameterizedTest
    @CsvSource({
            "paper.pdf",
            "paper.PDF"
    })
    void detectsPdfExtensionWhenFileTypeIsMissing(
            String fileName,
            @TempDir Path tempDir) throws Exception {
        Path pdf = Files.createFile(tempDir.resolve(fileName));

        BibEntry entry = publication(2025).withFiles(List.of(
                new LinkedFile(
                        "Full text",
                        pdf.toAbsolutePath().toString(),
                        ""
                )
        ));

        Path output = tempDir.resolve("publications.md");
        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<%s>)

                """.formatted(fileName), recentSection(output));
    }

    // [utest->req~export.markdown-publications.links~1]
    @Test
    void relativePdfLinkSurvivesMovingExportAndAttachmentsTogether(
            @TempDir Path tempDir) throws Exception {
        Path bundle = Files.createDirectory(tempDir.resolve("original"));
        Path pdfDirectory = Files.createDirectory(bundle.resolve("pdfs"));
        Path reportDirectory =
                Files.createDirectory(bundle.resolve("reports"));
        Path pdf = Files.createFile(pdfDirectory.resolve("paper.pdf"));

        BibEntry entry = publication(2025).withFiles(List.of(
                new LinkedFile(
                        "Full text",
                        pdf.toAbsolutePath().toString(),
                        "PDF"
                )
        ));

        Path output = reportDirectory.resolve("publications.md");
        createExporter().export(
                new BibDatabaseContext(),
                output,
                List.of(entry)
        );

        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<../pdfs/paper.pdf>)

                """, recentSection(output));

        Path moved = Files.move(bundle, tempDir.resolve("relocated"));
        Path movedOutput = moved.resolve("reports/publications.md");

        assertTrue(Files.exists(
                movedOutput.getParent().resolve("../pdfs/paper.pdf")
        ));
        assertEquals("""
                ## Recent publications

                * Paper 2025. 2025 [PDF](<../pdfs/paper.pdf>)

                """, recentSection(movedOutput));
    }

    @Test
    void avoidsDuplicatePeriodAfterAuthorInitial(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("Example paper")
                .withField(StandardField.AUTHOR, "Smith, Alice J.");

        assertEquals("""
                ## Recent publications

                * Smith, Alice J. Example paper. 2025

                """, exportRecentSection(tempDir, entry));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Research question?",
            "Important finding!"
    })
    void avoidsExtraPeriodAfterLinkedTitlePunctuation(
            String title,
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication(title)
                .withField(StandardField.URL, "https://example.org/paper");

        assertEquals("""
                ## Recent publications

                * [%s](<https://example.org/paper>) 2025

                """.formatted(title),
                exportRecentSection(tempDir, entry));
    }

    @ParameterizedTest
    @CsvSource({
            "'Research question?'",
            "'Important finding!'"
    })
    void avoidsExtraPeriodAfterUnlinkedTitlePunctuation(
            String title,
            @TempDir Path tempDir) throws Exception {
        assertEquals("""
                ## Recent publications

                * %s 2025

                """.formatted(title),
                exportRecentSection(tempDir, publication(title)));
    }

    @Test
    void escapesAdditionalMarkdownTitleCharacters(
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication("A <tag> `code` | # heading")
                .withField(StandardField.URL, "https://example.org/paper");

        assertEquals("""
                ## Recent publications

                * [A \\<tag> \\`code\\` \\| \\# heading](<https://example.org/paper>). 2025

                """, exportRecentSection(tempDir, entry));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Research question? ",
            "Research question?  ",
            "Important finding! ",
            "Important finding!  "
    })
    void handlesTrailingSpacesAfterTitlePunctuation(
            String title,
            @TempDir Path tempDir) throws Exception {
        BibEntry entry = publication(title)
                .withField(StandardField.URL, "https://example.org/paper");

        assertEquals("""
                ## Recent publications

                * [%s](<https://example.org/paper>) 2025

                """.formatted(title.stripTrailing()),
                exportRecentSection(tempDir, entry));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Research question? ",
            "Research question?  ",
            "Important finding! ",
            "Important finding!  "
    })
    void handlesTrailingSpacesAfterUnlinkedTitlePunctuation(
            String title,
            @TempDir Path tempDir) throws Exception {
        assertEquals("""
                ## Recent publications

                * %s 2025

                """.formatted(title.stripTrailing()),
                exportRecentSection(tempDir, publication(title)));
    }
}
