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

import javax.swing.SwingUtilities;
import java.awt.Component;

public class FocusRequester implements Runnable {
    private Component comp;

    public FocusRequester(Component comp) {
       if (comp == null)
               Thread.dumpStack();

        //System.out.println("FocusRequester: "+comp.toString());
	this.comp = comp;
	try {
	    SwingUtilities.invokeLater(this);
	} catch(Exception e) {
	    e.printStackTrace();
	}
    }
    public void run() {

    comp.requestFocus();
    }
}
