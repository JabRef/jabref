package org.jabref.gui.preview;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Worker;
import javafx.print.PrinterJob;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.Highlighter;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.util.WebViewStore;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.Number;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

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

    // https://stackoverflow.com/questions/5669448/get-selected-texts-html-in-div/5670825#5670825
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
    private final WebView previewView;
    private final StringProperty searchQueryProperty;
    private final GuiPreferences preferences;

    private @Nullable BibDatabaseContext databaseContext;
    private @Nullable BibEntry entry;
    private PreviewLayout layout;
    private String layoutText;
    private final ReadOnlyDoubleWrapper contentHeight = new ReadOnlyDoubleWrapper(0);
    private final ReadOnlyDoubleWrapper contentWidth = new ReadOnlyDoubleWrapper(0);

    public PreviewViewer(DialogService dialogService,
                         GuiPreferences preferences,
                         ThemeManager themeManager,
                         TaskExecutor taskExecutor) {
        this(dialogService, preferences, themeManager, taskExecutor, new SimpleStringProperty());
    }

    public PreviewViewer(DialogService dialogService,
                         GuiPreferences preferences,
                         ThemeManager themeManager,
                         TaskExecutor taskExecutor,
                         StringProperty searchQueryProperty) {
        this.dialogService = dialogService;
        this.clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.searchQueryProperty = searchQueryProperty;
        this.searchQueryProperty.addListener((_, _, _) -> highlightLayoutText());

        setFitToHeight(false);
        setFitToWidth(false);

        setHbarPolicy(ScrollBarPolicy.NEVER);
        setVbarPolicy(ScrollBarPolicy.NEVER);
        previewView = WebViewStore.get();
        setContent(previewView);

        configurePreviewView(themeManager);
    }

    private void configurePreviewView(ThemeManager themeManager) {
        previewView.setContextMenuEnabled(false);
        previewView.getEngine().setJavaScriptEnabled(true);
        themeManager.installCss(previewView.getEngine());

        previewView.getEngine().getLoadWorker().stateProperty().addListener((_, _, newValue) -> {
            if (newValue != Worker.State.SUCCEEDED) {
                return;
            }

            // https://stackoverflow.com/questions/15555510/javafx-stop-opening-url-in-webview-open-in-browser-instead
            NodeList anchorList = previewView.getEngine().getDocument().getElementsByTagName("a");
            for (int i = 0; i < anchorList.getLength(); i++) {
                Node node = anchorList.item(i);
                EventTarget eventTarget = (EventTarget) node;
                eventTarget.addEventListener("click", evt -> {
                    EventTarget target = evt.getCurrentTarget();
                    HTMLAnchorElement anchorElement = (HTMLAnchorElement) target;
                    String href = anchorElement.getHref();
                    if (href != null) {
                        try {
                            NativeDesktop.openBrowser(href, preferences.getExternalApplicationsPreferences());
                        } catch (MalformedURLException exception) {
                            LOGGER.error("Invalid URL", exception);
                        } catch (IOException e) {
                            LOGGER.error("Could not open URL: {}", href, e);
                        }
                    }
                    evt.preventDefault();
                }, false);
            }

            try {
                Object heightObj = previewView.getEngine().executeScript("document.getElementById('content').scrollHeight || document.body.scrollHeight");
                Object widthObj = previewView.getEngine().executeScript("document.getElementById('content').scrollWidth || document.body.scrollWidth");

                Double height = null;
                if (heightObj instanceof java.lang.Number heightNum) {
                    height = heightNum.doubleValue();
                }

                Double width = null;
                if (widthObj instanceof java.lang.Number widthNum) {
                    width = widthNum.doubleValue();
                }

                // Use a single runLater to update both properties at once. We store the
                // measured values as Optionals to make the intent explicit.
                Optional<Double> optHeight = Optional.ofNullable(height);
                Optional<Double> optWidth = Optional.ofNullable(width);

                javafx.application.Platform.runLater(() -> {
                    optHeight.ifPresent(h -> {
                        // small padding so the rendered content is not clipped at edges
                        contentHeight.set(h);
                        this.setPrefHeight(h + 8);
                    });
                    optWidth.ifPresent(w -> {
                        contentWidth.set(w);
                        this.setPrefWidth(w + 8);
                    });
                });

                setHvalue(0);
            } catch (Exception e) {
                LOGGER.debug("Could not compute preview content size", e);
            }
        });
    }

    public ReadOnlyDoubleProperty contentHeightProperty() {
        return contentHeight.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty contentWidthProperty() {
        return contentWidth.getReadOnlyProperty();
    }

    public void setLayout(PreviewLayout newLayout) {
        // Change listeners might set the layout to null while the update method is executing, therefore, we need to prevent this here
        if ((newLayout == null) || newLayout.equals(layout)) {
            return;
        }
        layout = newLayout;
        update();
    }

    public void setEntry(@Nullable BibEntry newEntry) {
        if (Objects.equals(entry, newEntry)) {
            return;
        }

        // Remove update listener for old entry
        if (entry != null) {
            for (Observable observable : entry.getObservables()) {
                observable.removeListener(this);
            }
        }

        entry = newEntry;

        // Register listeners for new entry
        if (entry != null) {
            for (Observable observable : entry.getObservables()) {
                observable.addListener(this);
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
                    databaseContext == null ? "null" : databaseContext,
                    entry == null ? "null" : entry,
                    layout == null ? "null" : layout);
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
        LOGGER.error("Error generating preview for entry: {}", entry.getCitationKey(), exception);

        return """
                <div class="error">
                    <h3>%s</h3>
                    <p>%s</p>
                    <p><small>Check the event logs for details.</small></p>
                </div>
                """.formatted(
                Localization.lang("Error while generating citation style"),
                exception.getLocalizedMessage() != null ? exception.getLocalizedMessage() : "Unknown error");
    }

    private void setPreviewText(String text) {
        layoutText = """
                <html>
                    <body id="previewBody">
                        <div id="content"> %s </div>
                    </body>
                </html>
                """.formatted(text);
        highlightLayoutText();
        setHvalue(0);
    }

    private void highlightLayoutText() {
        if (layoutText == null) {
            return;
        }

        String queryText = searchQueryProperty.get();
        if (StringUtil.isNotBlank(queryText)) {
            SearchQuery searchQuery = new SearchQuery(queryText);
            String highlighted = Highlighter.highlightHtml(layoutText, searchQuery);
            UiTaskExecutor.runInJavaFXThread(() -> previewView.getEngine().loadContent(highlighted));
        } else {
            UiTaskExecutor.runInJavaFXThread(() -> previewView.getEngine().loadContent(layoutText));
        }
    }

    public void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) {
            LOGGER.warn("PrinterJob.createPrinterJob() returned null; printing not available");
            dialogService.showErrorDialogAndWait("Could not find an available printer");
            return;
        }
        if ((entry == null) || !dialogService.showPrintDialog(job)) {
            return;
        }

        BackgroundTask.wrap(() -> {
                          job.getJobSettings().setJobName(entry.getCitationKey().orElse("NO CITATION KEY"));
                          previewView.getEngine().print(job);
                          job.endJob();
                      })
                      .onFailure(e -> dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"), e))
                      .executeWith(taskExecutor);
    }

    public void copyPreviewHtmlToClipBoard() {
        if ((entry == null) || (layout == null) || (databaseContext == null)) {
            LOGGER.warn("Cannot copy preview citation: Missing entry, layout, or database context.");
            return;
        }

        String citationHtml = layout.generatePreview(entry, databaseContext);
        ClipboardContent content = ClipboardContentGenerator.processHtml(List.of(citationHtml));
        clipBoardManager.setContent(content);
    }

    public void copyPreviewPlainTextToClipBoard() {
        if ((entry == null) || (layout == null) || (databaseContext == null)) {
            LOGGER.warn("Cannot copy preview citation: Missing entry, layout, or database context.");
            return;
        }

        clipBoardManager.setContent((String) previewView.getEngine().executeScript("document.body.innerText"));
    }

    public void copySelectionToClipBoard() {
        if ((entry == null) || (layout == null) || (databaseContext == null)) {
            LOGGER.warn("Cannot copy preview citation: Missing entry, layout, or database context.");
            return;
        }

        ClipboardContent content = new ClipboardContent();
        content.putString((String) previewView.getEngine().executeScript("window.getSelection().toString()"));
        content.putHtml(getSelectionHtmlContent());
        clipBoardManager.setContent(content);
    }

    public void exportToClipBoard(StateManager stateManager) {
        ExportToClipboardAction exportToClipboardAction = new ExportToClipboardAction(
                dialogService,
                stateManager,
                clipBoardManager,
                taskExecutor,
                preferences);
        exportToClipboardAction.execute();
    }

    @Override
    public void invalidated(Observable observable) {
        update();
    }

    public String getSelectionHtmlContent() {
        return (String) previewView.getEngine().executeScript(JS_GET_SELECTION_HTML_SCRIPT);
    }
}
