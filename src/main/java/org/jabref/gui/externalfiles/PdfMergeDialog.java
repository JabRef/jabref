package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;

import org.jabref.gui.mergeentries.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

public class PdfMergeDialog {

    /**
     * Constructs a merge dialog for a PDF file. This dialog calls various {@link PdfImporter}s, collects the results, and lets the user choose between them.
     * <p>
     * {@link PdfImporter}s try to extract a {@link BibEntry} out of a PDF file,
     * but it does not perform this 100% perfectly, it is only a set of heuristics that in some cases might work, in others not.
     * Thus, JabRef provides this merge dialog that collects the results of all {@link PdfImporter}s
     * and gives user a choice between field values.
     *
     * @param entry the entry to merge with
     * @param filePath the path to the PDF file. This PDF is used as the source for the {@link PdfImporter}s.
     * @param preferences the preferences to use. Full preference object is required, because of current implementation of {@link MultiMergeEntriesView}.
     * @param taskExecutor the task executor to use when the multi merge dialog executes the importers.
     */
    public static MultiMergeEntriesView createMergeDialog(BibEntry entry, Path filePath, GuiPreferences preferences, TaskExecutor taskExecutor) {
        MultiMergeEntriesView dialog = new MultiMergeEntriesView(preferences, taskExecutor);

        dialog.setTitle(Localization.lang("Merge PDF metadata"));

        dialog.addSource(Localization.lang("Entry"), entry);
        dialog.addSource(Localization.lang("Verbatim"), wrapImporterToSupplier(new PdfVerbatimBibtexImporter(preferences.getImportFormatPreferences()), filePath));
        dialog.addSource(Localization.lang("Embedded"), wrapImporterToSupplier(new PdfEmbeddedBibFileImporter(preferences.getImportFormatPreferences()), filePath));

        if (preferences.getGrobidPreferences().isGrobidEnabled()) {
            dialog.addSource("Grobid", wrapImporterToSupplier(new PdfGrobidImporter(preferences.getImportFormatPreferences()), filePath));
        }

        dialog.addSource(Localization.lang("XMP metadata"), wrapImporterToSupplier(new PdfXmpImporter(preferences.getXmpPreferences()), filePath));
        dialog.addSource(Localization.lang("Content"), wrapImporterToSupplier(new PdfContentImporter(), filePath));

        return dialog;
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
