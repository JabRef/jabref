package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.IdentifierBasedEntryFetcher;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

import com.google.common.base.Suppliers;

public class PdfMergeDialog {

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
        Supplier<BibEntry> verbatimSupplier = memoize(
                wrapImporterToSupplier(new PdfVerbatimBibtexImporter(preferences.getImportFormatPreferences()), filePath));
        Supplier<BibEntry> embeddedSupplier = memoize(
                wrapImporterToSupplier(new PdfEmbeddedBibFileImporter(preferences.getImportFormatPreferences()), filePath));
        Supplier<BibEntry> xmpSupplier = memoize(
                wrapImporterToSupplier(new PdfXmpImporter(preferences.getXmpPreferences()), filePath));
        Supplier<BibEntry> contentSupplier = memoize(
                wrapImporterToSupplier(new PdfContentImporter(), filePath));

        dialog.addSource(Localization.lang("Verbatim"), verbatimSupplier);
        dialog.addSource(Localization.lang("Embedded"), embeddedSupplier);

        List<Supplier<BibEntry>> parsedEntrySuppliers = new ArrayList<>(List.of(
                verbatimSupplier,
                embeddedSupplier,
                xmpSupplier,
                contentSupplier
        ));

        if (preferences.getGrobidPreferences().isGrobidEnabled()) {
            Supplier<BibEntry> grobidSupplier = memoize(
                    wrapImporterToSupplier(new PdfGrobidImporter(preferences.getImportFormatPreferences()), filePath));
            dialog.addSource("Grobid", grobidSupplier);
            parsedEntrySuppliers.add(grobidSupplier);
        }

        dialog.addSource(Localization.lang("XMP metadata"), xmpSupplier);
        dialog.addSource(Localization.lang("Content"), contentSupplier);

        Supplier<List<BibEntry>> parsedEntriesSupplier = memoize(() -> getAvailableEntries(parsedEntrySuppliers));
        IdentifierBasedEntryFetcher identifierBasedEntryFetcher = new IdentifierBasedEntryFetcher(preferences.getImportFormatPreferences());

        BackgroundTask.wrap(() -> identifierBasedEntryFetcher.fetchByFields(parsedEntriesSupplier.get(), IdentifierBasedEntryFetcher.SUPPORTED_FIELDS))
                      .onSuccess(fetchedEntries -> {
                          for (Field field : IdentifierBasedEntryFetcher.SUPPORTED_FIELDS) {
                              BibEntry fetchedEntry = fetchedEntries.get(field);
                              if (fetchedEntry != null) {
                                  dialog.addSource(Localization.lang("Entry from %0", FieldTextMapper.getDisplayName(field)), fetchedEntry);
                              }
                          }
                      })
                      .executeWith(taskExecutor);
    }

    private static List<BibEntry> getAvailableEntries(List<Supplier<BibEntry>> suppliers) {
        List<BibEntry> entries = new ArrayList<>();
        for (Supplier<BibEntry> supplier : suppliers) {
            BibEntry entry = supplier.get();
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
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

    private static <T> Supplier<T> memoize(Supplier<T> supplier) {
        com.google.common.base.Supplier<T> guavaSupplier = Suppliers.memoize(supplier::get);
        return guavaSupplier::get;
    }
}
