package org.jabref.gui.preview;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.print.PrinterJob;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.Highlighter;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.WebViewStore;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.Number;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.strings.StringUtil;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.events.EventTarget;
import org.w3c.dom.html.HTMLAnchorElement;

/**
 * Displays an BibEntry using the given layout format.
 */
public class PreviewViewer extends ScrollPane implements InvalidationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewViewer.class);

    private static final String JS_GET_SELECTION_HTML_SCRIPT = """
        function getSelectionHtml() {
            var html = "";
            if (typeof window.getSelection != "undefined") {
                var sel = window.getSelection();
                if (sel.rangeCount) {
                    var container = document.createElement("div");
                    for (var i = 0, len = sel.rangeCount; i < len; ++i) {
                        container.appendChild(sel.getRangeAt(i).cloneContents());
                    }
                    html = container.innerHTML;
                }
            } else if (typeof document.selection != "undefined") {
                if (document.selection.type == "Text") {
                    html = document.selection.createRange().htmlText;
                }
            }
            return html;
        }
        getSelectionHtml();
        """;

    private final ClipBoardManager clipBoardManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final GuiPreferences preferences;
    private final StringProperty searchQueryProperty;
    private final WebView previewView;

    private @Nullable BibDatabaseContext databaseContext;
    private @Nullable BibEntry entry;
    private PreviewLayout layout;
    private String layoutText;

    public PreviewViewer(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor) {
        this(dialogService, preferences, themeManager, taskExecutor, new SimpleStringProperty());
    }

    public PreviewViewer(DialogService dialogService, GuiPreferences preferences, ThemeManager themeManager, TaskExecutor taskExecutor, StringProperty searchQueryProperty) {
        this.dialogService = dialogService;
        this.clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.searchQueryProperty = searchQueryProperty;

        setFitToHeight(true);
        setFitToWidth(true);

        this.previewView = WebViewStore.get();
        setContent(previewView);

        configurePreviewView(themeManager);
        this.searchQueryProperty.addListener((observable, oldValue, newValue) -> highlightLayoutText());
    }

    private void configurePreviewView(ThemeManager themeManager) {
        previewView.setContextMenuEnabled(false);
        previewView.getEngine().setJavaScriptEnabled(true);
        themeManager.installCss(previewView.getEngine());

        previewView.getEngine().getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState != Worker.State.SUCCEEDED) {
                return;
            }

            NodeList anchorList = previewView.getEngine().getDocument().getElementsByTagName("a");
            for (int i = 0; i < anchorList.getLength(); i++) {
                Node node = anchorList.item(i);
                EventTarget eventTarget = (EventTarget) node;
                eventTarget.addEventListener("click", evt -> {
                    HTMLAnchorElement anchor = (HTMLAnchorElement) evt.getCurrentTarget();
                    String href = anchor.getHref();
                    if (href != null) {
                        try {
                            NativeDesktop.openBrowser(href, preferences.getExternalApplicationsPreferences());
                        } catch (IOException e) {
                            LOGGER.error("Could not open URL: {}", href, e);
                        }
                    }
                    evt.preventDefault();
                }, false);
            }
        });
    }

    public void setLayout(PreviewLayout newLayout) {
        if ((newLayout == null) || newLayout.equals(layout)) {
            return;
        }
        this.layout = newLayout;
        update();
    }

    public void setEntry(@Nullable BibEntry newEntry) {
        if (Objects.equals(entry, newEntry)) {
            return;
        }
        if (entry != null) {
            for (Observable obs : entry.getObservables()) {
                obs.removeListener(this);
            }
        }
        this.entry = newEntry;
        if (entry != null) {
            for (Observable obs : entry.getObservables()) {
                obs.addListener(this);
            }
        }
        update();
    }

    public void setDatabaseContext(BibDatabaseContext newDatabaseContext) {
        if (Objects.equals(databaseContext, newDatabaseContext)) {
            return;
        }
        setEntry(null);
        this.databaseContext = newDatabaseContext;
        update();
    }

    private void update() {
        if ((databaseContext == null) || (entry == null) || (layout == null)) {
            LOGGER.debug("Missing components - Database: {}, Entry: {}, Layout: {}",
                    (databaseContext == null) ? "null" : "OK",
                    (entry == null) ? "null" : "OK",
                    (layout == null) ? "null" : "OK");
            setPreviewText("");
            return;
        }

        Number.serialExportNumber = 1;
        BibEntry currentEntry = entry;

        BackgroundTask.wrap(() -> layout.generatePreview(currentEntry, databaseContext))
                      .onSuccess(this::setPreviewText)
                      .onFailure(e -> setPreviewText(formatError(currentEntry, e)))
                      .executeWith(taskExecutor);
    }

    private String formatError(BibEntry entry, Throwable exception) {
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        return String.format("%s\n\n%s\n\nBibTeX (internal):\n%s\n\nStack Trace:\n%s",
                Localization.lang("Error while generating citation style"),
                exception.getLocalizedMessage(),
                entry,
                sw.toString());
    }

    private void setPreviewText(String text) {
        layoutText = """
                <html><body id=\"previewBody\"><div id=\"content\"> %s </div></body></html>
                """.formatted(text);
        highlightLayoutText();
        setHvalue(0);
    }

    private void highlightLayoutText() {
        if (layoutText == null) {
            return;
        }

        String query = searchQueryProperty.get();
        if (StringUtil.isNotBlank(query)) {
            SearchQuery searchQuery = new SearchQuery(query);
            String highlighted = Highlighter.highlightHtml(layoutText, searchQuery);
            Platform.runLater(() -> previewView.getEngine().loadContent(highlighted));
        } else {
            Platform.runLater(() -> previewView.getEngine().loadContent(layoutText));
        }
    }

    public void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (!dialogService.showPrintDialog(job) || (entry == null)) {
            return;
        }

        BackgroundTask.wrap(() -> {
                          job.getJobSettings().setJobName(entry.getCitationKey().orElse("NO CITATION KEY"));
                          previewView.getEngine().print(job);
                          job.endJob();
                      }).onFailure(e -> dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"), e))
                      .executeWith(taskExecutor);
    }

    /**
     * New implementation based on CopyCitationAction to maintain HTML formatting when copying preview
     */
    public void copyPreviewHtmlToClipBoard() {
        if ((entry == null) || (layout == null) || (databaseContext == null)) {
            LOGGER.warn("Cannot copy preview citation: Missing entry, layout, or database context.");
            return;
        }

        try {
            String citationHtml = layout.generatePreview(entry, databaseContext);
            ClipboardContent content = ClipboardContentGenerator.processHtml(java.util.List.of(citationHtml));
            clipBoardManager.setContent(content);
        } catch (Exception e) {
            LOGGER.error("Failed to generate or copy citation HTML", e);
            dialogService.showErrorDialogAndWait(Localization.lang("Could not copy citation"), e);
        }
    }



    /**
     * Deprecated: Use {@link #copyPreviewHtmlToClipBoard()} instead.
     */
    public void copyPreviewTextToClipBoard() {
        // Deprecated in favor of copyPreviewHtmlToClipBoard using CopyCitationAction
    }

    public void copySelectionToClipBoard() {
        var content = new javafx.scene.input.ClipboardContent();
        content.putString(getSelectionTextContent());
        content.putHtml(getSelectionHtmlContent());
        clipBoardManager.setContent(content);
    }

    public void exportToClipBoard(StateManager stateManager) {
        new ExportToClipboardAction(dialogService, stateManager, clipBoardManager, taskExecutor, preferences).execute();
    }

    @Override
    public void invalidated(Observable observable) {
        update();
    }

    public String getSelectionTextContent() {
        return (String) previewView.getEngine().executeScript("window.getSelection().toString()");
    }

    public String getSelectionHtmlContent() {
        return (String) previewView.getEngine().executeScript(JS_GET_SELECTION_HTML_SCRIPT);
    }
}
