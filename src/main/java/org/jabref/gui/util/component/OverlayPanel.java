package org.jabref.gui.util.component;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.OverlayLayout;
import javax.swing.ScrollPaneConstants;

/**
 * Supports an underlying text for JComponent
 */
public class OverlayPanel extends JPanel {

    private final JLabel label;


    public OverlayPanel(JComponent overlay, String text) {
        OverlayLayout layout = new OverlayLayout(this);
        this.setLayout(layout);

        label = new JLabel(text);
        label.setFont(new Font(label.getFont().getName(), Font.ITALIC, 18));
        label.setForeground(new Color(224, 220, 220));
        label.setLocation(0, 0);

        JScrollPane scroller = new JScrollPane(overlay);
        scroller.setLocation(0, 0);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        add(label);
        add(scroller);
    }

    @Override
    public void paint(Graphics g) {
        int len = label.getWidth();

        Dimension dim = this.getSize();
        if ((dim.height > 25) && (dim.width > (len + 10))) {
            int x = (dim.width - len) / 2;
            int y = dim.height / 2;

            label.setLocation(x, y);
        }

        super.paint(g);
    }

}
