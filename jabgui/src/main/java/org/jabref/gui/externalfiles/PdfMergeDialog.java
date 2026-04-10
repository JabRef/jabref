package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.jabref.gui.mergeentries.multiwaymerge.MultiMergeEntriesView;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.Importer;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.PdfIdentifierExtractor;
import org.jabref.logic.importer.WebFetchers;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfMergeDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(PdfMergeDialog.class);

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

        Supplier<BibEntry> xmpSupplier = memoize(wrapImporterToSupplier(new PdfXmpImporter(preferences.getXmpPreferences()), filePath));
        Supplier<BibEntry> contentSupplier = memoize(wrapImporterToSupplier(new PdfContentImporter(), filePath));

        dialog.addSource(Localization.lang("XMP metadata"), xmpSupplier);
        dialog.addSource(Localization.lang("Content"), contentSupplier);

        PdfIdentifierExtractor extractor = new PdfIdentifierExtractor(preferences.getXmpPreferences());
        ImportFormatPreferences importFormatPreferences = preferences.getImportFormatPreferences();

        BackgroundTask.wrap(() -> {
                          List<BibEntry> parsedEntries = new ArrayList<>();
                          Optional.ofNullable(xmpSupplier.get()).ifPresent(parsedEntries::add);
                          Optional.ofNullable(contentSupplier.get()).ifPresent(parsedEntries::add);

                          Map<Field, String> identifiers = extractor.extract(parsedEntries);

                          Map<Field, BibEntry> fetched = new LinkedHashMap<>();
                          for (Field field : PdfIdentifierExtractor.SUPPORTED_FIELDS) {
                              String value = identifiers.get(field);
                              if (value != null) {
                                  BibEntry entry = fetchEntryFromWeb(field, value, importFormatPreferences);
                                  if (entry != null) {
                                      fetched.put(field, entry);
                                  }
                              }
                          }
                          return fetched;
                      })
                      .onSuccess(fetched -> fetched.forEach((field, entry) -> dialog.addSource(
                              Localization.lang("From %0", FieldTextMapper.getDisplayName(field)),
                              entry)))
                      .onFailure(e -> LOGGER.warn("Identifier-based fetch failed", e))
                      .executeWith(taskExecutor);
    }

    private static Supplier<BibEntry> memoize(Supplier<BibEntry> delegate) {
        AtomicReference<Optional<BibEntry>> cache = new AtomicReference<>();
        return () -> {
            Optional<BibEntry> result = cache.get();
            if (result != null) {
                return result.orElse(null);
            }
            synchronized (cache) {
                result = cache.get();
                if (result == null) {
                    result = Optional.ofNullable(delegate.get());
                    cache.set(result);
                }
            }
            return result.orElse(null);
        };
    }

    private static BibEntry fetchEntryFromWeb(Field field, String identifier, ImportFormatPreferences importFormatPreferences) {
        return WebFetchers.getIdBasedFetcherForField(field, importFormatPreferences)
                          .flatMap(fetcher -> {
                              try {
                                  return fetcher.performSearchById(identifier);
                              } catch (FetcherException e) {
                                  LOGGER.warn("Failed to fetch entry by {} {}", field, identifier, e);
                              } catch (RuntimeException e) {
                                  LOGGER.warn("Unexpected error during fetch by {} {}", field, identifier, e);
                              }
                              return Optional.empty();
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
