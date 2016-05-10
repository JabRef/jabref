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

    private static final String UNKOWN_VERSION = "*unknown*";

    public final static String OS = System.getProperty("os.name", UNKOWN_VERSION).toLowerCase();
    public final static String OS_VERSION = System.getProperty("os.version", UNKOWN_VERSION).toLowerCase();
    public final static String OS_ARCH = System.getProperty("os.arch", UNKOWN_VERSION).toLowerCase();
    public final static String JAVA_VERSION = System.getProperty("java.version", UNKOWN_VERSION).toLowerCase();

    private final String version;
    private final String authors;
    private final String developers;
    private final String year;

    public BuildInfo() {
        this("/build.properties");
    }

    public BuildInfo(String path) {
        Properties properties = new Properties();

        try (InputStream stream = getClass().getResourceAsStream(path)) {
            if(stream != null) {
                properties.load(stream);
            }
        } catch (IOException ignored) {
            // nothing to do -> default already set
        }

        version = properties.getProperty("version", UNKOWN_VERSION);
        authors = properties.getProperty("authors", "");
        year = properties.getProperty("year", "");
        developers = properties.getProperty("developers", "");

    }

    public String getVersion() {
        return version;
    }

    public String getAuthors() {
        return authors;
    }

    public String getDevelopers() {
        return developers;
    }

    public String getYear() {
        return year;
    }

}
