package net.sf.jabref;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.util.HashMap;
import net.sf.jabref.export.layout.*;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.Graphics2D;

public class PreviewPanel extends JEditorPane {

  public String CONTENT_TYPE = "text/html",
      LAYOUT_FILE = "simplehtml";
  BibtexEntry entry;
  Layout layout;
  String prefix = "", postfix = "";
  Dimension DIM = new Dimension(650, 110);
    HashMap layouts = new HashMap();

  public PreviewPanel(BibtexEntry be) {
    entry = be;
    setEditable(false);
    setContentType(CONTENT_TYPE);
    try {
      readLayout();
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    update();
  }

  public void readLayout() throws Exception {
      String entryType = entry.getType().getName().toLowerCase();
      if (layouts.get(entryType) != null) {
	  layout = (Layout)layouts.get(entryType);
	  return;
      }

      LayoutHelper layoutHelper = null;
      URL reso = JabRefFrame.class.getResource
	  (Globals.LAYOUT_PREFIX+LAYOUT_FILE+"."+entryType+".layout");
      //Util.pr(Globals.LAYOUT_PREFIX+LAYOUT_FILE+"."+entryType+".layout");
      try {
	  if (reso == null)
	      reso = JabRefFrame.class.getResource(Globals.LAYOUT_PREFIX+LAYOUT_FILE+".layout");

	  layoutHelper = new LayoutHelper(new InputStreamReader(reso.openStream()));
      }
      catch (IOException ex) {
      }
      layout = layoutHelper.getLayoutFromText(Globals.FORMATTER_PACKAGE);

      layouts.put(entryType, layout);

    reso = JabRefFrame.class.getResource
        (Globals.LAYOUT_PREFIX+LAYOUT_FILE+".begin.layout");
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
        (Globals.LAYOUT_PREFIX+LAYOUT_FILE+".end.layout");
    stw = new StringWriter();
    if (reso != null) {
      reader = new InputStreamReader(reso.openStream());
      while ((c = reader.read()) != -1) {
        stw.write((char)c);
      }
      reader.close();
    }
    postfix = stw.toString();

  }

  public void setEntry(BibtexEntry newEntry) {
    entry = newEntry;
    try {
      readLayout();
    }
    catch (Exception ex) {
    }
    update();
  }

  public void update() {
    //StringBuffer sb = new StringBuffer(prefix);
    StringBuffer sb = new StringBuffer();
    sb.append(layout.doLayout(entry));
    //sb.append(postfix);
    setText(sb.toString());
    //Util.pr(sb.toString());
  }

  public Dimension getPreferredSize() { return DIM; }
  public Dimension getMinimumSize() { return DIM; }

  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
    super.paintComponent(g2);
  }

}
