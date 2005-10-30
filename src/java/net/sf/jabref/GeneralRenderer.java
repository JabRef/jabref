package net.sf.jabref;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: May 30, 2005
 * Time: 9:43:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class GeneralRenderer /*extends JTable implements TableCellRenderer {*/ extends DefaultTableCellRenderer {
    private boolean antialiasing;

    public GeneralRenderer(Color c, boolean antialiasing) {
        super();
        this.antialiasing = antialiasing;
        setBackground(c);
    }


    public GeneralRenderer(Color c, Color fg, boolean antialiasing) {
        this(c, antialiasing);
        setForeground(fg);
    }

    public void firePropertyChange(String propertyName, boolean old, boolean newV) {}
    public void firePropertyChange(String propertyName, Object old, Object newV) {}

    /* For enabling the renderer to handle icons. */
    protected void setValue(Object value) {
        //System.out.println(""+value);
        if (value instanceof Icon) {
            setIcon((Icon)value);
            setText(null);
            //super.setValue(null);
        } else if (value instanceof JLabel) {
          JLabel lab = (JLabel)value;
          setIcon(lab.getIcon());
          //table.setToolTipText(lab.getToolTipText());
          setToolTipText(lab.getToolTipText());
          if (lab.getIcon() != null)
            setText(null);
        } else {

            setIcon(null);
            //table.setToolTipText(null);
            setToolTipText(null);
            if (value != null)
                setText(value.toString());
            else
                setText(null);
        }
    }

      public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        //System.out.println(antialiasing);
        if (antialiasing) {
            RenderingHints rh = g2.getRenderingHints();
            rh.put(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            rh.put(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHints(rh);
        }
          super.paint(g2);

    }

}
