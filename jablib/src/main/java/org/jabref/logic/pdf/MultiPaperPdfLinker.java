package org.jabref.logic.pdf;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.pdf.PdfOutlineSplitter.SplitPapers;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~logic.pdf.split-proceedings-outline~1]
// [impl->req~logic.pdf.extract-paper-page-range~1]
@NullMarked
public class MultiPaperPdfLinker {

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiPaperPdfLinker.class);

    private final BibDatabaseContext databaseContext;
    private final FilePreferences filePreferences;

    public MultiPaperPdfLinker(BibDatabaseContext databaseContext, FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.filePreferences = filePreferences;
    }

    // [impl->req~logic.importer.multi-paper-pdf~1]
    public static List<BibEntry> volumesToSplit(List<BibEntry> importedEntries) {
        Set<String> volumesOfPapersWithoutFile = importedEntries.stream()
                                                                .filter(entry -> entry.getFiles().isEmpty())
                                                                .flatMap(entry -> entry.getField(StandardField.CROSSREF).stream())
                                                                .collect(Collectors.toSet());
        return importedEntries.stream()
                              .filter(entry -> PdfOutlineSplitter.holdsPapers(entry.getType()))
                              .filter(entry -> entry.getCitationKey().filter(volumesOfPapersWithoutFile::contains).isPresent())
                              .toList();
    }

    public Optional<Path> firstPdf(BibEntry entry) {
        return entry.getFiles().stream()
                    .flatMap(file -> file.findIn(databaseContext, filePreferences).stream())
                    .filter(FileUtil::isPDFFile)
                    .findFirst();
    }

    /// @return a link to a copy of the pages that `entry` names, or empty when `pdf` holds no more than these pages
    public Optional<LinkedFile> copyPages(Path pdf, BibEntry entry) throws IOException {
        BibEntry pageOffsetOwner = PdfLocationResolver.pageOffsetOwner(entry, databaseContext.getDatabase());
        return PdfPaperExtractor.extractPaper(pdf, entry, pageOffsetOwner, pdf.getParent())
                                .map(copy -> new LinkedFile("", FileUtil.relativize(copy, databaseContext, filePreferences), StandardFileType.PDF.getName()));
    }

    public boolean isInLibrary(BibEntry entry) {
        return databaseContext.getDatabase().containsEntryWithId(entry.getId());
    }

    public PaperMatches match(BibEntry volume, SplitPapers split) {
        FileUtil.relativize(split.papers(), databaseContext, filePreferences);
        List<BibEntry> papersInLibrary = papersOf(volume);
        List<BibEntry> newPapers = new ArrayList<>();
        List<Match> existingPapers = new ArrayList<>();
        for (BibEntry paper : split.papers()) {
            sameTitle(papersInLibrary, paper).ifPresentOrElse(
                    existing -> existingPapers.add(new Match(existing, paper)),
                    () -> newPapers.add(paper));
        }
        return new PaperMatches(volume, split.pageOffset(), newPapers, existingPapers);
    }

    /// @return the changes to the volume and to the papers already in the library; the new papers are left for the caller to insert
    public List<FieldChange> apply(PaperMatches matches) {
        List<FieldChange> changes = new ArrayList<>();
        for (Match match : matches.existingPapers()) {
            BibEntry existing = match.existing();
            if (!ownsPdf(existing)) {
                match.split().getField(PdfLocationResolver.PAGE_OFFSET)
                     .flatMap(offset -> existing.setField(PdfLocationResolver.PAGE_OFFSET, offset))
                     .ifPresent(changes::add);
            }
            match.split().getFiles().forEach(file -> existing.addFile(file).ifPresent(changes::add));
        }
        BibEntry volume = matches.volume();
        if (databaseContext.getMode() == BibDatabaseMode.BIBTEX) {
            changes.addAll(prepareForBibtex(volume, Stream.concat(matches.existingPapers().stream().map(Match::existing), matches.newPapers().stream()).toList()));
        }
        if (!volume.hasField(PdfLocationResolver.PAGE_OFFSET) && (matches.pageOffset() != 0)) {
            volume.setField(PdfLocationResolver.PAGE_OFFSET, Integer.toString(matches.pageOffset())).ifPresent(changes::add);
        }
        return changes;
    }

    public static void deletePdfs(SplitPapers split) {
        for (BibEntry paper : split.papers()) {
            for (LinkedFile file : paper.getFiles()) {
                Path pdf = Path.of(file.getLink());
                try {
                    Files.deleteIfExists(pdf);
                } catch (IOException exception) {
                    LOGGER.warn("Could not delete the split PDF {}", pdf, exception);
                }
            }
        }
    }

    private static List<FieldChange> prepareForBibtex(BibEntry volume, List<BibEntry> papers) {
        List<FieldChange> changes = new ArrayList<>();
        if (!volume.hasField(StandardField.BOOKTITLE)) {
            volume.getField(StandardField.TITLE).flatMap(title -> volume.setField(StandardField.BOOKTITLE, title)).ifPresent(changes::add);
        }
        papers.stream()
              .filter(paper -> (paper.getType() == StandardEntryType.InBook) && paper.hasField(StandardField.TITLE))
              .forEach(paper -> paper.setType(StandardEntryType.InCollection).ifPresent(changes::add));
        return changes;
    }

    private List<BibEntry> papersOf(BibEntry volume) {
        return volume.getCitationKey()
                     .map(key -> databaseContext.getDatabase().getEntries().stream()
                                                .filter(entry -> entry.getField(StandardField.CROSSREF).filter(key::equals).isPresent())
                                                .toList())
                     .orElse(List.of());
    }

    private boolean ownsPdf(BibEntry entry) {
        return PdfLocationResolver.resolve(entry, databaseContext.getDatabase())
                                  .filter(location -> location.fileOwner() == entry)
                                  .isPresent();
    }

    private static Optional<BibEntry> sameTitle(List<BibEntry> papers, BibEntry paper) {
        return paper.getField(StandardField.TITLE).flatMap(title -> papers.stream()
                                                                          .filter(existing -> existing.getField(StandardField.TITLE).filter(title::equalsIgnoreCase).isPresent())
                                                                          .findFirst());
    }

    /// @param pageOffset the PDF page minus the printed page number that the papers' pages were read with
    public record PaperMatches(BibEntry volume, int pageOffset, List<BibEntry> newPapers, List<Match> existingPapers) {
    }

    /// @param split the entry created from the PDF for a paper that is already in the library as `existing`
    public record Match(BibEntry existing, BibEntry split) {
    }
}
