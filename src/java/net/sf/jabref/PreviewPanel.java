package net.sf.jabref;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.io.StringReader;

import javax.swing.JEditorPane;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import net.sf.jabref.export.layout.Layout;
import net.sf.jabref.export.layout.LayoutHelper;

/**
 * Displays an BibtexEntry using the given layout format.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public class PreviewPanel extends JEditorPane {

	public String CONTENT_TYPE = "text/html";

	BibtexEntry entry;

	MetaData metaData;

	/**
	 * If a database is set, the preview will attempt to resolve strings in the
	 * previewed entry using that database.
	 */
	BibtexDatabase database;

	Layout layout;

	String layoutFile;

	JScrollPane sp;

	public PreviewPanel(BibtexDatabase db, MetaData metaData, String layoutFile) {
		this(metaData, layoutFile);
		this.database = db;
	}

	public PreviewPanel(BibtexDatabase database, MetaData metaData, BibtexEntry entry, String layoutFile) {
		this(metaData, layoutFile);
		this.database = database;
		this.entry = entry;

		try {
			readLayout();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		update();
	}

	public PreviewPanel(BibtexEntry entry, MetaData metaData, String layoutFile) {
		this(null, metaData, entry, layoutFile);
	}

	/**
	 * Emtpy Preview Panel constructor
	 * 
	 * @param metaData
	 * @param layoutFile
	 */
	public PreviewPanel(MetaData metaData, String layoutFile) {
		this.metaData = metaData;
		this.layoutFile = layoutFile;
		
		sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setBorder(null);

		setEditable(false);
		setContentType(CONTENT_TYPE);
		addHyperlinkListener(new HyperlinkListener() {
			public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
				if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
					try {
						String address = hyperlinkEvent.getURL().toString();
						Util.openExternalViewer(PreviewPanel.this.metaData, address, "url");
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		});
	}

	public void setDatabase(BibtexDatabase db) {
		database = db;
	}

	public JScrollPane getPane() {
		return sp;
	}

	public void readLayout(String layoutFormat) throws Exception {
		layoutFile = layoutFormat;
		readLayout();
	}

	public void readLayout() throws Exception {
		StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
		layout = new LayoutHelper(sr).getLayoutFromText(Globals.FORMATTER_PACKAGE);
	}

	public void setEntry(BibtexEntry newEntry) {
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
		sb.append(layout.doLayout(entry, database));
		setText(sb.toString());
		invalidate();
		revalidate();

		// Scroll to top:
		final JScrollBar bar = sp.getVerticalScrollBar();
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				bar.setValue(0);
			}
		});
	}

	public boolean hasEntry() {
		return (entry != null);
	}

	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		Object hint = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		super.paintComponent(g2);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, hint);
	}
}
