package org.jabref.gui.ai.components.summary;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

import org.jabref.gui.DialogService;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.util.FileFilterConverter;
import org.jabref.gui.util.WebViewStore;
import org.jabref.logic.ai.summarization.Summary;
import org.jabref.logic.layout.format.MarkdownFormatter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

import com.airhacks.afterburner.views.ViewLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryShowingComponent extends VBox {
    private static final Logger LOGGER = LoggerFactory.getLogger(SummaryShowingComponent.class);
    private static final MarkdownFormatter MARKDOWN_FORMATTER = new MarkdownFormatter();
    @FXML private Text summaryInfoText;
    @FXML private CheckBox markdownCheckbox;
    @FXML private MenuButton exportButton;

    private WebView contentWebView;
    private final Summary summary;
    private final BibEntry entry;
    private final DialogService dialogService;
    private final Runnable regenerateCallback;

    public SummaryShowingComponent(Summary summary, BibEntry entry, DialogService dialogService, Runnable regenerateCallback) {
        this.summary = summary;
        this.entry = entry;
        this.dialogService = dialogService;
        this.regenerateCallback = regenerateCallback;

        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        initializeWebView();
        updateContent(false); // Start in plain text mode
        updateInfoText();
    }

    private void initializeWebView() {
        contentWebView = WebViewStore.get();
        VBox.setVgrow(contentWebView, Priority.ALWAYS);

        getChildren().addFirst(contentWebView);
    }

    private void updateContent(boolean isMarkdown) {
        String content = summary.content();
        if (isMarkdown) {
            contentWebView.getEngine().loadContent(MARKDOWN_FORMATTER.format(content));
        } else {
            contentWebView.getEngine().loadContent(
                    "<body style='margin: 0; padding: 5px; width: 100vw'>" +
                            "<div style='white-space: pre-wrap; word-wrap: break-word; width: 100vw'>" +
                            content +
                            "</div></body>"
            );
        }
    }

    private void updateInfoText() {
        String newInfo = summaryInfoText
                .getText()
                .replaceAll("%0", formatTimestamp(summary.timestamp()))
                .replaceAll("%1", summary.aiProvider().getLabel() + " " + summary.model());
        summaryInfoText.setText(newInfo);
    }

    private static String formatTimestamp(LocalDateTime timestamp) {
        return timestamp.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()));
    }

    @FXML
    private void onMarkdownToggle() {
        updateContent(markdownCheckbox.isSelected());
    }

    @FXML
    private void onRegenerateButtonClick() {
        regenerateCallback.run();
    }

    @FXML
    private void onExportMarkdown() {
        String citationKey = entry.getCitationKey().orElse("entry");

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.toExtensionFilter(StandardFileType.MARKDOWN))
                .withDefaultExtension(StandardFileType.MARKDOWN)
                .withInitialFileName(citationKey + "_summary.md")
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration).ifPresent(path -> {
            try {
                String markdownContent = generateMarkdownExport();
                Files.writeString(path, markdownContent, StandardCharsets.UTF_8);
                dialogService.notify(Localization.lang("Summary exported successfully to %0", path.toString()));
            } catch (IOException e) {
                LOGGER.error("Error exporting summary to Markdown", e);
                dialogService.showErrorDialogAndWait(Localization.lang("Export error"), Localization.lang("Could not export summary: %0", e.getMessage()));
            }
        });
    }

    @FXML
    private void onExportJson() {
        String citationKey = entry.getCitationKey().orElse("entry");

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileFilterConverter.toExtensionFilter(StandardFileType.JSON))
                .withDefaultExtension(StandardFileType.JSON)
                .withInitialFileName(citationKey + "_summary.json")
                .build();

        dialogService.showFileSaveDialog(fileDialogConfiguration).ifPresent(path -> {
            try {
                String jsonContent = generateJsonExport();
                Files.writeString(path, jsonContent, StandardCharsets.UTF_8);
                dialogService.notify(Localization.lang("Summary exported successfully to %0", path.toString()));
            } catch (IOException e) {
                LOGGER.error("Error exporting summary to JSON", e);
                dialogService.showErrorDialogAndWait(Localization.lang("Export error"), Localization.lang("Could not export summary: %0", e.getMessage()));
            }
        });
    }

    private String generateMarkdownExport() {
        StringBuilder markdown = new StringBuilder();

        // BibTeX section
        markdown.append("## Bibtex\n\n");
        markdown.append("```bibtex\n");
        markdown.append(entry.getParsedSerialization());
        markdown.append("\n```\n\n");

        // Summary section
        markdown.append("## Summary\n\n");
        markdown.append(summary.content());

        return markdown.toString();
    }

    private String generateJsonExport() {
        Map<String, Object> json = new LinkedHashMap<>();

        // Entry BibTeX source code
        json.put("entry_bibtex", entry.getParsedSerialization());

        // Entry as dictionary
        Map<String, String> entryDict = new LinkedHashMap<>();
        for (Field field : entry.getFields()) {
            entry.getField(field).ifPresent(value -> entryDict.put(field.getName(), value));
        }
        json.put("entry", entryDict);

        // Provider and model from summary
        json.put("latest_provider", summary.aiProvider().getLabel());
        json.put("latest_model", summary.model());

        // Timestamp from summary
        json.put("timestamp", summary.timestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        // Summary content
        json.put("summary", summary.content());

        // Simple JSON serialization
        return formatJson(json);
    }

    private String formatJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        if (obj instanceof String str) {
            return "\"" + escapeJsonString(str) + "\"";
        }
        if (obj instanceof Number || obj instanceof Boolean) {
            return obj.toString();
        }
        if (obj instanceof Map<?, ?> map) {
            StringBuilder sb = new StringBuilder("{\n");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;
                sb.append("    \"").append(entry.getKey()).append("\": ");
                String valueStr = formatJson(entry.getValue());
                // Indent multi-line values
                if (valueStr.contains("\n")) {
                    String[] lines = valueStr.split("\n");
                    sb.append(lines[0]);
                    for (int i = 1; i < lines.length; i++) {
                        sb.append("\n    ").append(lines[i]);
                    }
                } else {
                    sb.append(valueStr);
                }
            }
            sb.append("\n}");
            return sb.toString();
        }
        if (obj instanceof List<?> list) {
            StringBuilder sb = new StringBuilder("[\n");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    sb.append(",\n");
                }
                first = false;
                String itemStr = formatJson(item);
                // Indent multi-line items
                if (itemStr.contains("\n")) {
                    String[] lines = itemStr.split("\n");
                    sb.append("    ").append(lines[0]);
                    for (int i = 1; i < lines.length; i++) {
                        sb.append("\n    ").append(lines[i]);
                    }
                } else {
                    sb.append("    ").append(itemStr);
                }
            }
            sb.append("\n]");
            return sb.toString();
        }
        return "\"" + escapeJsonString(obj.toString()) + "\"";
    }

    private String escapeJsonString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\b", "\\b")
                  .replace("\f", "\\f")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
