package net.sf.jabref;

import javax.swing.JLabel;
import javax.swing.Icon;
import java.awt.*;
import javax.swing.BorderFactory;

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
