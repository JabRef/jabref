package org.jabref.gui.preview;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.print.PrinterJob;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.ClipboardContent;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.exporter.ExportToClipboardAction;
import org.jabref.gui.importer.BookCoverFetcher;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.search.Highlighter;
import org.jabref.gui.theme.ThemeManager;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.htmltonode.HtmlRenderOptions;
import org.jabref.htmltonode.HtmlView;
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

/// Displays an BibEntry using the given layout format.
///
/// Rendering is done with the html-to-node library (plain JavaFX nodes, no `javafx.web`).
public class PreviewViewer extends ScrollPane implements InvalidationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewViewer.class);

    private static final String COVER_IMAGE_FORMAT_HTML = "<img style=\"border-width:1px; border-style:solid; border-color:auto; display:block; height:12rem;\" src=\"%s\"> <br>";

    private final ClipBoardManager clipBoardManager;
    private final DialogService dialogService;
    private final TaskExecutor taskExecutor;
    private final HtmlView previewView;
    private final StringProperty searchQueryProperty;
    private final GuiPreferences preferences;

    private final BookCoverFetcher bookCoverFetcher;

    private @Nullable BibDatabaseContext databaseContext;
    private @Nullable BibEntry entry;
    private PreviewLayout layout;
    private String layoutText;

    public PreviewViewer(DialogService dialogService,
                         GuiPreferences preferences,
                         ThemeManager themeManager,
                         TaskExecutor taskExecutor) {
        this(dialogService, preferences, themeManager, taskExecutor, new SimpleStringProperty());
    }

    public PreviewViewer(DialogService dialogService,
                         GuiPreferences preferences,
                         ThemeManager themeManager, // kept for call-site compatibility; theming works via regular JavaFX CSS now
                         TaskExecutor taskExecutor,
                         StringProperty searchQueryProperty) {
        this.dialogService = dialogService;
        this.clipBoardManager = Injector.instantiateModelOrService(ClipBoardManager.class);
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
        this.searchQueryProperty = searchQueryProperty;
        this.searchQueryProperty.addListener((_, _, _) -> highlightLayoutText());

        this.bookCoverFetcher = new BookCoverFetcher(preferences.getExternalApplicationsPreferences());

        setFitToWidth(true);
        setFitToHeight(true);
        // WebView reported a fixed 800x600 preferred size. Keep that stable geometry so
        // surrounding layouts (preview panel, dialogs) behave as before, instead of exposing
        // the content-dependent preferred size of the rendered nodes.
        setPrefSize(800, 600);
        previewView = new HtmlView();
        previewView.setOptions(HtmlRenderOptions.defaults().withLinkHandler(this::openLink));
        setContent(previewView);
    }

    private void openLink(String href) {
        try {
            NativeDesktop.openBrowser(href, preferences.getExternalApplicationsPreferences());
        } catch (MalformedURLException exception) {
            LOGGER.error("Invalid URL", exception);
        } catch (IOException e) {
            LOGGER.error("Could not open URL: {}", href, e);
        }
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

    public void clearEntry() {
        setEntry(null);
    }

    public void setDatabaseContext(BibDatabaseContext newDatabaseContext) {
        if (Objects.equals(databaseContext, newDatabaseContext)) {
            return;
        }
        clearEntry();
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
                      .onSuccess(previewText -> {
                          setPreviewText(previewText);
                          if (preferences.getPreviewPreferences().shouldDownloadCovers()) {
                              downloadCoverAndRefresh(currentEntry, previewText);
                          }
                      })
                      .onFailure(e -> setPreviewText(formatError(currentEntry, e)))
                      .executeWith(taskExecutor);
    }

    private void downloadCoverAndRefresh(BibEntry entry, String previewText) {
        BackgroundTask.wrap(() -> bookCoverFetcher.downloadCoversForEntry(entry))
                      .onSuccess((ignored) -> {
                          if (entry.equals(this.entry)) {
                              setPreviewText(previewText);
                          }
                      })
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
        String coverIfAny = getCoverImageURL().map(COVER_IMAGE_FORMAT_HTML::formatted).orElse("");
        layoutText = coverIfAny + text;
        UiTaskExecutor.runInJavaFXThread(() -> {
            HtmlRenderOptions options = previewView.getOptions();
            previewView.setOptions(getBaseURL()
                    .map(options::withBaseUri)
                    .orElseGet(options::withoutBaseUri));
        });
        highlightLayoutText();
        setHvalue(0);
    }

    private Optional<String> getBaseURL() {
        if (databaseContext == null) {
            return Optional.empty();
        }
        return databaseContext.getFirstExistingFileDir(preferences.getFilePreferences()).map(path -> {
            String url = path.toUri().toString();
            if (!url.endsWith("/")) {
                url += "/";
            }
            return url;
        });
    }

    private Optional<String> getCoverImageURL() {
        if (entry != null) {
            return bookCoverFetcher.getDownloadedCoverForEntry(entry).map(path -> path.toUri().toString());
        }
        return Optional.empty();
    }

    private void highlightLayoutText() {
        if (layoutText == null) {
            return;
        }

        String queryText = searchQueryProperty.get();
        if (StringUtil.isNotBlank(queryText)) {
            SearchQuery searchQuery = new SearchQuery(queryText);
            String highlighted = Highlighter.highlightHtml(layoutText, searchQuery);
            UiTaskExecutor.runInJavaFXThread(() -> previewView.setHtml(highlighted));
        } else {
            UiTaskExecutor.runInJavaFXThread(() -> previewView.setHtml(layoutText));
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

        job.getJobSettings().setJobName(entry.getCitationKey().orElse("NO CITATION KEY"));
        // printPage must run on the JavaFX thread while the node is attached to a scene;
        // it only renders the page. The blocking spooling (endJob) happens in the background.
        boolean printed = job.printPage(previewView);
        if (!printed) {
            dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"));
            job.cancelJob();
            return;
        }
        BackgroundTask.wrap(job::endJob)
                      .onSuccess(success -> {
                          if (!success) {
                              dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"));
                          }
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

        ClipboardContent content = new ClipboardContent();
        content.putString(previewView.toPlainText());
        clipBoardManager.setContent(content);
    }

    public void copySelectionToClipBoard() {
        if ((entry == null) || (layout == null) || (databaseContext == null)) {
            LOGGER.warn("Cannot copy preview citation: Missing entry, layout, or database context.");
            return;
        }

        // Text selection is not supported (yet) without WebView; copy the whole preview instead
        ClipboardContent content = new ClipboardContent();
        content.putString(previewView.toPlainText());
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

    public void resizeForTooltipContent() {
        setVbarPolicy(ScrollBarPolicy.NEVER);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        // Tooltips size to the rendered content: fixed width, natural height
        // (WebView needed a JavaScript measurement hack for this). Only write the viewport
        // height when it actually changed, so rendering does not queue redundant relayouts.
        setPrefSize(USE_COMPUTED_SIZE, USE_COMPUTED_SIZE);
        setPrefViewportWidth(750);
        previewView.layoutBoundsProperty().addListener((_, _, bounds) -> {
            if (Math.abs(getPrefViewportHeight() - bounds.getHeight()) >= 1) {
                setPrefViewportHeight(bounds.getHeight());
            }
        });
    }

    @Override
    public void invalidated(Observable observable) {
        update();
    }

    public String getSelectionHtmlContent() {
        // Text selection is not supported (yet) without WebView; return the whole preview instead
        return layoutText == null ? "" : layoutText;
    }
}
