package net.sf.jabref.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefExecutorService;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.fieldeditors.PreviewPanelTransferHandler;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.worker.CitationStyleWorker;
import net.sf.jabref.logic.citationstyle.CitationStyle;
import net.sf.jabref.logic.exporter.ExportFormats;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.search.SearchQueryHighlightListener;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.event.FieldChangedEvent;
import net.sf.jabref.preferences.PreviewPreferences;

import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Displays an BibEntry using the given layout format.
 */
public class PreviewPanel extends JPanel implements SearchQueryHighlightListener, EntryContainer {

    private static final Log LOGGER = LogFactory.getLog(PreviewPanel.class);

    /**
     * The bibtex entry currently shown
     */
    private Optional<BibEntry> bibEntry = Optional.empty();

    /**
     * If a database is set, the preview will attempt to resolve strings in the
     * previewed entry using that database.
     */
    private Optional<BibDatabaseContext> databaseContext = Optional.empty();

    private Optional<BasePanel> basePanel = Optional.empty();

    private boolean fixedLayout;
    private Optional<Layout> layout = Optional.empty();
    private JEditorPaneWithHighlighting previewPane;

    private final JScrollPane scrollPane;

    private final PrintAction printAction = new PrintAction();
    private final CloseAction closeAction = new CloseAction();
    private final CopyPreviewAction copyPreviewAction = new CopyPreviewAction();

    private Optional<Pattern> highlightPattern = Optional.empty();
    private Optional<CitationStyleWorker> citationStyleWorker = Optional.empty();

    /**
     * @param databaseContext
     *            (may be null) Optionally used to resolve strings and for resolving pdf directories for links.
     * @param entry
     *            (may be null) If given this entry is shown otherwise you have
     *            to call setEntry to make something visible.
     * @param panel
     *            (may be null) If not given no toolbar is shown on the right
     *            hand side.
     */
    public PreviewPanel(BibDatabaseContext databaseContext, BibEntry entry, BasePanel panel) {
        this(panel, databaseContext);
        setEntry(entry);
    }

    /**
     *
     * @param panel
     *            (may be null) If not given no toolbar is shown on the right
     *            hand side.
     * @param databaseContext
     *            (may be null) Used for resolving pdf directories for links.
     */
    public PreviewPanel(BasePanel panel, BibDatabaseContext databaseContext) {
        super(new BorderLayout(), true);

        this.databaseContext = Optional.ofNullable(databaseContext);
        this.basePanel = Optional.ofNullable(panel);

        createPreviewPane();

        if (this.basePanel.isPresent()) {
            // dropped files handler only created for main window
            // not for Windows as like the search results window
            this.previewPane.setTransferHandler(new PreviewPanelTransferHandler(panel.frame(), this, this.previewPane.getTransferHandler()));
        }

        // Set up scroll pane for preview pane
        scrollPane = new JScrollPane(previewPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        add(scrollPane, BorderLayout.CENTER);

        this.createKeyBindings();
        updateLayout();
    }

    private void createKeyBindings(){
        ActionMap actionMap = this.getActionMap();
        InputMap inputMap = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        final String close = "close";
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), close);
        actionMap.put(close, this.closeAction);

        final String copy = "copy";
        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.COPY_PREVIEW), copy);
        actionMap.put(copy, this.copyPreviewAction);
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(this.printAction);
        menu.add(this.copyPreviewAction);
        this.basePanel.ifPresent(p -> menu.add(p.frame().getNextPreviewStyleAction()));
        this.basePanel.ifPresent(p -> menu.add(p.frame().getPreviousPreviewStyleAction()));
        return menu;
    }

    private void createPreviewPane() {
        previewPane = new JEditorPaneWithHighlighting() {
            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return getPreferredSize();
            }

        };
        previewPane.setMargin(new Insets(3, 3, 3, 3));

        previewPane.setComponentPopupMenu(createPopupMenu());

        previewPane.setEditable(false);
        previewPane.setDragEnabled(true); // this has an effect only, if no custom transfer handler is registered. We keep the statement if the transfer handler is removed.
        previewPane.setContentType("text/html");
        previewPane.addHyperlinkListener(hyperlinkEvent -> {
            if ((hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) && PreviewPanel.this.databaseContext
                    .isPresent()) {
                try {
                    String address = hyperlinkEvent.getURL().toString();
                    JabRefDesktop.openExternalViewer(PreviewPanel.this.databaseContext.get(), address, FieldName.URL);
                } catch (IOException e) {
                    LOGGER.warn("Could not open external viewer", e);
                }
            }
        });

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

    public void updateLayout() {
        if (fixedLayout) {
            LOGGER.debug("cannot change the layout because the layout is fixed");
            return;
        }

        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
        String style = previewPreferences.getPreviewCycle().get(previewPreferences.getPreviewCyclePosition());

        if (CitationStyle.isCitationStyleFile(style)) {
            if (basePanel.isPresent()) {
                layout = Optional.empty();
                CitationStyle citationStyle = CitationStyle.createCitationStyleFromFile(style);
                if (citationStyle != null) {
                    basePanel.get().getCitationStyleCache().setCitationStyle(citationStyle);
                    basePanel.get().output(Localization.lang("Preview style changed to: %0", citationStyle.getTitle()));
                }
            }
        } else {
            updatePreviewLayout(previewPreferences.getPreviewStyle());
            if (basePanel.isPresent()) {
                basePanel.get().output(Localization.lang("Preview style changed to: %0", Localization.lang("Preview")));
            }
        }

        update();
    }

    private void updatePreviewLayout(String layoutFile){
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
    }

    public void setEntry(BibEntry newEntry) {

        bibEntry.filter(e -> e != newEntry).ifPresent(e -> e.unregisterListener(this));
        bibEntry = Optional.ofNullable(newEntry);
        bibEntry.ifPresent(e -> e.registerListener(this));

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
        ExportFormats.entryNumber = 1; // Set entry number in case that is included in the preview layout.

        if (citationStyleWorker.isPresent()){
            citationStyleWorker.get().cancel(true);
            citationStyleWorker = Optional.empty();
        }

        if (layout.isPresent()){
            StringBuilder sb = new StringBuilder();
            bibEntry.ifPresent(entry -> sb.append(layout.get()
                    .doLayout(entry, databaseContext.map(BibDatabaseContext::getDatabase).orElse(null))));
            setPreviewLabel(sb.toString());
            markHighlights();
        }
        else if (basePanel.isPresent()){
            citationStyleWorker = Optional.of(new CitationStyleWorker(this, previewPane));
            citationStyleWorker.get().execute();
        }

    }

    public void markHighlights() {
        previewPane.highlightPattern(highlightPattern);
    }

    public void setPreviewLabel(String text) {
        if (SwingUtilities.isEventDispatchThread()) {
            previewPane.setText(text);
            previewPane.revalidate();
        } else {
            SwingUtilities.invokeLater(() -> {
                previewPane.setText(text);
                previewPane.revalidate();
            });
        }
        this.scrollToTop();
    }

    private void scrollToTop() {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    @Override
    public void highlightPattern(Optional<Pattern> newPattern) {
        this.highlightPattern = newPattern;
        update();
    }

    public Optional<Pattern> getHighlightPattern() {
        return highlightPattern;
    }

    /**
     * this fixes the Layout, the user cannot change it anymore. Useful for testing the styles in the settings
     * @param parameter should be either a {@link String} (for the old PreviewStyle) or a {@link CitationStyle}.
     */
    public PreviewPanel setFixedLayout(Object parameter) {
        this.fixedLayout = true;

        if (parameter instanceof String) {
            updatePreviewLayout((String) parameter);
        } else if (parameter instanceof CitationStyle) {
            layout = Optional.empty();
            if (basePanel.isPresent()){
                basePanel.get().getCitationStyleCache().setCitationStyle((CitationStyle) parameter);
            }
        } else {
            LOGGER.error("unknown style type");
        }
        update();
        return this;
    }

    class PrintAction extends AbstractAction {
        public PrintAction() {
            super(Localization.lang("Print entry preview"), IconTheme.JabRefIcon.PRINTED.getIcon());

            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Print entry preview"));
        }


        @Override
        public void actionPerformed(ActionEvent arg0) {

            // Background this, as it takes a while.
            JabRefExecutorService.INSTANCE.execute(() -> {
                try {
                    PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
                    pras.add(new JobName(bibEntry.flatMap(BibEntry::getCiteKeyOptional).orElse("NO ENTRY"), null));
                    previewPane.print(null, null, true, null, pras, false);

                } catch (PrinterException e) {
                    // Inform the user... we don't know what to do.
                    JOptionPane.showMessageDialog(PreviewPanel.this,
                            Localization.lang("Could not print preview") + ".\n" + e.getMessage(),
                            Localization.lang("Print entry preview"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.info("Could not print preview", e);
                }
            });
        }
    }

    public void close() {
        basePanel.ifPresent(BasePanel::hideBottomComponent);
    }

    class CloseAction extends AbstractAction {

        public CloseAction() {
            super(Localization.lang("Close window"), IconTheme.JabRefIcon.CLOSE.getSmallIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close window"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            close();
        }
    }

    class CopyPreviewAction extends AbstractAction {

        public CopyPreviewAction() {
            super(Localization.lang("Copy preview"), IconTheme.JabRefIcon.COPY.getSmallIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Copy preview"));
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.COPY_PREVIEW));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            previewPane.selectAll();
            previewPane.copy();
            previewPane.select(0, -1);
        }
    }

    public PrintAction getPrintAction() {
        return printAction;
    }


}
