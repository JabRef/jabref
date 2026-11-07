package org.jabref.logic.pdf;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.pdf.MultiPaperPdfLinker.PaperMatches;
import org.jabref.logic.pdf.PdfOutlineSplitter.SplitPapers;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@NullMarked
class MultiPaperPdfLinkerTest {

    // [utest->req~logic.importer.multi-paper-pdf~1]
    @Test
    void splitsOnlyVolumesWhosePapersHaveNoFile() {
        BibEntry volume = new BibEntry(StandardEntryType.Proceedings).withCitationKey("volume");
        BibEntry paper = new BibEntry(StandardEntryType.InProceedings).withField(StandardField.CROSSREF, "volume");
        BibEntry linkedVolume = new BibEntry(StandardEntryType.Collection).withCitationKey("linked");
        BibEntry linkedPaper = new BibEntry(StandardEntryType.InCollection)
                .withField(StandardField.CROSSREF, "linked")
                .withFiles(List.of(new LinkedFile("", Path.of("alpha.pdf"), "PDF")));
        BibEntry article = new BibEntry(StandardEntryType.Article).withCitationKey("article");

        assertEquals(List.of(volume), MultiPaperPdfLinker.volumesToSplit(List.of(paper, volume, linkedPaper, linkedVolume, article)));
    }

    // [utest->req~logic.pdf.split-proceedings-outline~1]
    @Test
    void preparesABookAndItsPapersForBibtex() {
        BibEntry book = new BibEntry(StandardEntryType.Book).withCitationKey("volume").withField(StandardField.TITLE, "An Example Monograph");
        BibEntry existing = new BibEntry(StandardEntryType.InBook)
                .withField(StandardField.TITLE, "Alpha")
                .withField(StandardField.CROSSREF, "volume");
        BibEntry newPaper = new BibEntry(StandardEntryType.InBook)
                .withField(StandardField.TITLE, "Beta")
                .withField(StandardField.CROSSREF, "volume");
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(List.of(book, existing)));
        databaseContext.setMode(BibDatabaseMode.BIBTEX);
        MultiPaperPdfLinker linker = new MultiPaperPdfLinker(databaseContext, mock(FilePreferences.class));
        BibEntry splitAlpha = new BibEntry(StandardEntryType.InBook).withField(StandardField.TITLE, "Alpha");

        PaperMatches matches = linker.match(book, new SplitPapers(List.of(splitAlpha, newPaper), 0));
        linker.apply(matches);

        assertEquals(List.of(newPaper), matches.newPapers());
        assertEquals(List.of(StandardEntryType.InCollection, StandardEntryType.InCollection), List.of(existing.getType(), newPaper.getType()));
        assertEquals(Optional.of("An Example Monograph"), book.getField(StandardField.BOOKTITLE));
    }
}
