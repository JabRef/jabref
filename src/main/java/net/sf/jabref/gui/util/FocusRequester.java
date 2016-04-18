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
package net.sf.jabref.gui.util;

import java.awt.Component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FocusRequester implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(FocusRequester.class);

    private final Component comp;

    public FocusRequester(Component comp) {
        if (comp == null) {
            Thread.dumpStack();
        }

        this.comp = comp;

        run();
    }

    @Override
    public void run() {
        LOGGER.debug("requesting focus for " + comp);
        comp.requestFocus();
    }
}
