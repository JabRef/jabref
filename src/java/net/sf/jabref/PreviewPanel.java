package net.sf.jabref;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.border.Border;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import net.sf.jabref.export.layout.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.Graphics2D;

public class PreviewPanel extends JEditorPane {

  public String CONTENT_TYPE = "text/html";
      //LAYOUT_FILE = "simplehtml";
  BibtexEntry entry;
  BibtexDatabase database = null;
    // If a database is set, the preview will attempt to resolve strings in the previewed
    // entry using that database.

  Layout layout;
  String prefix = "", postfix = "";
  Dimension DIM = new Dimension(650, 110);
  HashMap layouts = new HashMap();
  String layoutFile;
  JScrollPane sp;

    public PreviewPanel(BibtexDatabase db, String layoutFile) {
        this(layoutFile);
        this.database = db;
    }

    public PreviewPanel(BibtexDatabase db, BibtexEntry be, String layoutFile) {
        this(be, layoutFile);
        this.database = db;
    }

  public PreviewPanel(BibtexEntry be, String layoutFile) {
    entry = be;
    sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    sp.setBorder(null);
    //Util.pr(layoutFile);
    init();
    this.layoutFile = layoutFile;

    try {
      readLayout();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    update();

  }

  public PreviewPanel(String layoutFile) {
    sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                         JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    this.layoutFile = layoutFile;
    sp.setBorder(null);

    init();
    //setText("<HTML></HTML>");
  }

   public void setDatabase(BibtexDatabase db) {
       database = db;
   }

  private void init() {
    setEditable(false);
    setContentType(CONTENT_TYPE);
      addHyperlinkListener(new HyperlinkListener () {
          public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
              if (hyperlinkEvent.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    String address = hyperlinkEvent.getURL().toString(); 
                      Util.openExternalViewer(address, "url", Globals.prefs);
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }
          }
      });
    //setSize(100, 100);
  }

  public JScrollPane getPane() {
    return sp;
  }

  public void readLayout(String layoutFormat) throws Exception {
    layoutFile = layoutFormat;
    readLayout();
  }

  public void readLayout() throws Exception {
    LayoutHelper layoutHelper = null;
    StringReader sr = new StringReader(layoutFile.replaceAll("__NEWLINE__", "\n"));
    layoutHelper = new LayoutHelper(sr);
    layout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);

      /*String entryType = entry.getType().getName().toLowerCase();
      if (layouts.get(entryType) != null) {
	  layout = (Layout)layouts.get(entryType);
	  return;
      }*/


      //URL reso = JabRefFrame.class.getResource
      //  (Globals.LAYOUT_PREFIX+layoutFile+"."+entryType+".layout");

      //Util.pr(Globals.LAYOUT_PREFIX+LAYOUT_FILE+"."+entryType+".layout");



      /*try {
        if (reso == null)
          reso = JabRefFrame.class.getResource(Globals.LAYOUT_PREFIX+layoutFile+".layout");
        layoutHelper = new LayoutHelper(new InputStreamReader(reso.openStream()));
      }
      catch (IOException ex) {
      }*/



      //layouts.put(entryType, layout);

    /*reso = JabRefFrame.class.getResource
        (Globals.LAYOUT_PREFIX+layoutFile+".begin.layout");
    StringWriter stw = new StringWriter();
    InputStreamReader reader;
    int c;
    if (reso != null) {
      reader = new InputStreamReader(reso.openStream());
      while ((c = reader.read()) != -1) {
        stw.write((char)c);
      }
      reader.close();
    }
    prefix = stw.toString();

    reso = JabRefFrame.class.getResource
        (Globals.LAYOUT_PREFIX+layoutFile+".end.layout");
    stw = new StringWriter();
    if (reso != null) {
      reader = new InputStreamReader(reso.openStream());
      while ((c = reader.read()) != -1) {
        stw.write((char)c);
      }
      reader.close();
    }
    postfix = stw.toString();
*/
  }

  public void setEntry(BibtexEntry newEntry) {
    //Util.pr("en");
    entry = newEntry;
    try {
      readLayout();
      update();
    }
    catch (Exception ex) {
        ex.printStackTrace();
    }
    
  }

  public void update() {

    //StringBuffer sb = new StringBuffer(prefix);
    StringBuffer sb = new StringBuffer();
    sb.append(layout.doLayout(entry, database));
    //sb.append(postfix);
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


    //Util.pr(sb.toString());
    //revalidate();

    //Util.pr(""+getPreferredSize()+"\t"+getMinimumSize());


  }

  public boolean hasEntry() {
    return (entry != null);
  }

  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

/*  public Dimension getPreferredSize() {
    Util.pr(""+super.getPreferredSize());
    return super.getPreferredSize();
  }*/
  /*public Dimension getMinimumSize() { return DIM; }*/

  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
    super.paintComponent(g2);
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
  }

}
