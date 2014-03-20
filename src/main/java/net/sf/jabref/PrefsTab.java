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

/**
 * A prefsTab is a component displayed in the PreferenceDialog.
 * 
 * It needs to extend from Component.
 * 
 * @author $Author$
 * @version $Revision$ ($Date$)
 *
 */
public interface PrefsTab {

    /**
     * This method is called when the dialog is opened, or if it is made
     * visible after being hidden. The tab should update all its values.
     *
     * This is the ONLY PLACE to set values for the fields in the tab. It
     * is ILLEGAL to set values only at construction time, because the dialog
     * will be reused and updated.
     */
    public void setValues();

    /**
     * This method is called when the user presses OK in the
     * Preferences dialog. Implementing classes must make sure all
     * settings presented get stored in JabRefPreferences.
     *
     */
    public void storeSettings();

    /**
     * This method is called before the {@ling storeSettings()} method, 
     * to check if there are illegal settings in the tab, or if is ready
     * to be closed.
     * If the tab is *not* ready, it should display a message to the user 
     * informing about the illegal setting.
     */
    public boolean readyToClose();

    /**
     * Should return the localized identifier to use for the tab.
     * 
     * @return Identifier for the tab (for instance "General", "Appearance" or "External Files").
     */
    public String getTabName();  
}
