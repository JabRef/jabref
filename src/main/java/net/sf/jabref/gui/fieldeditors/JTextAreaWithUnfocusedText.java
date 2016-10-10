package net.sf.jabref.gui.fieldeditors;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextArea;
import javax.swing.UIManager;

import net.sf.jabref.gui.util.component.JTextFieldWithUnfocusedText;

/**
 * A text area which displays a predefined text the same way as {@link JTextFieldWithUnfocusedText} does.
 */
public class JTextAreaWithUnfocusedText extends JTextArea implements FocusListener {

    private final String textWhenNotFocused;

    public JTextAreaWithUnfocusedText() {
        this("");
    }

    public JTextAreaWithUnfocusedText(String content, String textWhenNotFocused) {
        this(textWhenNotFocused);
        setText(content);
    }

    public JTextAreaWithUnfocusedText(String textWhenNotFocused) {
        super();
        this.setEditable(true);
        this.setText("");
        this.textWhenNotFocused = textWhenNotFocused;
        this.addFocusListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (!this.hasFocus() && this.getText().isEmpty()) {
            Font prev = g.getFont();
            Color prevColor = g.getColor();
            g.setColor(UIManager.getColor("textInactiveText"));
            int h = g.getFontMetrics().getHeight();
            int x = this.getInsets().left;
            Graphics2D g2d = (Graphics2D) g;
            RenderingHints hints = g2d.getRenderingHints();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.drawString(textWhenNotFocused, x, h + this.getInsets().top);
            g2d.setRenderingHints(hints);
            g.setFont(prev);
            g.setColor(prevColor);
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        this.repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        this.repaint();
    }
}
