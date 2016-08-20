package net.sf.jabref.gui.fieldeditors;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.model.entry.FieldName;

public class FieldNameLabel extends JLabel {

    public FieldNameLabel(String name) {
        super(FieldNameLabel.getFieldNameLabelText(name), SwingConstants.LEFT);

        setVerticalAlignment(SwingConstants.TOP);
        setForeground(GUIGlobals.ENTRY_EDITOR_LABEL_COLOR);
        setBorder(BorderFactory.createEmptyBorder());
    }

    @Override
    public void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);
        super.paintComponent(g2);
    }

    private static String getFieldNameLabelText(String fieldName) {
        return ' ' + FieldName.getDisplayName(fieldName) + ' ';
    }


}
