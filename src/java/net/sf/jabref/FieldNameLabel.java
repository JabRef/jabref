package net.sf.jabref;

import javax.swing.JLabel;
import javax.swing.Icon;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Graphics;
import javax.swing.BorderFactory;

public class FieldNameLabel extends JLabel {

  public FieldNameLabel(String name) {
    super(name, JLabel.CENTER);
    //setFont(GUIGlobals.fieldNameFont);
    setForeground(GUIGlobals.validFieldColor);
    setBorder(BorderFactory.createEtchedBorder());
  }

  public void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D)g;
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                        RenderingHints.VALUE_RENDER_QUALITY);
    super.paintComponent(g2);
  }

}
