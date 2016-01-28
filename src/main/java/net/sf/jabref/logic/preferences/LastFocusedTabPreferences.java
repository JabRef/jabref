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

package net.sf.jabref.logic.preferences;

import java.io.File;
import java.util.Objects;

import net.sf.jabref.JabRefPreferences;

public class LastFocusedTabPreferences {

    private final JabRefPreferences preferences;

    public LastFocusedTabPreferences(JabRefPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    public void setLastFocusedTab(File file) {
        if (file == null) {
            return; // no change detected
        }

        String filePath = file.getAbsolutePath();
        preferences.put(JabRefPreferences.LAST_FOCUSED, filePath);
    }

    public boolean hadLastFocus(File file) {
        if (file == null) {
            return false;
        }

        String lastFocusedDatabase = preferences.get(JabRefPreferences.LAST_FOCUSED);
        return file.getAbsolutePath().equals(lastFocusedDatabase);
    }
}
