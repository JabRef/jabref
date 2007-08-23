/*
 Copyright (C) 2003 Nizar N. Batada, Morten O. Alver

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html

 */
package net.sf.jabref;

import java.awt.*;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

/**
 * An implementation of the FieldEditor backed by a JTextArea. Used for
 * multi-line input.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 * 
 */
public class FieldTextArea extends JTextArea implements FieldEditor {

	Dimension PREFERRED_SIZE;

	JScrollPane sp;

	FieldNameLabel label;

	String fieldName;

	final static Pattern bull = Pattern.compile("\\s*[-\\*]+.*");

	final static Pattern indent = Pattern.compile("\\s+.*");

	final boolean antialias = Globals.prefs.getBoolean("antialias");

	public FieldTextArea(String fieldName_, String content) {
		super(content);

        updateFont();
                
		// Add the global focus listener, so a menu item can see if this field
		// was focused when an action was called.
		addFocusListener(Globals.focusListener);
		addFocusListener(new FieldEditorFocusListener());
		sp = new JScrollPane(this, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
			JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sp.setMinimumSize(new Dimension(200, 1));

		setLineWrap(true);
		setWrapStyleWord(true);
		fieldName = fieldName_;

		label = new FieldNameLabel(" " + Util.nCase(fieldName) + " ");
		setBackground(GUIGlobals.validFieldBackground);

		FieldTextMenu popMenu = new FieldTextMenu(this);
		this.addMouseListener(popMenu);
		label.addMouseListener(popMenu);
	}

	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	/*public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		if (antialias)
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		super.paint(g2);
	}*/

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String newName) {
		fieldName = newName;
	}

	public JLabel getLabel() {
		return label;
	}

	public void setLabelColor(Color c) {
		label.setForeground(c);
	}

	public JComponent getPane() {
		return sp;
	}

	public JComponent getTextComponent() {
		return this;
	}


    public void updateFont() {
        setFont(GUIGlobals.CURRENTFONT);
    }

    public void paste(String textToInsert) {
		int sel = getSelectionEnd() - getSelectionStart();
		if (sel > 0) // selected text available
			replaceSelection(textToInsert);
		else {
			int cPos = this.getCaretPosition();
			this.insert(textToInsert, cPos);
		}
	}

	// public void keyPressed(KeyEvent event) {
	// int keyCode = event.getKeyCode();
	// if (keyCode == KeyEvent.VK_ENTER) {
	// // Consume; we will handle this ourselves:
	// event.consume();
	// autoWrap();
	//
	// }
	//
	// }
	//
	// private void autoWrap() {
	// int pos = getCaretPosition();
	// int posAfter = pos + 1;
	// StringBuffer sb = new StringBuffer(getText());
	// // First insert the line break:
	// sb.insert(pos, '\n');
	//
	// // We want to investigate the beginning of the last line:
	// // int end = sb.length();
	//
	// // System.out.println("."+sb.substring(0, pos)+".");
	//
	// // Find 0 or the last line break before our current position:
	// int idx = sb.substring(0, pos).lastIndexOf("\n") + 1;
	// String prevLine = sb.substring(idx, pos);
	// if (bull.matcher(prevLine).matches()) {
	// int id = findFirstNonWhitespace(prevLine);
	// if (id >= 0) {
	// sb.insert(posAfter, prevLine.substring(0, id));
	// posAfter += id;
	// }
	// } else if (indent.matcher(prevLine).matches()) {
	// int id = findFirstNonWhitespace(prevLine);
	// if (id >= 0) {
	// sb.insert(posAfter, prevLine.substring(0, id));
	// posAfter += id;
	// }
	// }
	// /*
	// * if (prevLine.startsWith(" ")) { sb.insert(posAfter, " "); posAfter++; }
	// */
	//
	// setText(sb.toString());
	// setCaretPosition(posAfter);
	// }
	//
	// private int findFirstNonWhitespace(String s) {
	// for (int i = 0; i < s.length(); i++) {
	// if (!Character.isWhitespace(s.charAt(i)))
	// return i;
	// }
	// return -1;
	// }
	//
	// public void keyReleased(KeyEvent event) {
	//
	// }
	//
	// public void keyTyped(KeyEvent event) {
	// }
}
