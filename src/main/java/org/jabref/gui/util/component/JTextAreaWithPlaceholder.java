package org.jabref.gui.util.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextArea;
import javax.swing.UIManager;

/**
 * A text area which displays a predefined text the same way as {@link JTextFieldWithPlaceholder} does.
 */
public class JTextAreaWithPlaceholder extends JTextArea implements KeyListener {

    private final String textWhenNotFocused;

    public JTextAreaWithPlaceholder() {
        this("");
    }

    /**
     * Additionally to {@link JTextAreaWithPlaceholder#JTextAreaWithPlaceholder(String)}
     * this also sets the initial text of the text field component.
     *
     * @param content as the text of the textfield
     * @param placeholder as the placeholder of the textfield
     */
    public JTextAreaWithPlaceholder(String content, String placeholder) {
        this(placeholder);
        setText(content);
    }

    /**
     * This will create a {@link JTextArea} with a placeholder text. The placeholder
     * will always be displayed if the text of the {@link JTextArea} is empty.
     *
     * @param placeholder as the placeholder of the textarea
     */
    public JTextAreaWithPlaceholder(String placeholder) {
        super();
        this.setEditable(true);
        this.setText("");
        this.textWhenNotFocused = placeholder;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (this.getText().isEmpty()) {
            Font prev = graphics.getFont();
            Color prevColor = graphics.getColor();
            graphics.setColor(UIManager.getColor("textInactiveText"));
            int textHeight = graphics.getFontMetrics().getHeight();
            int x = this.getInsets().left;
            Graphics2D g2d = (Graphics2D) graphics;
            RenderingHints hints = g2d.getRenderingHints();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.drawString(textWhenNotFocused, x, textHeight + this.getInsets().top);
            g2d.setRenderingHints(hints);
            graphics.setFont(prev);
            graphics.setColor(prevColor);
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (this.getText().isEmpty()) {
            this.repaint();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (this.getText().isEmpty()) {
            this.repaint();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (this.getText().isEmpty()) {
            this.repaint();
        }
    }
}
