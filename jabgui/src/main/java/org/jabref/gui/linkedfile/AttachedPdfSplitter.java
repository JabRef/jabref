package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.PdfLocationResolver.PageSpan;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.MultiPaperPdfLinker;
import org.jabref.logic.pdf.MultiPaperPdfLinker.PaperMatches;
import org.jabref.logic.pdf.PdfOutlineSplitter;
import org.jabref.logic.pdf.PdfOutlineSplitter.SplitPapers;
import org.jabref.logic.pdf.PdfOutlineSplitter.Volume;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
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
    private final TaskExecutor taskExecutor;
    private final UndoManager undoManager;
    private final FilePreferences filePreferences;
    private final MultiPaperPdfLinker linker;

    public AttachedPdfSplitter(BibDatabaseContext databaseContext,
                               DialogService dialogService,
                               FilePreferences filePreferences,
                               TaskExecutor taskExecutor,
                               UndoManager undoManager) {
        this.databaseContext = databaseContext;
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.undoManager = undoManager;
        this.filePreferences = filePreferences;
        this.linker = new MultiPaperPdfLinker(databaseContext, filePreferences);
    }

    /// @param linkPaper links the copy of the entry's own pages to the entry
    public void split(Path pdf, BibEntry entry, Consumer<LinkedFile> linkPaper) {
        if (!FileUtil.isPDFFile(pdf)) {
            return;
        }
        if (PdfOutlineSplitter.holdsPapers(entry.getType())) {
            splitIntoPapers(pdf, entry, false);
            return;
        }
        BackgroundTask.wrap(() -> linker.pagesToCopy(pdf, entry))
                      .onSuccess(pages -> pages.filter(span -> confirmCopy(pdf, span)).ifPresent(span -> copyPages(pdf, span, linkPaper)))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    private void copyPages(Path pdf, PageSpan pages, Consumer<LinkedFile> linkPaper) {
        BackgroundTask.wrap(() -> linker.copyPages(pdf, pages))
                      .onSuccess(linkPaper)
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    private boolean confirmCopy(Path pdf, PageSpan pages) {
        return dialogService.showConfirmationDialogAndWait(
                Localization.lang("Copy entry pages"),
                Localization.lang("Copy pages %0 to %1 of %2 into a new PDF for this entry? The original PDF is kept.",
                        String.valueOf(pages.firstPage()), String.valueOf(pages.lastPage()), pdf.getFileName().toString()),
                Localization.lang("Copy pages"),
                Localization.lang("Cancel"));
    }

    /// @return whether the user wants `pdf` imported as one entry per paper
    // [impl->req~logic.importer.multi-paper-pdf~1]
    public static boolean confirmImportAsPapers(DialogService dialogService, Path pdf, int paperCount) {
        return dialogService.showConfirmationDialogAndWait(
                Localization.lang("Split PDF"),
                splitQuestion(pdf, paperCount),
                Localization.lang("Split PDF"),
                Localization.lang("Import as one entry"));
    }

    // [impl->req~logic.importer.multi-paper-pdf~1]
    public void splitImportedVolumes(List<BibEntry> importedEntries) {
        splitImportedVolumes(importedEntries, _ -> false);
    }

    /// @param confirmed whether the user already agreed to split the PDF of a volume
    // [impl->req~logic.importer.multi-paper-pdf~1]
    public void splitImportedVolumes(List<BibEntry> importedEntries, Predicate<BibEntry> confirmed) {
        MultiPaperPdfLinker.volumesToSplit(importedEntries)
                           .forEach(volume -> linker.firstPdf(volume).ifPresent(pdf -> splitIntoPapers(pdf, volume, confirmed.test(volume))));
    }

    /// @return whether `volume` links one of `pdfs`
    public boolean linksAnyOf(BibEntry volume, Set<Path> pdfs) {
        return volume.getFiles().stream()
                     .flatMap(file -> file.findIn(databaseContext, filePreferences).stream())
                     .anyMatch(pdf -> pdfs.contains(pdf.toAbsolutePath().normalize()));
    }

    public void splitOnRequest(Path pdf, BibEntry volume) {
        BackgroundTask.wrap(() -> PdfOutlineSplitter.readVolume(pdf))
                      .onSuccess(papers -> papers.ifPresentOrElse(
                              found -> splitOnRequest(pdf, volume, linker.withoutPapersThatHavePdfs(volume, found)),
                              () -> dialogService.notify(Localization.lang("No separate papers were found in %0.", pdf.getFileName().toString()))))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    private void splitOnRequest(Path pdf, BibEntry volume, Volume papers) {
        if (papers.papers().isEmpty()) {
            dialogService.notify(Localization.lang("All papers in %0 already have a PDF.", pdf.getFileName().toString()));
            return;
        }
        if (confirmSplit(pdf, papers)) {
            splitIntoPapers(pdf, volume, papers);
        }
    }

    private void splitIntoPapers(Path pdf, BibEntry volume, boolean confirmed) {
        BackgroundTask.wrap(() -> PdfOutlineSplitter.readVolume(pdf))
                      .onSuccess(papers -> papers.map(found -> linker.withoutPapersThatHavePdfs(volume, found))
                                                 .filter(found -> !found.papers().isEmpty())
                                                 .filter(found -> confirmed || confirmSplit(pdf, found))
                                                 .ifPresent(found -> splitIntoPapers(pdf, volume, found)))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    private boolean confirmSplit(Path pdf, Volume papers) {
        return dialogService.showConfirmationDialogAndWait(
                Localization.lang("Split PDF"),
                splitQuestion(pdf, papers.papers().size()),
                Localization.lang("Split PDF"),
                Localization.lang("Cancel"));
    }

    private static String splitQuestion(Path pdf, int paperCount) {
        return Localization.lang("%0 contains %1 papers. Split it into one PDF per paper? Each PDF is linked to its paper's entry, and missing entries are created. The original PDF is kept.",
                pdf.getFileName().toString(), String.valueOf(paperCount));
    }

    private void splitIntoPapers(Path pdf, BibEntry volume, Volume papers) {
        BackgroundTask<SplitPapers> task = BackgroundTask.wrap(() -> PdfOutlineSplitter.split(pdf, volume, papers, pdf.getParent()));
        task.titleProperty().set(Localization.lang("Split PDF"));
        task.showToUser(true);
        task.onSuccess(split -> addPapers(volume, split))
            .onFailure(exception -> showFailure(pdf, exception))
            .executeWith(taskExecutor);
    }

    private void addPapers(BibEntry volume, SplitPapers split) {
        if (split.papers().isEmpty()) {
            return;
        }
        if (!linker.isInLibrary(volume)) {
            MultiPaperPdfLinker.deletePdfs(split);
            return;
        }
        PaperMatches matches = linker.match(volume, split);
        undoManager.addEdit(Localization.lang("Split PDF"), edit -> {
            edit.addAll(linker.apply(matches));
            if (!matches.newPapers().isEmpty()) {
                edit.applyEdit(new UndoableInsertEntries(databaseContext.getDatabase(), matches.newPapers()));
            }
        });
        if (!matches.newPapers().isEmpty()) {
            dialogService.notify(Localization.lang("Created %0 entries from the PDF.", String.valueOf(matches.newPapers().size())));
        }
        if (!matches.existingPapers().isEmpty()) {
            dialogService.notify(Localization.lang("Added split PDFs to %0 entries that were already in the library.", String.valueOf(matches.existingPapers().size())));
        }
    }

    private void showFailure(Path pdf, Exception exception) {
        LOGGER.error("Could not split PDF {}", pdf, exception);
        dialogService.showErrorDialogAndWait(Localization.lang("Could not split the PDF."), exception);
    }
}
