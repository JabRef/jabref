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

public interface FieldEditor {

    public String getFieldName();
    public javax.swing.JComponent getPane();
    // Returns the component to be added to a container. Might
    // be a JScrollPane or the component itself.
    public javax.swing.JComponent getTextComponent();
    // Returns the text component itself.
    public javax.swing.JLabel getLabel();
    public void setLabelColor(java.awt.Color c);
    public void setBackground(java.awt.Color c);
    public String getText();
    public void setText(String newText);
    public void append(String text);
    public java.awt.Container getParent();
    public void requestFocus();
    public void setEnabled(boolean enabled);
    // paste text into component, it should also take some selected text into account
    public void paste(String textToInsert) ;

    // normally implemented in JTextArea and JTextField
    public String getSelectedText() ;
}
