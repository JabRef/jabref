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
package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.JobName;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.export.ExportFormats;
import net.sf.jabref.gui.PreviewPanelTransferHandler;

/**
 * Displays an BibtexEntry using the given layout format.
 */
public class PreviewPanel extends JPanel implements VetoableChangeListener, SearchTextListener, EntryContainer {

	/**
	 * The bibtex entry currently shown
	 */
	BibtexEntry entry;

	MetaData metaData;

	/**
	 * If a database is set, the preview will attempt to resolve strings in the
	 * previewed entry using that database.
	 */
	BibtexDatabase database;

	Layout layout;

	String layoutFile;

	public JEditorPane previewPane;

	JScrollPane scrollPane;
	PdfPreviewPanel pdfPreviewPanel;

	BasePanel panel;

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
    public PreviewPanel(BibtexDatabase database, BibtexEntry entry,
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
	public PreviewPanel(BibtexDatabase database, BibtexEntry entry,
		BasePanel panel, MetaData metaData, String layoutFile, boolean withPDFPreview) {
		this(panel, metaData, layoutFile, withPDFPreview);
		this.database = database;
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
		
		this.panel = panel;
		this.metaData = metaData;
		this.layoutFile = layoutFile;
		this.previewPane = createPreviewPane();
		if (withPDFPreview) {
			this.pdfPreviewPanel = new PdfPreviewPanel(metaData);
		} else {
			this.pdfPreviewPanel = null;
		}
		if (panel != null) {
			// dropped files handler only created for main window
			// not for Windows as like the search results window
			this.previewPane.setTransferHandler(new PreviewPanelTransferHandler(panel.frame(), this, this.previewPane.getTransferHandler()));
		}

		// Set up scroll pane for preview pane
		scrollPane = new JScrollPane(previewPane,
			JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);
		
		/*
		 * If we have been given a panel and the preference option
		 * previewPrintButton is set, show the tool bar
		 */
		if (panel != null
			&& JabRefPreferences.getInstance().getBoolean("previewPrintButton")) {
			add(createToolBar(), BorderLayout.LINE_START);
		}

		if (withPDFPreview) {
			JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
					scrollPane, pdfPreviewPanel);
			splitPane.setOneTouchExpandable(true);
			
			// int oneThird = panel.getWidth()/3;
			int oneThird = 400; // arbitrarily set as panel.getWidth() always
								// returns 0 at this point
			splitPane.setDividerLocation(oneThird*2);

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
	
	class PrintAction extends AbstractAction {

		public PrintAction() {
			super(Globals.lang("Print Preview"), GUIGlobals.getImage("psSmall"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Print Preview"));
		}

		//DocumentPrinter printerService;

		public void actionPerformed(ActionEvent arg0) {

			// Background this, as it takes a while.
			new Thread() {
				public void run() {
					try {
						PrintRequestAttributeSet pras = new HashPrintRequestAttributeSet();
						pras.add(new JobName(entry.getCiteKey(), null));
						previewPane.print(null, null, true, null, pras, false);

					} catch (PrinterException e) {

						// Inform the user... we don't know what to do.
						JOptionPane.showMessageDialog(PreviewPanel.this,
							Globals.lang("Could not print preview") + ".\n"
								+ e.getMessage(), Globals
								.lang("Printing Entry Preview"),
							JOptionPane.ERROR_MESSAGE);
					}
				}
			}.start();
		}
	}

	Action printAction;

	public Action getPrintAction() {
		if (printAction == null)
			printAction = new PrintAction();
		return printAction;
	}

	class CloseAction extends AbstractAction {
		public CloseAction() {
			super(Globals.lang("Close window"), GUIGlobals.getImage("close"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Close window"));
		}

		public void actionPerformed(ActionEvent e) {
			panel.hideBottomComponent();
		}
	}

	Action closeAction;

	private ArrayList<String> wordsToHighlight = null;

	public Action getCloseAction() {
		if (closeAction == null)
			closeAction = new CloseAction();
		return closeAction;
	}

	JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(getPrintAction());
		if (panel != null) {
			menu.add(panel.frame.switchPreview);
		}
		return menu;
	}

	JToolBar createToolBar() {

		JToolBar tlb = new JToolBar(JToolBar.VERTICAL);
		JabRefPreferences prefs = JabRefPreferences.getInstance();
		Action printAction = getPrintAction();
		Action closeAction = getCloseAction();

		tlb.setMargin(new Insets(0, 0, 0, 2));

		// The toolbar carries all the key bindings that are valid for the whole
		// window.
		ActionMap am = tlb.getActionMap();
		InputMap im = tlb.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

		im.put(prefs.getKey("Close entry preview"), "close");
		am.put("close", closeAction);

		im.put(prefs.getKey("Print entry preview"), "print");
		am.put("print", printAction);

		tlb.setFloatable(false);

		// Add actions (and thus buttons)
		tlb.add(closeAction);

		tlb.addSeparator();

		tlb.add(printAction);

		Component[] comps = tlb.getComponents();

		for (int i = 0; i < comps.length; i++)
			((JComponent) comps[i]).setOpaque(false);

		return tlb;
	}

	JEditorPane createPreviewPane() {
		JEditorPane previewPane = new JEditorPane() {
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
			public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
				if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					try {
						String address = hyperlinkEvent.getURL().toString();
						Util.openExternalViewer(PreviewPanel.this.metaData,
							address, "url");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
		
		return previewPane;
	}

	public void setDatabase(BibtexDatabase db) {
		database = db;
	}

    public void setMetaData(MetaData metaData) {
        this.metaData = metaData;
    }

    public void readLayout(String layoutFormat) throws Exception {
		layoutFile = layoutFormat;
		readLayout();
	}

	public void readLayout() throws Exception {
		StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__",
			"\n"));
		layout = new LayoutHelper(sr)
			.getLayoutFromText(Globals.FORMATTER_PACKAGE);
	}

    public void setLayout(Layout layout) {
        this.layout = layout;
    }

    public void setEntry(BibtexEntry newEntry) {
		if (newEntry != entry) {
			if (entry != null)
				entry.removePropertyChangeListener(this);
			newEntry.addPropertyChangeListener(this);
		}
		entry = newEntry;
		try {
			readLayout();
			update();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
    
    public BibtexEntry getEntry() {
    	return this.entry;
    }

	public void update() {

		StringBuffer sb = new StringBuffer();
        ExportFormats.entryNumber = 1; // Set entry number in case that is included in the preview layout.
		if (entry != null) {
			sb.append(layout.doLayout(entry, database, wordsToHighlight));
		}
		previewPane.setText(sb.toString());
		previewPane.revalidate();

		// Scroll to top:
		final JScrollBar bar = scrollPane.getVerticalScrollBar();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bar.setValue(0);
			}
		});
		
		// update pdf preview
		if (pdfPreviewPanel != null) {
			pdfPreviewPanel.updatePanel(entry);
		}
	}


	public boolean hasEntry() {
		return (entry != null);
	}

	/**
	 * The PreviewPanel has registered itself as an event listener with the
	 * currently displayed BibtexEntry. If the entry changes, an event is
	 * received here, and we can update the preview immediately.
	 */
	public void vetoableChange(PropertyChangeEvent evt)
		throws PropertyVetoException {
		// TODO updating here is not really necessary isn't it?
		// Only if we are visible.
		update();
	}

	@Override
	public void searchText(ArrayList<String> words) {
		if (Globals.prefs.getBoolean("highLightWords")) {
			this.wordsToHighlight = words;
			update();
		} else {
			if (this.wordsToHighlight != null) {
				// setting of "highLightWords" seems to have changed.
				// clear all highlights and remember the clearing (by wordsToHighlight = null)
				this.wordsToHighlight = null;
				update();
			}
		}
	}
}
