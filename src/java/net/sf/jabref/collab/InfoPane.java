package net.sf.jabref.collab;

import javax.swing.JEditorPane;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class InfoPane extends JEditorPane {
  public InfoPane() {
    setEditable(false);
    setContentType("text/html");
  }
  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    super.paint(g2);
  }
}
