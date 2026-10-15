package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jabref.gui.DialogService;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.pdf.PdfOutlineSplitter;
import org.jabref.logic.pdf.PdfPaperExtractor;
import org.jabref.logic.undo.UndoManager;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.undo.UndoableInsertEntries;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Splits a PDF that was just attached to an entry. The original file stays where it is.
///
/// A proceedings entry gets one new entry per paper. Any other entry gets a copy of just its own pages.
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
        if (PdfOutlineSplitter.isProceedings(entry.getType())) {
            BackgroundTask<List<BibEntry>> task = BackgroundTask.wrap(() -> PdfOutlineSplitter.split(pdf, entry, pdf.getParent()));
            task.titleProperty().set(Localization.lang("Split PDF"));
            task.showToUser(true);
            task.onSuccess(this::addPapers)
                .onFailure(exception -> showFailure(pdf, exception))
                .executeWith(taskExecutor);
            return;
        }
        BackgroundTask.wrap(() -> copyPaper(pdf, entry))
                      .onSuccess(paper -> paper.ifPresent(linkPaper))
                      .onFailure(exception -> showFailure(pdf, exception))
                      .executeWith(taskExecutor);
    }

    private Optional<LinkedFile> copyPaper(Path pdf, BibEntry entry) throws IOException {
        return PdfPaperExtractor.extractPaper(pdf, entry, pdf.getParent())
                                .map(extracted -> new LinkedFile(
                                        "",
                                        FileUtil.relativize(extracted, databaseContext, filePreferences),
                                        StandardFileType.PDF.getName()));
    }

    private void addPapers(List<BibEntry> papers) {
        if (papers.isEmpty()) {
            return;
        }
        FileUtil.relativize(papers, databaseContext, filePreferences);
        undoManager.applyEdit(new UndoableInsertEntries(databaseContext.getDatabase(), papers));
        dialogService.notify(Localization.lang("Created %0 entries from the PDF.", String.valueOf(papers.size())));
    }

    private void showFailure(Path pdf, Exception exception) {
        LOGGER.error("Could not split PDF {}", pdf, exception);
        dialogService.showErrorDialogAndWait(Localization.lang("Could not split the PDF."), exception);
    }
}
