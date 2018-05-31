package org.jabref.gui;

import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javafx.print.PrinterJob;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.web.WebView;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.search.SearchQueryHighlightListener;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.preferences.PreviewPreferences;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Displays an BibEntry using the given layout format.
 */
public class PreviewPanel extends ScrollPane implements SearchQueryHighlightListener, EntryContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPanel.class);

    private final ClipBoardManager clipBoardManager;
    private final DialogService dialogService;
    private final KeyBindingRepository keyBindingRepository;

    private Optional<BasePanel> basePanel = Optional.empty();

    private boolean fixedLayout;
    private Optional<Layout> layout = Optional.empty();
    /**
     * The entry currently shown
     */
    private Optional<BibEntry> bibEntry = Optional.empty();

    /**
     * If a database is set, the preview will attempt to resolve strings in the previewed entry using that database.
     */
    private Optional<BibDatabaseContext> databaseContext = Optional.empty();
    private WebView previewView;
    private Optional<Future<?>> citationStyleFuture = Optional.empty();

    /**
     * @param panel           (may be null) Only set this if the preview is associated to the main window.
     * @param databaseContext (may be null) Used for resolving pdf directories for links.
     */
    public PreviewPanel(BasePanel panel, BibDatabaseContext databaseContext) {
        this.databaseContext = Optional.ofNullable(databaseContext);
        this.basePanel = Optional.ofNullable(panel);
        this.clipBoardManager = new ClipBoardManager();
        this.dialogService = new FXDialogService();
        this.keyBindingRepository = Globals.getKeyPrefs();

        DefaultTaskExecutor.runInJavaFXThread(() -> {
            // Set up scroll pane for preview pane
            setFitToHeight(true);
            setFitToWidth(true);
            previewView = new WebView();
            setContent(previewView);
            previewView.setContextMenuEnabled(false);
            setContextMenu(createPopupMenu());

            if (this.basePanel.isPresent()) {
                // Handler for drag content of preview to different window
                // only created for main window (not for windows like the search results dialog)
                setOnDragDetected(event -> {
                            Dragboard dragboard = startDragAndDrop(TransferMode.COPY);
                            ClipboardContent content = new ClipboardContent();
                            content.putHtml((String) previewView.getEngine().executeScript("window.getSelection().toString()"));
                            dragboard.setContent(content);

                            event.consume();
                        }
                );
            }
            createKeyBindings();
            updateLayout();
        });
    }

    private void createKeyBindings() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = Globals.getKeyPrefs().mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case COPY_PREVIEW:
                        copyPreviewToClipBoard();
                        event.consume();
                        break;
                    case CLOSE_DIALOG:
                        close();
                        event.consume();
                        break;
                    default:
                }
            }
        });
    }

    private ContextMenu createPopupMenu() {
        MenuItem copyPreview = new MenuItem(Localization.lang("Copy preview"), IconTheme.JabRefIcon.COPY.getGraphicNode());
        copyPreview.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.COPY_PREVIEW));
        copyPreview.setOnAction(event -> copyPreviewToClipBoard());
        MenuItem printEntryPreview = new MenuItem(Localization.lang("Print entry preview"), IconTheme.JabRefIcon.PRINTED.getGraphicNode());
        printEntryPreview.setOnAction(event -> print());
        MenuItem previousPreviewLayout = new MenuItem(Localization.menuTitleFX("Previous preview layout"));
        previousPreviewLayout.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.PREVIOUS_PREVIEW_LAYOUT));
        previousPreviewLayout.setOnAction(event -> basePanel.ifPresent(BasePanel::previousPreviewStyle));
        MenuItem nextPreviewLayout = new MenuItem(Localization.menuTitleFX("Next preview layout"));
        nextPreviewLayout.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.NEXT_PREVIEW_LAYOUT));
        nextPreviewLayout.setOnAction(event -> basePanel.ifPresent(BasePanel::nextPreviewStyle));

        ContextMenu menu = new ContextMenu();
        menu.getItems().add(copyPreview);
        menu.getItems().add(printEntryPreview);
        menu.getItems().add(new SeparatorMenuItem());
        menu.getItems().add(nextPreviewLayout);
        menu.getItems().add(previousPreviewLayout);
        return menu;
    }

    public void setDatabaseContext(BibDatabaseContext databaseContext) {
        this.databaseContext = Optional.ofNullable(databaseContext);
    }

    public Optional<BasePanel> getBasePanel() {
        return this.basePanel;
    }

    public void setBasePanel(BasePanel basePanel) {
        this.basePanel = Optional.ofNullable(basePanel);
    }

    public void updateLayout(PreviewPreferences previewPreferences) {
        if (fixedLayout) {
            LOGGER.debug("cannot change the layout because the layout is fixed");
            return;
        }

        String style = previewPreferences.getPreviewCycle().get(previewPreferences.getPreviewCyclePosition());

        if (CitationStyle.isCitationStyleFile(style)) {
            if (basePanel.isPresent()) {
                layout = Optional.empty();
                CitationStyle.createCitationStyleFromFile(style)
                        .ifPresent(citationStyle -> {
                    basePanel.get().getCitationStyleCache().setCitationStyle(citationStyle);
                    basePanel.get().output(Localization.lang("Preview style changed to: %0", citationStyle.getTitle()));
                        });
            }
        } else {
            updatePreviewLayout(previewPreferences.getPreviewStyle());
            basePanel.ifPresent(panel -> panel.output(Localization.lang("Preview style changed to: %0", Localization.lang("Preview"))));
        }

        update();
    }

    public void updateLayout() {
        updateLayout(Globals.prefs.getPreviewPreferences());
    }

    private void updatePreviewLayout(String layoutFile) {
        StringReader sr = new StringReader(layoutFile.replace("__NEWLINE__", "\n"));
        try {
            layout = Optional.of(
                    new LayoutHelper(sr, Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader))
                            .getLayoutFromText());
        } catch (IOException e) {
            layout = Optional.empty();
            LOGGER.debug("no layout could be set", e);
        }
    }

    public void setLayout(Layout layout) {
        this.layout = Optional.ofNullable(layout);
        update();
    }

    public void setEntry(BibEntry newEntry) {
        bibEntry.filter(e -> e != newEntry).ifPresent(e -> e.unregisterListener(this));
        bibEntry = Optional.ofNullable(newEntry);
        newEntry.registerListener(this);

        update();
    }

    /**
     * Listener for ChangedFieldEvent.
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void listen(FieldChangedEvent fieldChangedEvent) {
        update();
    }

    @Override
    public BibEntry getEntry() {
        return this.bibEntry.orElse(null);
    }

    public void update() {
        ExporterFactory.entryNumber = 1; // Set entry number in case that is included in the preview layout.

        if (citationStyleFuture.isPresent()) {
            citationStyleFuture.get().cancel(true);
            citationStyleFuture = Optional.empty();
        }

        if (layout.isPresent()) {
            StringBuilder sb = new StringBuilder();
            bibEntry.ifPresent(entry -> sb.append(layout.get()
                    .doLayout(entry, databaseContext.map(BibDatabaseContext::getDatabase).orElse(null))));
            setPreviewLabel(sb.toString());
        } else if (basePanel.isPresent() && bibEntry.isPresent()) {
            Future<?> citationStyleWorker = BackgroundTask
                    .wrap(() -> basePanel.get().getCitationStyleCache().getCitationFor(bibEntry.get()))
                    .onRunning(() -> {
                        CitationStyle citationStyle = basePanel.get().getCitationStyleCache().getCitationStyle();
                        setPreviewLabel("<i>" + Localization.lang("Processing %0", Localization.lang("Citation Style")) +
                                ": " + citationStyle.getTitle() + " ..." + "</i>");
                    })
                    .onSuccess(this::setPreviewLabel)
                    .onFailure(exception -> {
                        LOGGER.error("Error while generating citation style", exception);
                        setPreviewLabel(Localization.lang("Error while generating citation style"));
                    })
                    .executeWith(Globals.TASK_EXECUTOR);
            this.citationStyleFuture = Optional.of(citationStyleWorker);
        }
    }

    private void setPreviewLabel(String text) {
        DefaultTaskExecutor.runInJavaFXThread(() -> {
            previewView.getEngine().loadContent(text);
            this.setHvalue(0);
        });
    }

    @Override
    public void highlightPattern(Optional<Pattern> newPattern) {
        // TODO: Implement that search phrases are highlighted
        update();
    }

    /**
     * this fixes the Layout, the user cannot change it anymore. Useful for testing the styles in the settings
     *
     * @param layout should be either a {@link String} (for the old PreviewStyle) or a {@link CitationStyle}.
     */
    public void setFixedLayout(String layout) {
        this.fixedLayout = true;
        updatePreviewLayout(layout);
    }

    public void print() {
        PrinterJob job = PrinterJob.createPrinterJob();
        boolean proceed = dialogService.showPrintDialog(job);
        if (!proceed) {
            return;
        }

        BackgroundTask.wrap(() -> {
            job.getJobSettings().setJobName(bibEntry.flatMap(BibEntry::getCiteKeyOptional).orElse("NO ENTRY"));
            previewView.getEngine().print(job);
            job.endJob();
            return null;
        })
                .onFailure(exception -> dialogService.showErrorDialogAndWait(Localization.lang("Could not print preview"), exception))
                .executeWith(Globals.TASK_EXECUTOR);
    }

    public void close() {
        basePanel.ifPresent(BasePanel::hideBottomComponent);
    }

    private void copyPreviewToClipBoard() {
        String previewContent = (String) previewView.getEngine().executeScript("document.documentElement.outerHTML");
        clipBoardManager.setClipboardContents(previewContent);
    }
}
