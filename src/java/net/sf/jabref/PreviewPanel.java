package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import java.beans.VetoableChangeListener;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;
import net.sf.jabref.util.DocumentPrinter;

/**
 * Displays an BibtexEntry using the given layout format.
 * 
 * @author $Author$
 * @version $Revision$ ($Date: 2007-08-01 20:23:38 +0200 (Mi, 01 Aug
 *          2007) $)
 * 
 */
public class PreviewPanel extends JPanel implements VetoableChangeListener {

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

	BasePanel panel;

	/**
	 * 
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
		this(panel, metaData, layoutFile);
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
		super(new BorderLayout(), true);

		this.panel = panel;
		this.metaData = metaData;
		this.layoutFile = layoutFile;
		this.previewPane = createPreviewPane();

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

		add(scrollPane, BorderLayout.CENTER);
	}

	class PrintAction extends AbstractAction {

		public PrintAction() {
			super(Globals.lang("Print Preview"), GUIGlobals.getImage("psSmall"));
			putValue(SHORT_DESCRIPTION, Globals.lang("Print Preview"));
		}

		DocumentPrinter printerService;

		public void actionPerformed(ActionEvent arg0) {
			if (printerService == null)
				printerService = new DocumentPrinter();

			// Background this, as it takes a while.
			new Thread() {
				public void run() {
					try {
						printerService.print(entry.getCiteKey(), previewPane);
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

	public Action getCloseAction() {
		if (closeAction == null)
			closeAction = new CloseAction();
		return closeAction;
	}

	JPopupMenu createPopupMenu() {
		JPopupMenu menu = new JPopupMenu();
		menu.add(getPrintAction());

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

			public void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D) g;
				Object hint = g2
					.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_RENDERING,
					RenderingHints.VALUE_RENDER_QUALITY);
				super.paintComponent(g2);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
			}
		};
		previewPane.setMargin(new Insets(3, 3, 3, 3));

		previewPane.setComponentPopupMenu(createPopupMenu());
		
		previewPane.setEditable(false);
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

	public void update() {

		StringBuffer sb = new StringBuffer();
		if (entry != null)
			sb.append(layout.doLayout(entry, database));
		previewPane.setText(sb.toString());
		previewPane.invalidate();
		previewPane.revalidate();

		// Scroll to top:
		final JScrollBar bar = scrollPane.getVerticalScrollBar();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bar.setValue(0);
			}
		});
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
}
