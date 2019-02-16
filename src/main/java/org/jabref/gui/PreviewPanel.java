package org.jabref.gui;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.print.PrinterJob;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.web.WebView;

import org.jabref.Globals;
import org.jabref.gui.externalfiles.ExternalFilesEntryLinker;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.search.SearchQueryHighlightListener;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.preferences.PreviewPreferences;

import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Displays an BibEntry using the given layout format.
 */
public class PreviewPanel extends ScrollPane implements SearchQueryHighlightListener, EntryContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPanel.class);

    private final ClipBoardManager clipBoardManager;
    private final DialogService dialogService;
    private final KeyBindingRepository keyBindingRepository;

    private final String defaultPreviewStyle = "Preview";
    private String previewStyle;
    private CitationStyle citationStyle;
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
    private BibDatabaseContext databaseContext;
    private final WebView previewView;
    private Optional<Future<?>> citationStyleFuture = Optional.empty();

    private final ExternalFilesEntryLinker fileLinker;

    /**
     * @param panel           (may be null) Only set this if the preview is associated to the main window.
     * @param databaseContext Used for resolving pdf directories for links. Must not be null.
     */
    public PreviewPanel(BasePanel panel, BibDatabaseContext databaseContext, KeyBindingRepository keyBindingRepository, PreviewPreferences preferences, DialogService dialogService, ExternalFileTypes externalFileTypes) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.basePanel = Optional.ofNullable(panel);
        this.dialogService = dialogService;
        this.clipBoardManager = Globals.clipboardManager;
        this.keyBindingRepository = keyBindingRepository;

        fileLinker = new ExternalFilesEntryLinker(externalFileTypes, Globals.prefs.getFilePreferences(), databaseContext);

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
                startFullDrag();

                Dragboard dragboard = startDragAndDrop(TransferMode.COPY);
                ClipboardContent content = new ClipboardContent();
                content.putHtml((String) previewView.getEngine().executeScript("window.getSelection().toString()"));
                dragboard.setContent(content);

                event.consume();
            });
        }
        this.previewView.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY, TransferMode.MOVE, TransferMode.LINK);
            }
            event.consume();
        });

        this.previewView.setOnDragDropped(event -> {
            BibEntry entry = this.getEntry();
            boolean success = false;
            if (event.getDragboard().hasContent(DataFormat.FILES)) {
                List<Path> files = event.getDragboard().getFiles().stream().map(File::toPath).collect(Collectors.toList());

                if (event.getTransferMode() == TransferMode.MOVE) {
                    LOGGER.debug("Mode MOVE"); //shift on win or no modifier
                    fileLinker.moveFilesToFileDirAndAddToEntry(entry, files);
                }
                if (event.getTransferMode() == TransferMode.LINK) {
                    LOGGER.debug("Node LINK"); //alt on win
                    fileLinker.addFilesToEntry(entry, files);
                }
                if (event.getTransferMode() == TransferMode.COPY) {
                    LOGGER.debug("Mode Copy"); //ctrl on win, no modifier on Xubuntu
                    fileLinker.copyFilesToFileDirAndAddToEntry(entry, files);
                }
                success = true;
            }

            event.setDropCompleted(success);
            event.consume();

        });

        createKeyBindings();
        updateLayout(preferences, true);
    }

    private void createKeyBindings() {
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            Optional<KeyBinding> keyBinding = keyBindingRepository.mapToKeyBinding(event);
            if (keyBinding.isPresent()) {
                switch (keyBinding.get()) {
                    case COPY_PREVIEW:
                        copyPreviewToClipBoard();
                        event.consume();
                        break;
                    case CLOSE:
                        close();
                        event.consume();
                        break;
                    default:
                }
            }
        });
    }

    private ContextMenu createPopupMenu() {
        MenuItem copyPreview = new MenuItem(Localization.lang("Copy preview"), IconTheme.JabRefIcons.COPY.getGraphicNode());
        copyPreview.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.COPY_PREVIEW));
        copyPreview.setOnAction(event -> copyPreviewToClipBoard());
        MenuItem printEntryPreview = new MenuItem(Localization.lang("Print entry preview"), IconTheme.JabRefIcons.PRINTED.getGraphicNode());
        printEntryPreview.setOnAction(event -> print());
        MenuItem previousPreviewLayout = new MenuItem(Localization.lang("Previous preview layout"));
        previousPreviewLayout.setAccelerator(keyBindingRepository.getKeyCombination(KeyBinding.PREVIOUS_PREVIEW_LAYOUT));
        previousPreviewLayout.setOnAction(event -> basePanel.ifPresent(BasePanel::previousPreviewStyle));
        MenuItem nextPreviewLayout = new MenuItem(Localization.lang("Next preview layout"));
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
        this.databaseContext = databaseContext;
    }

    public Optional<BasePanel> getBasePanel() {
        return this.basePanel;
    }

    public void setBasePanel(BasePanel basePanel) {
        this.basePanel = Optional.ofNullable(basePanel);
    }

    public void updateLayout(PreviewPreferences previewPreferences) {
        updateLayout(previewPreferences, false);
    }

    private void updateLayout(PreviewPreferences previewPreferences, boolean init) {
        if (fixedLayout) {
            LOGGER.debug("cannot change the layout because the layout is fixed");
            return;
        }

        String style = previewPreferences.getCurrentPreviewStyle();
        if (previewStyle == null) {
            previewStyle = style;
            CitationStyle.createCitationStyleFromFile(style).ifPresent(cs -> citationStyle = cs);
        }
        if (basePanel.isPresent() && !previewStyle.equals(style)) {
            if (CitationStyle.isCitationStyleFile(style)) {
                layout = Optional.empty();
                CitationStyle.createCitationStyleFromFile(style)
                             .ifPresent(cs -> {
                                 citationStyle = cs;
                                 if (!init) {
                                     basePanel.get().output(Localization.lang("Preview style changed to: %0", citationStyle.getTitle()));
                                 }
                             });
                previewStyle = style;
            }
        } else {
            previewStyle = defaultPreviewStyle;
            updatePreviewLayout(previewPreferences.getPreviewStyle(), previewPreferences.getLayoutFormatterPreferences());
            if (!init) {
                basePanel.get().output(Localization.lang("Preview style changed to: %0", Localization.lang("Preview")));
            }
        }

        update();
    }

    private void updatePreviewLayout(String layoutFile, LayoutFormatterPreferences layoutFormatterPreferences) {
        StringReader sr = new StringReader(layoutFile.replace("__NEWLINE__", "\n"));
        try {
            layout = Optional.of(new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText());
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
                                                        .doLayout(entry, databaseContext.getDatabase())));
            setPreviewLabel(sb.toString());
        } else if (basePanel.isPresent() && bibEntry.isPresent()) {
            if ((citationStyle != null) && !previewStyle.equals(defaultPreviewStyle)) {
                basePanel.get().getCitationStyleCache().setCitationStyle(citationStyle);
            }
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
        updatePreviewLayout(layout, Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader));
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
        basePanel.ifPresent(BasePanel::closeBottomPane);
    }

    private void copyPreviewToClipBoard() {
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
}
