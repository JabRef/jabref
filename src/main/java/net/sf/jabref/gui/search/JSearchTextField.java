package net.sf.jabref.gui.search;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.UIManager;


/**
 * A text field which displays a predefined text (e.g. "Search") if it has not the focus and no text is entered.
 * Implementation based on https://gmigdos.wordpress.com/2010/03/30/java-a-custom-jtextfield-for-searching/
 */
public class JSearchTextField extends JTextField implements FocusListener {

    private String textWhenNotFocused;

	public JSearchTextField() {
		super();
		this.setEditable(true);
		this.setText("");
		this.textWhenNotFocused = "Search...";
		this.addFocusListener(this);
	}

	public String getTextWhenNotFocused() {
		return this.textWhenNotFocused;
	}

	public void setTextWhenNotFocused(String newText) {
		this.textWhenNotFocused = newText;
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		if (!this.hasFocus() && this.getText().isEmpty()) {
			int height = this.getHeight();
			Font prev = g.getFont();
			Color prevColor = g.getColor();
			g.setColor(UIManager.getColor("textInactiveText"));
			int h = g.getFontMetrics().getHeight();
			int textBottom = (((height - h) / 2) + h) - 4;
			int x = this.getInsets().left;
			Graphics2D g2d = (Graphics2D) g;
			RenderingHints hints = g2d.getRenderingHints();
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.drawString(textWhenNotFocused, x, textBottom);
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