/*  Copyright (C) 2003-2012 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.*;
import net.sf.jabref.exporter.layout.Layout;
import net.sf.jabref.exporter.layout.LayoutHelper;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.gui.fieldeditors.PreviewPanelTransferHandler;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.search.SearchQueryHighlightListener;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Displays an BibEntry using the given layout format.
 */
public class PreviewPanel extends JPanel implements VetoableChangeListener, SearchQueryHighlightListener, EntryContainer {

    private static final Log LOGGER = LogFactory.getLog(PreviewPanel.class);

    /**
     * The bibtex entry currently shown
     */
    private Optional<BibEntry> entry = Optional.empty();

    /**
     * If a database is set, the preview will attempt to resolve strings in the
     * previewed entry using that database.
     */
    private Optional<BibDatabase> database = Optional.empty();

    private Optional<Layout> layout = Optional.empty();

    /**
     * must not be null, must always be set during constructor, but can change over time
     */
    private MetaData metaData;

    /**
     * must not be null, must always be set during constructor, but can change over time
     */
    private String layoutFile;

    private final Optional<PdfPreviewPanel> pdfPreviewPanel;
    private final Optional<BasePanel> panel;

    private JEditorPane previewPane;

    private final JScrollPane scrollPane;

    private final PrintAction printAction;

    private final CloseAction closeAction;

    private Optional<Pattern> highlightPattern = Optional.empty();

    /**
     * @param database
     *            (may be null) Optionally used to resolve strings.
     * @param entry
     *            (may be null) If given this entry is shown otherwise you have
     *            to call setEntry to make something visible.
     * @param panel
     *            (may be null) If not given no toolbar is shown on the right
     *            hand side.
     * @param metaData
     *            (must be given) Used for resolving pdf directories for links.
     * @param layoutFile
     *            (must be given) Used for layout
     */
    public PreviewPanel(BibDatabase database, BibEntry entry,
            BasePanel panel, MetaData metaData, String layoutFile) {
        this(database, entry, panel, metaData, layoutFile, false);
    }


    /**
     * @param database
     *            (may be null) Optionally used to resolve strings.
     * @param entry
     *            (may be null) If given this entry is shown otherwise you have
     *            to call setEntry to make something visible.
     * @param panel
     *            (may be null) If not given no toolbar is shown on the right
     *            hand side.
     * @param metaData
     *            (must be given) Used for resolving pdf directories for links.
     * @param layoutFile
     *            (must be given) Used for layout
     * @param withPDFPreview if true, a PDF preview is included in the PreviewPanel
     */
    public PreviewPanel(BibDatabase database, BibEntry entry,
            BasePanel panel, MetaData metaData, String layoutFile, boolean withPDFPreview) {
        this(panel, metaData, layoutFile, withPDFPreview);
        this.database = Optional.ofNullable(database);
        setEntry(entry);
    }

    /**
     *
     * @param panel
     *            (may be null) If not given no toolbar is shown on the right
     *            hand side.
     * @param metaData
     *            (must be given) Used for resolving pdf directories for links.
     * @param layoutFile
     *            (must be given) Used for layout
     */
    public PreviewPanel(BasePanel panel, MetaData metaData, String layoutFile) {
        this(panel, metaData, layoutFile, false);
    }

    /**
     *
     * @param panel
     *            (may be null) If not given no toolbar is shown on the right
     *            hand side.
     * @param metaData
     *            (must be given) Used for resolving pdf directories for links.
     * @param layoutFile
     *            (must be given) Used for layout
     * @param withPDFPreview if true, a PDF preview is included in the PreviewPanel.
     * The user can override this setting by setting the config setting JabRefPreferences.PDF_PREVIEW to false.
     */
    private PreviewPanel(BasePanel panel, MetaData metaData, String layoutFile, boolean withPDFPreview) {
        super(new BorderLayout(), true);

        withPDFPreview = withPDFPreview && JabRefPreferences.getInstance().getBoolean(JabRefPreferences.PDF_PREVIEW);

        this.metaData = Objects.requireNonNull(metaData);
        this.layoutFile = Objects.requireNonNull(layoutFile);
        updateLayout();

        this.closeAction = new CloseAction();
        this.printAction = new PrintAction();

        this.panel = Optional.ofNullable(panel);

        createPreviewPane();
        if (withPDFPreview) {
            this.pdfPreviewPanel = Optional.of(new PdfPreviewPanel(metaData));
        } else {
            this.pdfPreviewPanel = Optional.empty();
        }
        if (panel != null) {
            // dropped files handler only created for main window
            // not for Windows as like the search results window
            this.previewPane.setTransferHandler(new PreviewPanelTransferHandler(panel.frame(), this, this.previewPane.getTransferHandler()));
        }

        // Set up scroll pane for preview pane
        scrollPane = new JScrollPane(previewPane,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);

        /*
         * If we have been given a panel and the preference option
         * previewPrintButton is set, show the tool bar
         */
        if (this.panel.isPresent()
                && JabRefPreferences.getInstance().getBoolean(JabRefPreferences.PREVIEW_PRINT_BUTTON)) {
            add(createToolBar(), BorderLayout.LINE_START);
        }

        if (withPDFPreview) {
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scrollPane, pdfPreviewPanel.orElse(null));
            splitPane.setOneTouchExpandable(true);

            int oneThird = 400; // arbitrarily set
            splitPane.setDividerLocation(oneThird * 2);

            // Provide minimum sizes for the two components in the split pane
            //			Dimension minimumSize = new Dimension(oneThird * 2, 50);
            //			scrollPane.setMinimumSize(minimumSize);
            //			minimumSize = new Dimension(oneThird, 50);
            //			pdfScrollPane.setMinimumSize(minimumSize);
            add(splitPane);
        } else {
            add(scrollPane, BorderLayout.CENTER);
        }
    }

    private JPopupMenu createPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        menu.add(this.printAction);
        this.panel.ifPresent(p -> menu.add(p.frame().switchPreview));
        return menu;
    }

    private JToolBar createToolBar() {
        JToolBar toolBar = new JToolBar(SwingConstants.VERTICAL);

        toolBar.setMargin(new Insets(0, 0, 0, 2));

        // The toolbar carries all the key bindings that are valid for the whole
        // window.
        ActionMap actionMap = toolBar.getActionMap();
        InputMap inputMap = toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        actionMap.put("close", this.closeAction);

        inputMap.put(Globals.getKeyPrefs().getKey(KeyBinding.PRINT_ENTRY_PREVIEW), "print");
        actionMap.put("print", this.printAction);

        toolBar.setFloatable(false);

        // Add actions (and thus buttons)
        toolBar.add(this.closeAction);
        toolBar.addSeparator();
        toolBar.add(this.printAction);

        Component[] comps = toolBar.getComponents();

        for (Component comp : comps) {
            ((JComponent) comp).setOpaque(false);
        }

        return toolBar;
    }

    private void createPreviewPane() {
        previewPane = new JEditorPane() {
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
        previewPane.addHyperlinkListener(new HyperlinkListener() {

            @Override
            public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
                if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    try {
                        String address = hyperlinkEvent.getURL().toString();
                        JabRefDesktop.openExternalViewer(PreviewPanel.this.metaData,
                                address, "url");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public void updateLayout(String layoutFormat) throws IOException {
        layoutFile = layoutFormat;
        updateLayout();
    }

    private void updateLayout() {
        StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
        try {
            layout = Optional.of(new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE));
        } catch (IOException e) {
            layout = Optional.empty();
            LOGGER.debug("no layout could be set", e);
        }
    }

    public void setLayout(Layout layout) {
        this.layout = Optional.of(layout);
    }

    public void setEntry(BibEntry newEntry) {
        if(entry.isPresent() && (entry.get() != newEntry)) {
            entry.ifPresent(e -> e.removePropertyChangeListener(this));
            newEntry.addPropertyChangeListener(this);
        }
        entry = Optional.ofNullable(newEntry);

        updateLayout();
        update();
    }

    @Override
    public BibEntry getEntry() {
        return this.entry.orElse(null);
    }

    public void update() {
        StringBuilder sb = new StringBuilder();
        ExportFormats.entryNumber = 1; // Set entry number in case that is included in the preview layout.
        entry.ifPresent(entry ->
                layout.ifPresent(layout ->
                        sb.append(layout.doLayout(entry, database.orElse(null), highlightPattern))
                )
        );
        String newValue = sb.toString();

        previewPane.setText(newValue);
        previewPane.revalidate();

        // Scroll to top:
        scrollToTop();

        // update pdf preview
        pdfPreviewPanel.ifPresent(p -> p.updatePanel(entry.orElse(null)));
    }

    private void scrollToTop() {
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));
    }

    /**
     * The PreviewPanel has registered itself as an event listener with the
     * currently displayed BibEntry. If the entry changes, an event is
     * received here, and we can update the preview immediately.
     */
    @Override
    public void vetoableChange(PropertyChangeEvent evt)
            throws PropertyVetoException {
        // TODO updating here is not really necessary isn't it?
        // Only if we are visible.
        update();
    }

    @Override
    public void highlightPattern(Optional<Pattern> highlightPattern) {
        this.highlightPattern = highlightPattern;
        update();
    }

    class PrintAction extends AbstractAction {
        public PrintAction() {
            super(Localization.lang("Print entry preview"), IconTheme.JabRefIcon.PRINTED.getIcon());

            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Print entry preview"));
            putValue(Action.ACCELERATOR_KEY, Globals.getKeyPrefs().getKey(KeyBinding.PRINT_ENTRY_PREVIEW));
        }


        @Override
        public void actionPerformed(ActionEvent arg0) {

            // Background this, as it takes a while.
            JabRefExecutorService.INSTANCE.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
                        pras.add(new JobName(entry.map(BibEntry::getCiteKey).orElse("NO ENTRY"), null));
                        previewPane.print(null, null, true, null, pras, false);

                    } catch (PrinterException e) {
                        // Inform the user... we don't know what to do.
                        JOptionPane.showMessageDialog(PreviewPanel.this,
                                Localization.lang("Could not print preview") + ".\n"
                                        + e.getMessage(),
                                Localization.lang("Print entry preview"),
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
        }

    }

    class CloseAction extends AbstractAction {

        public CloseAction() {
            super(Localization.lang("Close window"), IconTheme.JabRefIcon.CLOSE.getSmallIcon());
            putValue(Action.SHORT_DESCRIPTION, Localization.lang("Close window"));
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            panel.ifPresent(BasePanel::hideBottomComponent);
        }


    }

}
