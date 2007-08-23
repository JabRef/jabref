package net.sf.jabref;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Renderer for table cells, which supports both Icons, JLabels and plain text.
 */
public class GeneralRenderer /*extends JTable implements TableCellRenderer {*/ extends DefaultTableCellRenderer {

    public GeneralRenderer(Color c) {
        super();
        setBackground(c);
    }


    public GeneralRenderer(Color c, Color fg) {
        this(c);
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

    /*  public void paint(Graphics g) {
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

    }*/

}
