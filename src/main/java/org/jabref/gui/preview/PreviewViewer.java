package org.jabref.gui.preview;

import java.util.Objects;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.print.PrinterJob;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;
import javafx.scene.web.WebView;

import org.jabref.Globals;
import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.PreviewLayout;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.l10n.Localization;
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

    private final ClipBoardManager clipBoardManager;
    private final DialogService dialogService;

    private final TaskExecutor taskExecutor = Globals.TASK_EXECUTOR;
    private final WebView previewView;
    private PreviewLayout layout;
    /**
     * The entry currently shown
     */
    private Optional<BibEntry> entry = Optional.empty();
    private BibDatabaseContext database;

    /**
     * @param database Used for resolving strings and pdf directories for links.
     */
    public PreviewViewer(BibDatabaseContext database, DialogService dialogService) {
        this.database = Objects.requireNonNull(database);
        this.dialogService = dialogService;
        this.clipBoardManager = Globals.clipboardManager;

        setFitToHeight(true);
        setFitToWidth(true);
        previewView = new WebView();
        setContent(previewView);
        previewView.setContextMenuEnabled(false);
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
        previewView.getEngine().loadContent(text);
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
