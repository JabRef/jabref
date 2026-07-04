package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;

import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfMergeMetadataImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfMergeDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMergeDialog.class);

    /// Constructs a merge dialog for a PDF file. This dialog calls various {@link org.jabref.logic.importer.fileformat.pdf.PdfImporter}s, collects the results, and lets the user choose between them.
    ///
    /// {@link org.jabref.logic.importer.fileformat.pdf.PdfImporter}s try to extract a {@link BibEntry} out of a PDF file,
    /// but it does not perform this 100% perfectly, it is only a set of heuristics that in some cases might work, in others not.
    /// Thus, JabRef provides this merge dialog that collects the results of all {@link org.jabref.logic.importer.fileformat.pdf.PdfImporter}s
    /// and gives user a choice between field values.
    ///
    /// @param entry        the entry to merge with
    /// @param filePath     the path to the PDF file. This PDF is used as the source for the {@link org.jabref.logic.importer.fileformat.pdf.PdfImporter}s.
    /// @param preferences  the preferences to use. Full preference object is required, because of current implementation of {@link MultiMergeEntriesView}.
    /// @param taskExecutor the task executor to use when the multi merge dialog executes the importers.
    public static MultiMergeEntriesView createMergeDialog(BibEntry entry, Path filePath, GuiPreferences preferences, TaskExecutor taskExecutor) {
        MultiMergeEntriesView dialog = initDialog(preferences, taskExecutor);
        dialog.addSource(Localization.lang("Entry"), entry);
        finishDialog(dialog, filePath, preferences, taskExecutor);
        return dialog;
    }

    public static MultiMergeEntriesView createMergeDialog(Path filePath, GuiPreferences preferences, TaskExecutor taskExecutor) {
        MultiMergeEntriesView dialog = initDialog(preferences, taskExecutor);
        finishDialog(dialog, filePath, preferences, taskExecutor);
        return dialog;
    }

    private static MultiMergeEntriesView initDialog(GuiPreferences preferences, TaskExecutor taskExecutor) {
        MultiMergeEntriesView dialog = new MultiMergeEntriesView(preferences, taskExecutor);
        dialog.setTitle(Localization.lang("Merge PDF metadata"));
        return dialog;
    }

    private static void finishDialog(MultiMergeEntriesView dialog, Path filePath, GuiPreferences preferences, TaskExecutor taskExecutor) {
        dialog.addSource(Localization.lang("Verbatim"), wrapImporterToSupplier(new PdfVerbatimBibtexImporter(preferences.getImportFormatPreferences()), filePath));
        dialog.addSource(Localization.lang("Embedded"), wrapImporterToSupplier(new PdfEmbeddedBibFileImporter(preferences.getImportFormatPreferences()), filePath));
        if (preferences.getGrobidPreferences().isGrobidEnabled()) {
            dialog.addSource("Grobid", wrapImporterToSupplier(new PdfGrobidImporter(preferences.getImportFormatPreferences()), filePath));
        }
        dialog.addSource(Localization.lang("XMP metadata"), wrapImporterToSupplier(new PdfXmpImporter(preferences.getXmpPreferences()), filePath));
        dialog.addSource(Localization.lang("Content"), wrapImporterToSupplier(new PdfContentImporter(), filePath));
        addIdentifierSources(dialog, filePath, preferences, taskExecutor);
    }

    /// Adds a column for each identifier (DOI, arXiv, ISBN) that is actually contained in the PDF, showing the
    /// bibliographic metadata fetched from that identifier. See [issue #15415](https://github.com/JabRef/jabref/issues/15415).
    ///
    /// The identifiers are extracted asynchronously (the extraction runs the PDF importers, which may access the
    /// network via Grobid), and the resulting columns are added on the JavaFX thread once extraction completes.
    /// Columns are only added for identifiers that are present, so a PDF without an ISBN does not show an empty
    /// "From ISBN" column. The actual fetch for each identifier happens lazily inside the added source.
    private static void addIdentifierSources(MultiMergeEntriesView dialog, Path filePath, GuiPreferences preferences, TaskExecutor taskExecutor) {
        PdfMergeMetadataImporter importer = new PdfMergeMetadataImporter(preferences.getImportFormatPreferences());
        BackgroundTask.wrap(() -> importer.extractCandidates(filePath))
                      .onSuccess(candidates -> {
                          addIdentifierSource(dialog, importer, candidates, StandardField.DOI, Localization.lang("From DOI"));
                          addIdentifierSource(dialog, importer, candidates, StandardField.EPRINT, Localization.lang("From arXiv"));
                          addIdentifierSource(dialog, importer, candidates, StandardField.ISBN, Localization.lang("From ISBN"));
                      })
                      .onFailure(exception -> LOGGER.warn("Could not extract identifiers from PDF {}", filePath, exception))
                      .executeWith(taskExecutor);
    }

    private static void addIdentifierSource(MultiMergeEntriesView dialog, PdfMergeMetadataImporter importer, List<BibEntry> candidates, StandardField field, String title) {
        boolean identifierPresent = candidates.stream().anyMatch(candidate -> candidate.hasField(field));
        if (!identifierPresent) {
            return;
        }
        dialog.addSource(title, () -> {
            try {
                return importer.fetchByIdentifier(field, candidates).orElse(null);
            } catch (FetcherException exception) {
                LOGGER.warn("Failed to fetch metadata for {} contained in the PDF", field, exception);
                return null;
            }
        });
    }

    private static Supplier<BibEntry> wrapImporterToSupplier(Importer importer, Path filePath) {
        return () -> {
            try {
                ParserResult parserResult = importer.importDatabase(filePath);
                if (parserResult.isInvalid() || parserResult.isEmpty() || !parserResult.getDatabase().hasEntries()) {
                    return null;
                }
                return parserResult.getDatabase().getEntries().getFirst();
            } catch (IOException e) {
                return null;
            }
        };
    }
}
