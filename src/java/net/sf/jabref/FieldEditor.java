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

import net.sf.jabref.gui.AutoCompleteListener;

import java.awt.Color;
import java.awt.Container;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.UndoableEditListener;


/**
 * FieldEditors is a common interface between the FieldTextField and FieldTextArea.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public interface FieldEditor {

	public String getFieldName();

	/*
	 * Returns the component to be added to a container. Might be a JScrollPane
	 * or the component itself.
	 */
	public JComponent getPane();

	/*
	 * Returns the text component itself.
	 */
	public JComponent getTextComponent();

	public JLabel getLabel();

    public void setActiveBackgroundColor();

    public void setValidBackgroundColor();

    public void setInvalidBackgroundColor();

	public void setLabelColor(Color c);

	public void setBackground(Color c);

	public String getText();

	/**
	 * Sets the given text on the current field editor and marks this text
	 * editor as modified.
	 * 
	 * @param newText
	 */
	public void setText(String newText);

	public void append(String text);

	public Container getParent();

	public void requestFocus();

	public void setEnabled(boolean enabled);

    public void updateFont();
    /**
	 * paste text into component, it should also take some selected text into
	 * account
	 */
	public void paste(String textToInsert);

	/**
	 * normally implemented in JTextArea and JTextField
	 * 
	 * @return
	 */
	public String getSelectedText();


    public boolean hasUndoInformation();

    public void undo();

    public boolean hasRedoInformation();

    public void redo();

    public void addUndoableEditListener(UndoableEditListener listener);

    public void setAutoCompleteListener(AutoCompleteListener listener);

    public void clearAutoCompleteSuggestion();
}
