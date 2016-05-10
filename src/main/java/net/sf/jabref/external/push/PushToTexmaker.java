/*  Copyright (C) 2015 JabRef contributors.
    Copyright (C) 2015 Oscar Gustafsson.
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
 * Class for pushing entries into TexMaker.
 */
public class PushToTexmaker extends AbstractPushToApplication implements PushToApplication {

    @Override
    public String getApplicationName() {
        return "Texmaker";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("texmaker");
    }

    @Override
    protected String[] getCommandLine(String keyString) {
        return new String[] {commandPath, "-insert", getCiteCommand() + "{" + keyString + "}"};
    }

    @Override
    protected void initParameters() {
        commandPathPreferenceKey = JabRefPreferences.TEXMAKER_PATH;
    }

}
