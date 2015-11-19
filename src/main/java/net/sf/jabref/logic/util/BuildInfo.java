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
package net.sf.jabref.logic.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BuildInfo {

    private final String version;
    private final String UNKOWN_VERSION = "*unknown*";


    public BuildInfo() {
        this("/build.properties");
    }

    public BuildInfo(String path) {
        Properties properties = new Properties();

        String versionFromProps = UNKOWN_VERSION;

        try (InputStream stream = getClass().getResourceAsStream(path)) {
            properties.load(stream);
            versionFromProps = properties.getProperty("version", UNKOWN_VERSION);
        } catch (IOException | NullPointerException ignored) {
            // nothing to do -> default already set
        }

        version = versionFromProps;
    }

    public String getVersion() {
        return version;
    }
}
