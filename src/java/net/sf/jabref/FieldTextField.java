/*
Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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

import javax.swing.*;
import java.awt.Color;

public class FieldTextField extends JTextField implements FieldEditor {

    protected String fieldName;
    protected JLabel label;

    public FieldTextField(String fieldName_, String content) {
	super(content);
	fieldName = fieldName_;
	label = new JLabel(Util.nCase(fieldName), JLabel.CENTER);
	label.setBorder(BorderFactory.createEtchedBorder());
	setBackground(GUIGlobals.validFieldBackground);
	label.setBorder(BorderFactory.createEtchedBorder
			 (Color.white, Color.gray));	
	label.setOpaque(true);
	label.setBackground(Color.white);
	if ((content != null) && (content.length() > 0))
	    label.setForeground(GUIGlobals.validFieldColor);
	// At construction time, the field can never have an invalid value.
	else
	    label.setForeground(GUIGlobals.nullFieldColor);
    }

    public String getFieldName() { return fieldName; }
    public JLabel getLabel() { return label; }
    public void setLabelColor(Color c) { label.setForeground(c); }
    public JComponent getPane() { return this; }


}
