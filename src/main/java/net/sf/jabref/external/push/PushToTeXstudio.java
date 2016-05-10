/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.external.push;

import javax.swing.Icon;

import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.IconTheme;

/**
 * Created by IntelliJ IDEA. User: alver Date: Jan 14, 2006 Time: 4:55:23 PM To change this template use File | Settings
 * | File Templates.
 */
public class PushToTeXstudio extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "TeXstudio";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("texstudio");
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "--insert-cite", String.format("%s{%s}", getCiteCommand(), keyString)};
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.TEXSTUDIO_PATH;
    }
}
