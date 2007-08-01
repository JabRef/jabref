package net.sf.jabref;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

public class FieldNameLabel extends JLabel {

  public FieldNameLabel(String name) {
    super(name, JLabel.LEFT);
      setVerticalAlignment(NORTH);
    //setFont(GUIGlobals.fieldNameFont);
    setForeground(GUIGlobals.validFieldColor);
      setBorder(BorderFactory.createMatteBorder(1,0,0,0, Color.GRAY));
    //setBorder(BorderFactory.createEtchedBorder());
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
