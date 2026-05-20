package org.jabref.gui.ai.summary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.ai.summarization.exporters.AiSummaryJsonExporter;
import org.jabref.logic.ai.summarization.exporters.AiSummaryMarkdownExporter;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.MarkdownFormatter;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.ai.AiMetadata;
import org.jabref.model.ai.identifiers.FullBibEntry;
import org.jabref.model.ai.summarization.AiSummary;
import org.jabref.model.entry.BibEntryTypesManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiSummaryShowingViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(AiSummaryShowingViewModel.class);

    private static final String HTML_TEMPLATE = """
            <body style="margin: 0; padding: 5px; width: 100vw">
                <div style="white-space: pre-wrap; word-wrap: break-word; width: 100vw">%s</div>
            </body>
            """;

    private static final MarkdownFormatter MARKDOWN_FORMATTER = new MarkdownFormatter();

    private final ObjectProperty<AiSummary> summary = new SimpleObjectProperty<>();
    private final ObjectProperty<FullBibEntry> entry = new SimpleObjectProperty<>();
    private final BooleanProperty isMarkdown = new SimpleBooleanProperty(true);

    private final StringProperty webViewSource = new SimpleStringProperty("");

    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerate = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustom = new SimpleObjectProperty<>();

    private final AiPreferences aiPreferences;
    private final FieldPreferences fieldPreferences;
    private final BibEntryTypesManager entryTypesManager;
    private final DialogService dialogService;

    public AiSummaryShowingViewModel(
            AiPreferences aiPreferences,
            FieldPreferences fieldPreferences,
            BibEntryTypesManager entryTypesManager,
            DialogService dialogService
    ) {
        this.aiPreferences = aiPreferences;
        this.fieldPreferences = fieldPreferences;
        this.entryTypesManager = entryTypesManager;
        this.dialogService = dialogService;

        setupBindings();
    }

    private void setupBindings() {
        webViewSource.bind(Bindings.createObjectBinding(
                this::generateWebSource,
                summary, isMarkdown
        ));
    }

    private String generateWebSource() {
        if (summary.get() == null) {
            return "";
        }

        String content = summary.get().content();

        if (isMarkdown.get()) {
            return MARKDOWN_FORMATTER.format(content);
        } else {
            return HTML_TEMPLATE.formatted(content);
        }
    }

    public void regenerate() {
        BindingsHelper.handle(onRegenerate);
    }

    public void regenerateCustom() {
        BindingsHelper.handle(onRegenerateCustom);
    }

    public void exportMarkdown() {
        AiSummary currentSummary = summary.get();
        FullBibEntry fullEntry = entry.get();

        if (currentSummary == null || fullEntry == null) {
            dialogService.notify(Localization.lang("No summary available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.MARKDOWN)
                .withDefaultExtension(StandardFileType.MARKDOWN)
                .withInitialDirectory(Directories.getUserDirectory())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiSummaryMarkdownExporter exporter = new AiSummaryMarkdownExporter(entryTypesManager, fieldPreferences, aiPreferences.getMarkdownChatExportTemplate());
                             AiMetadata metadata = currentSummary.metadata();
                             String content = exporter.export(metadata, fullEntry.entry(), fullEntry.databaseContext().getMode(), currentSummary);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    public void exportJson() {
        AiSummary currentSummary = summary.get();
        FullBibEntry fullEntry = entry.get();

        if (currentSummary == null || fullEntry == null) {
            dialogService.notify(Localization.lang("No summary available to export"));
            return;
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(StandardFileType.JSON)
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialDirectory(Directories.getUserDirectory())
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration)
                     .ifPresent(path -> {
                         try {
                             AiSummaryJsonExporter exporter = new AiSummaryJsonExporter(entryTypesManager, fieldPreferences);
                             AiMetadata metadata = currentSummary.metadata();
                             String content = exporter.export(metadata, fullEntry.entry(), fullEntry.databaseContext().getMode(), currentSummary);
                             Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                             dialogService.notify(Localization.lang("Export operation finished successfully."));
                         } catch (IOException e) {
                             LOGGER.error("Problem occurred while writing the export file", e);
                             dialogService.showErrorDialogAndWait(Localization.lang("Problem occurred while writing the export file"), e);
                         }
                     });
    }

    public ObjectProperty<AiSummary> summaryProperty() {
        return summary;
    }

    public ObjectProperty<FullBibEntry> entryProperty() {
        return entry;
    }

    public BooleanProperty isMarkdownProperty() {
        return isMarkdown;
    }

    public StringProperty webViewSourceProperty() {
        return webViewSource;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateProperty() {
        return onRegenerate;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onRegenerateCustomProperty() {
        return onRegenerateCustom;
    }
}
