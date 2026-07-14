package org.jabref.logic.importer.fileformat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.logic.exporter.HayagrivaExporter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Guards the symmetry of [HayagrivaMapping]: everything the Hayagriva writer emits must read
/// back as the identical [BibEntry], and re-exporting an imported file must be stable.
class HayagrivaRoundTripTest {

    @TempDir
    Path tempDir;

    private final HayagrivaExporter exporter = new HayagrivaExporter();
    private final HayagrivaImporter importer = new HayagrivaImporter();

    private List<BibEntry> exportAndReimport(List<BibEntry> entries) throws IOException {
        Path file = tempDir.resolve("roundtrip-" + entries.hashCode() + ".yml");
        Files.deleteIfExists(file);
        Files.createFile(file);
        exporter.export(new BibDatabaseContext(), file, entries);
        return importer.importDatabase(file).getDatabase().getEntries();
    }

    static Stream<Arguments> writerAuthoredEntries() {
        return Stream.of(
                Arguments.of(new BibEntry(StandardEntryType.Article)
                        .withCitationKey("kinetics")
                        .withField(StandardField.TITLE, "Kinetics and luminescence")
                        .withField(StandardField.AUTHOR, "Doan, T. D. and Haug, Hartmut")
                        .withField(StandardField.DATE, "2020-10-14")
                        .withField(StandardField.JOURNAL, "Physical Review B")
                        .withField(StandardField.VOLUME, "102")
                        .withField(StandardField.NUMBER, "16")
                        .withField(StandardField.PUBLISHER, "American Physical Society")
                        .withField(StandardField.PAGES, "165126-165139")
                        .withField(StandardField.PAGETOTAL, "13")
                        .withField(StandardField.DOI, "10.1103/PhysRevB.102.165126")
                        .withField(StandardField.NOTE, "A note on the entry")
                        .withField(StandardField.ABSTRACT, "An abstract text")),
                Arguments.of(new BibEntry(StandardEntryType.InProceedings)
                        .withCitationKey("zygos")
                        .withField(StandardField.TITLE, "Achieving Low Tail Latency")
                        .withField(StandardField.AUTHOR, "Prekas, George and Kogias, Marios")
                        .withField(StandardField.BOOKTITLE, "Proceedings of the 26th Symposium on Operating Systems Principles")
                        .withField(StandardField.PAGES, "325-341")),
                Arguments.of(new BibEntry(StandardEntryType.Book)
                        .withCitationKey("donne")
                        .withField(StandardField.TITLE, "The Anniversaries")
                        .withField(StandardField.EDITOR, "Stringer, Gary A. and Pebworth, Ted-Larry")
                        .withField(StandardField.ADDRESS, "Bloomington")
                        .withField(StandardField.PUBLISHER, "Indiana University Press")
                        .withField(StandardField.EDITION, "2")
                        .withField(StandardField.VOLUMES, "7")
                        .withField(StandardField.SERIES, "The Variorum Edition")
                        .withField(StandardField.ISBN, "978-0747551003")
                        .withField(StandardField.LANGUAGE, "en-US")),
                Arguments.of(new BibEntry(StandardEntryType.InBook)
                        .withCitationKey("lambchapter")
                        .withField(StandardField.TITLE, "A chapter")
                        .withField(StandardField.BOOKTITLE, "The life of a friendly lamb")
                        .withField(StandardField.CHAPTER, "3")
                        .withField(StandardField.PAGES, "10-12")),
                Arguments.of(new BibEntry(StandardEntryType.Misc)
                        .withCitationKey("terminator")
                        .withField(StandardField.TITLE, "Terminator 2")
                        .withField(StandardField.PUBLISHER, "Carolco Pictures")
                        .withField(StandardField.TRANSLATOR, "Translator, Some")
                        .withField(new UnknownField("director"), "Cameron, James")
                        .withField(new UnknownField("cast-member"), "Schwarzenegger, Arnold and Hamilton, Linda")
                        .withField(new UnknownField("runtime"), "137:00")
                        .withField(new UnknownField("time-range"), "17:05-17:48")),
                Arguments.of(new BibEntry(StandardEntryType.Online)
                        .withCitationKey("mattermost")
                        .withField(StandardField.TITLE, "Mattermost Privacy Policy")
                        .withField(StandardField.URL, "https://mattermost.com/privacy-policy/")),
                Arguments.of(new BibEntry(StandardEntryType.Report)
                        .withCitationKey("unhdr")
                        .withField(StandardField.TITLE, "Human Development Report 2019")
                        .withField(StandardField.INSTITUTION, "United Nations Development Programme")
                        .withField(StandardField.ISSN, "2412-3129")),
                Arguments.of(new BibEntry(StandardEntryType.Article)
                        .withCitationKey("habitable")
                        .withField(StandardField.TITLE, "Defining the Really Habitable Zone")
                        .withField(StandardField.EPRINT, "2003.13722")
                        .withField(StandardField.EPRINTTYPE, "arxiv")
                        .withField(StandardField.URL, "https://arxiv.org/abs/2003.13722")));
    }

    @ParameterizedTest
    @MethodSource("writerAuthoredEntries")
    void writerAuthoredEntryRoundTripsExactly(BibEntry entry) throws IOException {
        assertEquals(List.of(entry), exportAndReimport(List.of(entry)));
    }

    /// The upstream `basic.yml` fixture uses constructs JabRef normalizes on first import (e.g.
    /// corporate author names, `serial` numbers becoming `issue`), so the guarantee starts after
    /// one import/export cycle: from then on, further cycles must be the identity.
    @Test
    void basicFixtureIsStableAfterFirstCycle() throws IOException, URISyntaxException {
        Path fixture = Path.of(HayagrivaRoundTripTest.class.getResource("basic.yml").toURI());
        List<BibEntry> firstGeneration = importer.importDatabase(fixture).getDatabase().getEntries();

        List<BibEntry> secondGeneration = exportAndReimport(firstGeneration);
        List<BibEntry> thirdGeneration = exportAndReimport(secondGeneration);

        assertEquals(secondGeneration, thirdGeneration);
    }
}
