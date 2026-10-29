package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.PdfLocationResolver;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.PdfOutlineSplitter;
import org.jabref.logic.pdf.PdfOutlineSplitter.SplitPapers;
import org.jabref.logic.pdf.PdfOutlineSplitter.Volume;
import org.jabref.logic.pdf.PdfPaperExtractor;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.undo.UndoableFieldChange;
import org.jabref.model.undo.UndoableInsertEntries;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// [impl->req~logic.pdf.split-proceedings-outline~1]
// [impl->req~logic.pdf.extract-paper-page-range~1]
@NullMarked
public class AttachedPdfSplitter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttachedPdfSplitter.class);

    private final BibDatabaseContext databaseContext;
    private final DialogService dialogService;
    private final FilePreferences filePreferences;
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;

    public AttachedPdfSplitter(BibDatabaseContext databaseContext,
                               DialogService dialogService,
                               FilePreferences filePreferences,
                               TaskExecutor taskExecutor,
                               UndoManager undoManager) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.filePreferences = filePreferences;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;
    }

    /// @param linkPaper links the copy of the entry's own pages to the entry
    public void split(Path pdf, BibEntry entry, Consumer<LinkedFile> linkPaper) {
        if (!FileUtil.isPDFFile(pdf)) {
            return;
        }
        if (PdfOutlineSplitter.holdsPapers(entry.getType())) {
            splitIntoPapers(pdf, entry);
            return;
        }
        BibEntry pageOffsetOwner = PdfLocationResolver.pageOffsetOwner(entry, databaseContext.getDatabase());
        BackgroundTask.wrap(() -> copyPaper(pdf, entry, pageOffsetOwner))
                      .onSuccess(paper -> paper.ifPresent(copy -> {
                          linkPaper.accept(copy);
                          takeOverPageOffset(pdf, entry, pageOffsetOwner);
                      }))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    // [impl->req~logic.importer.multi-paper-pdf~1]
    public void splitImportedVolumes(List<BibEntry> importedEntries) {
        Set<String> volumesOfPapersWithoutFile = importedEntries.stream()
                                                                .filter(entry -> entry.getFiles().isEmpty())
                                                                .flatMap(entry -> entry.getField(StandardField.CROSSREF).stream())
                                                                .collect(Collectors.toSet());
        importedEntries.stream()
                       .filter(entry -> PdfOutlineSplitter.holdsPapers(entry.getType()))
                       .filter(entry -> entry.getCitationKey().filter(volumesOfPapersWithoutFile::contains).isPresent())
                       .forEach(this::splitVolume);
    }

    private void splitVolume(BibEntry volume) {
        volume.getFiles().stream()
              .flatMap(file -> file.findIn(databaseContext, filePreferences).stream())
              .filter(FileUtil::isPDFFile)
              .findFirst()
              .ifPresent(pdf -> splitIntoPapers(pdf, volume));
    }

    private void splitIntoPapers(Path pdf, BibEntry volume) {
        BackgroundTask.wrap(() -> PdfOutlineSplitter.readVolume(pdf))
                      .onSuccess(papers -> papers.ifPresent(found -> splitIntoPapers(pdf, volume, found)))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    private void splitIntoPapers(Path pdf, BibEntry volume, Volume papers) {
        BackgroundTask<SplitPapers> task = BackgroundTask.wrap(() -> PdfOutlineSplitter.split(pdf, volume, papers, pdf.getParent()));
        task.titleProperty().set(Localization.lang("Split PDF"));
        task.showToUser(true);
        task.onSuccess(split -> addPapers(volume, split))
            .onFailure(exception -> showFailure(pdf, exception))
            .executeWith(taskExecutor);
    }

    private Optional<LinkedFile> copyPaper(Path pdf, BibEntry entry, BibEntry pageOffsetOwner) throws IOException {
        return PdfPaperExtractor.extractPaper(pdf, entry, pageOffsetOwner, pdf.getParent())
                                .map(extracted -> new LinkedFile(
                                        "",
                                        FileUtil.relativize(extracted, databaseContext, filePreferences),
                                        StandardFileType.PDF.getName()));
    }

    private void takeOverPageOffset(Path pdf, BibEntry entry, BibEntry pageOffsetOwner) {
        int offset = PdfLocationResolver.pageOffset(pageOffsetOwner);
        boolean viewerOpensFile = PdfLocationResolver.resolve(entry, databaseContext.getDatabase())
                                                     .flatMap(location -> location.file().findIn(databaseContext, filePreferences))
                                                     .filter(viewed -> viewed.toAbsolutePath().normalize().equals(pdf.toAbsolutePath().normalize()))
                                                     .isPresent();
        if ((pageOffsetOwner != entry) && (offset != 0) && viewerOpensFile) {
            entry.setField(PdfLocationResolver.PAGE_OFFSET, Integer.toString(offset))
                 .ifPresent(change -> undoManager.addEdit(new UndoableFieldChange(change)));
        }
    }

    private void addPapers(BibEntry volume, SplitPapers split) {
        if (split.papers().isEmpty()) {
            return;
        }
        FileUtil.relativize(split.papers(), databaseContext, filePreferences);
        List<BibEntry> papersInLibrary = papersOf(volume);
        List<BibEntry> newPapers = new ArrayList<>();
        List<BibEntry> linkedPapers = new ArrayList<>();
        undoManager.addEdit(Localization.lang("Split PDF"), edit -> {
            for (BibEntry paper : split.papers()) {
                sameTitle(papersInLibrary, paper).ifPresentOrElse(
                        existing -> {
                            linkedPapers.add(existing);
                            if (!ownsPdf(existing)) {
                                edit.addEdit(paper.getField(PdfLocationResolver.PAGE_OFFSET)
                                                  .flatMap(offset -> existing.setField(PdfLocationResolver.PAGE_OFFSET, offset)));
                            }
                            paper.getFiles().forEach(file -> edit.addEdit(existing.addFile(file)));
                        },
                        () -> newPapers.add(paper));
            }
            if (databaseContext.getMode() == BibDatabaseMode.BIBTEX) {
                edit.addAll(prepareForBibtex(volume, Stream.concat(linkedPapers.stream(), newPapers.stream()).toList()));
            }
            if (!newPapers.isEmpty()) {
                edit.applyEdit(new UndoableInsertEntries(databaseContext.getDatabase(), newPapers));
            }
            if (!volume.hasField(PdfLocationResolver.PAGE_OFFSET) && (split.pageOffset() != 0)) {
                edit.addEdit(volume.setField(PdfLocationResolver.PAGE_OFFSET, Integer.toString(split.pageOffset())));
            }
        });
        if (!newPapers.isEmpty()) {
            dialogService.notify(Localization.lang("Created %0 entries from the PDF.", String.valueOf(newPapers.size())));
        }
        if (!linkedPapers.isEmpty()) {
            dialogService.notify(Localization.lang("Added split PDFs to %0 entries that were already in the library.", String.valueOf(linkedPapers.size())));
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

    private void showFailure(Path pdf, Exception exception) {
        LOGGER.error("Could not split PDF {}", pdf, exception);
        dialogService.showErrorDialogAndWait(Localization.lang("Could not split the PDF."), exception);
    }
}
