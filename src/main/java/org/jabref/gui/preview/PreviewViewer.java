package org.jabref.gui.preview;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Objects;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
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
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.OptionalObjectProperty;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.format.Number;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.search.retrieval.Highlighter;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.WebViewStore;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.query.SearchQuery;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
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
    private final OptionalObjectProperty<SearchQuery> searchQueryProperty;
    private final GuiPreferences preferences;

    // Used for resolving strings and pdf directories for links.
    private @Nullable BibDatabaseContext databaseContext;

    private @Nullable BibEntry entry;

    private PreviewLayout layout;

    private String layoutText;

    public PreviewViewer(DialogService dialogService,
                         GuiPreferences preferences,
                         ThemeManager themeManager,
                         TaskExecutor taskExecutor) {
        this(dialogService, preferences, themeManager, taskExecutor, OptionalObjectProperty.empty());
    }

    public PreviewViewer(DialogService dialogService,
                         GuiPreferences preferences,
                         ThemeManager themeManager,
                         TaskExecutor taskExecutor,
                         OptionalObjectProperty<SearchQuery> searchQueryProperty) {
        this.dialogService = dialogService;
        this.clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.searchQueryProperty = searchQueryProperty;
        this.searchQueryProperty.addListener((queryObservable, queryOldValue, queryNewValue) -> highlightLayoutText());

        setFitToHeight(true);
        setFitToWidth(true);
        previewView = WebViewStore.get();
        setContent(previewView);
        previewView.setContextMenuEnabled(false);
        previewView.getEngine().setJavaScriptEnabled(true);

        previewView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
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
                        } catch (IOException exception) {
                            LOGGER.error("Invalid URL Input", exception);
                        }
                    }
                    evt.preventDefault();
                }, false);
            }
        });

        themeManager.installCss(previewView.getEngine());
    }

    public void setLayout(PreviewLayout newLayout) {
        // Change listeners might set the layout to null while the update method is executing, therefore we need to prevent this here
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

        if (entry != null) {
            // Register for changes
            for (Observable observable : newEntry.getObservables()) {
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

        databaseContext = newDatabaseContext;
        update();
    }

    private void update() {
        if ((databaseContext == null) || (entry == null) || (layout == null)) {
            LOGGER.debug("databaseContext null {}, entry null {}, or layout null {}", databaseContext == null, entry == null, layout == null);
            // Make sure that the preview panel is not completely white, especially with dark theme on
            setPreviewText("");
            return;
        }

        Number.serialExportNumber = 1; // Set entry number in case that is included in the preview layout.

        final BibEntry theEntry = entry;
        BackgroundTask
                .wrap(() -> layout.generatePreview(theEntry, databaseContext))
                .onSuccess(this::setPreviewText)
                .onFailure(exception -> {
                    LOGGER.error("Error while generating citation style", exception);

                    // Convert stack trace to a string
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    exception.printStackTrace(printWriter);
                    String stackTraceString = stringWriter.toString();

                    // Set the preview text with the localized error message and the stack trace
                    setPreviewText(Localization.lang("Error while generating citation style") + "\n\n" + exception.getLocalizedMessage() + "\n\nBibTeX (internal):\n" + theEntry + "\n\nStack Trace:\n" + stackTraceString);
                })
                .executeWith(taskExecutor);
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
        this.setHvalue(0);
    }

    private void highlightLayoutText() {
        if (layoutText == null) {
            return;
        }
        if (searchQueryProperty.get().isPresent()) {
            String highlightedHtml = Highlighter.highlightHtml(layoutText, searchQueryProperty.get().get());
            previewView.getEngine().loadContent(highlightedHtml);
        } else {
            previewView.getEngine().loadContent(layoutText);
        }
    }

    public void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        boolean proceed = dialogService.showPrintDialog(job);
        if (!proceed && (entry != null)) {
            return;
        }

        BackgroundTask
                .wrap(() -> {
                    job.getJobSettings().setJobName(entry.getCitationKey().orElse("NO CITATION KEY"));
                    previewView.getEngine().print(job);
                    job.endJob();
                })
                .onFailure(exception -> dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"), exception))
                .executeWith(taskExecutor);
    }

    public void copyPreviewHtmlToClipBoard() {
        Document document = previewView.getEngine().getDocument();
        ClipboardContent content = ClipboardContentGenerator.processHtml(Arrays.asList(document.getElementById("content").getTextContent()));
        clipBoardManager.setContent(content);
    }

    public void copyPreviewTextToClipBoard() {
        Document document = previewView.getEngine().getDocument();
        ClipboardContent content = ClipboardContentGenerator.processText(Arrays.asList(document.getElementById("content").getTextContent()));
        clipBoardManager.setContent(content);
    }

    public void copySelectionToClipBoard() {
        ClipboardContent content = new ClipboardContent();
        content.putString(getSelectionTextContent());
        content.putHtml(getSelectionHtmlContent());

        clipBoardManager.setContent(content);
    }

    public void exportToClipBoard(StateManager stateManager) {
        ExportToClipboardAction exportToClipboardAction = new ExportToClipboardAction(dialogService, stateManager, clipBoardManager, taskExecutor, preferences);
        exportToClipboardAction.execute();
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
