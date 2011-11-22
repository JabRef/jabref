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
package net.sf.jabref;


import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
/**
 * This class extends {@link AbstractAction} with the ability to set
 * the mnemonic key based on a '&' character inserted in front of
 * the desired mnemonic letter. This is done by setting the action's
 * name using putValue(NAME, actionname).
 * This facilitates localized mnemonics.
 */
public abstract class MnemonicAwareAction extends AbstractAction {

    public MnemonicAwareAction() {
	//super("");
    }

    public MnemonicAwareAction(ImageIcon icon) {
	//super(icon);
        
        putValue(SMALL_ICON, icon);
    }

    public void putValue(String key, Object value) {
	if (key.equals(Action.NAME)) {
	    String name = Globals.menuTitle(value.toString());
	    int i = name.indexOf('&');
	    if (i >= 0) {
		char mnemonic = Character.toUpperCase(name.charAt(i+1));
		putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
		value = name.substring(0, i) + name.substring(i+1);
	    } else
                value = name;
	}
	super.putValue(key, value);
    }
}
