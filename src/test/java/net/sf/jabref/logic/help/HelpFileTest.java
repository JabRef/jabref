/*
 * Copyright (C) 2003-2016 JabRef contributors.
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.logic.help;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import net.sf.jabref.logic.help.HelpFile;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;

public class HelpFileTest {
    private final String jabrefHelp = "http://help.jabref.org/en/";
    @Test
    public void referToValidPage() throws IOException {
        for (HelpFile help : HelpFile.values()) {
            URL url = new URL(jabrefHelp + help.getPageName());
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64)");
            assertEquals(200, http.getResponseCode());
        }
    }
}
