package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.pdf.PdfContentImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfEmbeddedBibFileImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfGrobidImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfVerbatimBibtexImporter;
import org.jabref.logic.importer.fileformat.pdf.PdfXmpImporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldTextMapper;

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
        finishDialog(dialog, filePath, preferences);
        return dialog;
    }

    public static MultiMergeEntriesView createMergeDialog(Path filePath, GuiPreferences preferences, TaskExecutor taskExecutor) {
        MultiMergeEntriesView dialog = initDialog(preferences, taskExecutor);
        finishDialog(dialog, filePath, preferences);
        return dialog;
    }

    private static MultiMergeEntriesView initDialog(GuiPreferences preferences, TaskExecutor taskExecutor) {
        MultiMergeEntriesView dialog = new MultiMergeEntriesView(preferences, taskExecutor);
        dialog.setTitle(Localization.lang("Merge PDF metadata"));
        return dialog;
    }

    private static void finishDialog(MultiMergeEntriesView dialog, Path filePath, GuiPreferences preferences) {
        dialog.addSource(Localization.lang("Verbatim"), wrapImporterToSupplier(new PdfVerbatimBibtexImporter(preferences.getImportFormatPreferences()), filePath));
        dialog.addSource(Localization.lang("Embedded"), wrapImporterToSupplier(new PdfEmbeddedBibFileImporter(preferences.getImportFormatPreferences()), filePath));
        if (preferences.getGrobidPreferences().isGrobidEnabled()) {
            dialog.addSource("Grobid", wrapImporterToSupplier(new PdfGrobidImporter(preferences.getImportFormatPreferences()), filePath));
        }
        dialog.addSource(Localization.lang("XMP metadata"), wrapImporterToSupplier(new PdfXmpImporter(preferences.getXmpPreferences()), filePath));
        dialog.addSource(Localization.lang("Content"), wrapImporterToSupplier(new PdfContentImporter(), filePath));

        @SuppressWarnings("unchecked")
        Map<Field, String>[] identifierCache = new Map[1];

        for (Field identifierField : FetchAndMergeEntry.SUPPORTED_FIELDS) {
            dialog.addSource(
                Localization.lang("From %0", FieldTextMapper.getDisplayName(identifierField)),
                () -> {
                    synchronized (identifierCache) {
                        if (identifierCache[0] == null) {
                            identifierCache[0] = extractIdentifiers(
                                    parsePdfForEntries(filePath, preferences));
                        }
                    }
                    String value = identifierCache[0].get(identifierField);
                    if (value == null) {
                        return null;
                    }
                    return fetchEntryFromWeb(identifierField, value, preferences.getImportFormatPreferences());
                }
            );
        }
    }

    private static List<BibEntry> parsePdfForEntries(Path filePath, GuiPreferences preferences) {
        List<BibEntry> entries = new ArrayList<>();
        List<Importer> importers = List.of(
                new PdfXmpImporter(preferences.getXmpPreferences()),
                new PdfContentImporter()
        );
        for (Importer importer : importers) {
            try {
                ParserResult result = importer.importDatabase(filePath);
                if (!result.isInvalid() && !result.isEmpty() && result.getDatabase().hasEntries()) {
                    entries.add(result.getDatabase().getEntries().getFirst());
                }
            } catch (IOException e) {
                LOGGER.warn("Could not parse PDF for identifier lookup", e);
            }
        }
        return entries;
    }

    static Map<Field, String> extractIdentifiers(List<BibEntry> entries) {
        Map<Field, String> identifiers = new HashMap<>();
        for (BibEntry entry : entries) {
            for (Field field : FetchAndMergeEntry.SUPPORTED_FIELDS) {
                if (!identifiers.containsKey(field)) {
                    entry.getField(field).ifPresent(value -> identifiers.put(field, value));
                }
            }
        }
        return identifiers;
    }

    private static BibEntry fetchEntryFromWeb(Field field, String identifier, ImportFormatPreferences importFormatPreferences) {
        return WebFetchers.getIdBasedFetcherForField(field, importFormatPreferences)
                         .flatMap(fetcher -> {
                             try {
                                 return fetcher.performSearchById(identifier);
                             } catch (FetcherException e) {
                                 LOGGER.warn("Failed to fetch entry by {} {}", field, identifier, e);
                                 return Optional.empty();
                             }
                         })
                         .orElse(null);
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
