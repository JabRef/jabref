package org.jabref.gui.util.component;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import javax.swing.JTextField;
import javax.swing.UIManager;

/**
 * A text field which displays a predefined text (e.g. "Search") if the text field is empty.
 * This is similar to a html5 input element with a defined placeholder attribute.
 * Implementation based on https://gmigdos.wordpress.com/2010/03/30/java-a-custom-jtextfield-for-searching/
 */
public class JTextFieldWithPlaceholder extends JTextField implements KeyListener {

    private final String textWhenNotFocused;

    /**
     * Additionally to {@link JTextFieldWithPlaceholder#JTextFieldWithPlaceholder(String)}
     * this also sets the initial text of the text field component.
     *
     * @param content as the text of the textfield
     * @param placeholder as the placeholder of the textfield
     */
    public JTextFieldWithPlaceholder(String content, String placeholder) {
        this(placeholder);
        setText(content);
    }

    /**
     * This will create a {@link JTextField} with a placeholder text. The placeholder
     * will always be displayed if the text of the {@link JTextField} is empty.
     *
     * @param placeholder as the placeholder of the textfield
     */
    public JTextFieldWithPlaceholder(String placeholder) {
        super();
        this.setEditable(true);
        this.setText("");
        this.textWhenNotFocused = placeholder;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);

        if (this.getText().isEmpty()) {
            int height = this.getHeight();
            Font prev = graphics.getFont();
            Color prevColor = graphics.getColor();
            graphics.setColor(UIManager.getColor("textInactiveText"));
            int textHeight = graphics.getFontMetrics().getHeight();
            int textBottom = (((height - textHeight) / 2) + textHeight) - 4;
            int x = this.getInsets().left;
            Graphics2D g2d = (Graphics2D) graphics;
            RenderingHints hints = g2d.getRenderingHints();
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.drawString(textWhenNotFocused, x, textBottom);
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
