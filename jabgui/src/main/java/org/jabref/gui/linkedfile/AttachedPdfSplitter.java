package org.jabref.gui.linkedfile;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
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
        this.linker = new MultiPaperPdfLinker(databaseContext, filePreferences);
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
        BackgroundTask.wrap(() -> linker.copyPages(pdf, entry))
                      .onSuccess(copy -> copy.ifPresent(linkPaper))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    // [impl->req~logic.importer.multi-paper-pdf~1]
    public void splitImportedVolumes(List<BibEntry> importedEntries) {
        MultiPaperPdfLinker.volumesToSplit(importedEntries)
                           .forEach(volume -> linker.firstPdf(volume).ifPresent(pdf -> splitIntoPapers(pdf, volume)));
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
