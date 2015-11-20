/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.fieldeditors;

import javax.swing.*;

import net.sf.jabref.gui.GUIGlobals;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Focus listener that changes the color of the text area when it has focus.
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: 18.mar.2005
 * Time: 18:20:14
 * To change this template use File | Settings | File Templates.
 */
public class FieldEditorFocusListener implements FocusListener {

    @Override
    public void focusGained(FocusEvent event) {
        if (event.getSource() instanceof FieldEditor) {
            ((FieldEditor) event.getSource()).setActiveBackgroundColor();
        } else {
            ((JComponent) event.getSource()).setBackground(GUIGlobals.activeBackground);
        }
    }

    @Override
    public void focusLost(FocusEvent event) {
        if (event.getSource() instanceof FieldEditor) {
            ((FieldEditor) event.getSource()).setValidBackgroundColor();
        } else {
            ((JComponent) event.getSource()).setBackground(GUIGlobals.validFieldBackgroundColor);
        }
    }

}
