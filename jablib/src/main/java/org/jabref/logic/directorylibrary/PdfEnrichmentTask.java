package org.jabref.logic.directorylibrary;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NullMarked;

/// Extracts metadata for the sidecar-less PDFs of a directory library after the tab is already
/// visible: each pending stub entry is enriched in place as its PDF is processed, so the table
/// fills progressively and opening the folder never blocks on PDF parsing or identifier
/// lookups. Cancelling keeps the stubs created by the scan.
///
/// Extraction runs on the background thread; entry mutations (and progress updates, which drive
/// JavaFX properties) go through the injected marshaller.
// [impl->req~directory-library.scan~4]
@NullMarked
public class PdfEnrichmentTask extends BackgroundTask<Void> {

    private final List<DirectoryLibraryScanner.PendingPdfImport> pendingImports;
    private final PdfEntryFactory pdfEntryFactory;
    private final BibDatabaseContext databaseContext;
    private final Consumer<Runnable> modelUpdateMarshaller;

    public PdfEnrichmentTask(List<DirectoryLibraryScanner.PendingPdfImport> pendingImports,
                             PdfEntryFactory pdfEntryFactory,
                             BibDatabaseContext databaseContext,
                             Consumer<Runnable> modelUpdateMarshaller) {
        this.pendingImports = pendingImports;
        this.pdfEntryFactory = pdfEntryFactory;
        this.databaseContext = databaseContext;
        this.modelUpdateMarshaller = modelUpdateMarshaller;
        setTitle(Localization.lang("Extracting metadata from %0 PDF file(s)", Integer.toString(pendingImports.size())));
        showToUser(true);
    }

    @Override
    public Void call() {
        int counter = 0;
        for (DirectoryLibraryScanner.PendingPdfImport pending : pendingImports) {
            if (isCancelled()) {
                break;
            }
            counter++;
            final int progress = counter;
            modelUpdateMarshaller.accept(() -> {
                updateMessage(pending.pdfFile().getFileName().toString());
                updateProgress(progress, pendingImports.size());
            });

            Optional<BibEntry> extracted = pdfEntryFactory.extractMetadata(pending.pdfFile(), databaseContext);
            modelUpdateMarshaller.accept(() -> {
                extracted.ifPresent(metadata -> pdfEntryFactory.applyExtractedMetadata(metadata, pending.entry()));
                pdfEntryFactory.generateCitationKeyIfMissing(pending.entry(), databaseContext);
            });
        }
        return null;
    }
}
