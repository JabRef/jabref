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
package net.sf.jabref.plugin;

import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;

import javax.swing.*;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Nov 26, 2007
 * Time: 5:44:16 PM
 * To change this template use File | Settings | File Templates.
 */
public interface SidePanePlugin {

    public void init(JabRefFrame frame, SidePaneManager manager);
    
    public SidePaneComponent getSidePaneComponent();

    public JMenuItem getMenuItem();

    public String getShortcutKey();
}
