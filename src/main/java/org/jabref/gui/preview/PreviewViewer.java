package org.jabref.gui.preview;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker;
import javafx.print.PrinterJob;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;

import org.jabref.Globals;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Displays an BibEntry using the given layout format.
 */
public class PreviewViewer extends ScrollPane implements InvalidationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewViewer.class);

    private static final String JS_HIGHLIGHT_FUNCTION = " <script type=\"text/javascript\">\r\n" +
                                                        "        function highlight(text) {\r\n" +
                                                        "            var innertxt = document.body.innerText;\r\n" +
                                                        "            var response = innertxt.replace(new RegExp(text, 'gi'), str => `<span style='background-color:red'>${str}</span>`);\r\n" +
                                                        "            document.body.innerHTML = response;\r\n" +
                                                        "        }\r\n" +
                                                        "    </script>";

    private final ClipBoardManager clipBoardManager;
    private final DialogService dialogService;

    private final TaskExecutor taskExecutor = Globals.TASK_EXECUTOR;
    private final WebView previewView;
    private PreviewLayout layout;

    /**
     * The entry currently shown
     */
    private Optional<BibEntry> entry = Optional.empty();
    private Optional<Pattern> searchHighlightPattern = Optional.empty();

    private BibDatabaseContext database;
    private boolean registered;

    private ChangeListener<Optional<SearchQuery>> listener = (queryObservable, queryOldValue, queryNewValue) -> {
        searchHighlightPattern = queryNewValue.flatMap(SearchQuery::getPatternForWords);
        highlightSearchPattern();
    };

    /**
     * @param database Used for resolving strings and pdf directories for links.
     */
    public PreviewViewer(BibDatabaseContext database, DialogService dialogService, StateManager stateManager) {
        this.database = Objects.requireNonNull(database);
        this.dialogService = dialogService;
        this.clipBoardManager = Globals.clipboardManager;

        setFitToHeight(true);
        setFitToWidth(true);
        previewView = new WebView();
        setContent(previewView);
        previewView.setContextMenuEnabled(false);

        previewView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {

            if (newValue != Worker.State.SUCCEEDED) {
                return;
            }
            if (!registered) {
                stateManager.activeSearchQueryProperty().addListener(listener);
                registered = true;
            }
            highlightSearchPattern();
        });

    }

    private void highlightSearchPattern() {
        if (searchHighlightPattern.isPresent()) {
            String pattern = searchHighlightPattern.get().pattern().replace("\\Q", "").replace("\\E", "");

            previewView.getEngine().executeScript("highlight('" + pattern + "');");
        }
    }

    public void setLayout(PreviewLayout newLayout) {
        layout = newLayout;
        update();
    }

    public void setEntry(BibEntry newEntry) {
        // Remove update listener for old entry
        entry.ifPresent(oldEntry -> {
            for (Observable observable : oldEntry.getObservables()) {
                observable.removeListener(this);
            }
        });

        entry = Optional.of(newEntry);

        // Register for changes
        for (Observable observable : newEntry.getObservables()) {
            observable.addListener(this);
        }
        update();

    }

    private void update() {
        if (!entry.isPresent() || layout == null) {
            // Nothing to do
            return;
        }

        ExporterFactory.entryNumber = 1; // Set entry number in case that is included in the preview layout.

        BackgroundTask
                      .wrap(() -> layout.generatePreview(entry.get(), database.getDatabase()))
                      .onRunning(() -> setPreviewText("<i>" + Localization.lang("Processing %0", Localization.lang("Citation Style")) + ": " + layout.getName() + " ..." + "</i>"))
                      .onSuccess(this::setPreviewText)
                      .onFailure(exception -> {
                          LOGGER.error("Error while generating citation style", exception);
                          setPreviewText(Localization.lang("Error while generating citation style"));
                      })
                      .executeWith(taskExecutor);
    }

    private void setPreviewText(String text) {
        String myText = JS_HIGHLIGHT_FUNCTION + "<div id=\"content\"" + text + "</div>";
        previewView.getEngine().setJavaScriptEnabled(true);
        previewView.getEngine().loadContent(myText);

        this.setHvalue(0);
    }

    public void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        boolean proceed = dialogService.showPrintDialog(job);
        if (!proceed) {
            return;
        }

        BackgroundTask
                      .wrap(() -> {
                          job.getJobSettings().setJobName(entry.flatMap(BibEntry::getCiteKeyOptional).orElse("NO ENTRY"));
                          previewView.getEngine().print(job);
                          job.endJob();
                      })
                      .onFailure(exception -> dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"), exception))
                      .executeWith(taskExecutor);
    }

    public void copyPreviewToClipBoard() {
        StringBuilder previewStringContent = new StringBuilder();
        Document document = previewView.getEngine().getDocument();

        NodeList nodeList = document.getElementsByTagName("html");

        //Nodelist does not implement iterable
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            previewStringContent.append(element.getTextContent());
        }

        ClipboardContent content = new ClipboardContent();
        content.putString(previewStringContent.toString());
        content.putHtml((String) previewView.getEngine().executeScript("document.documentElement.outerHTML"));

        clipBoardManager.setContent(content);
    }

    @Override
    public void invalidated(Observable observable) {
        update();
    }

    public String getSelectionHtmlContent() {
        return (String) previewView.getEngine().executeScript("window.getSelection().toString()");
    }
}
