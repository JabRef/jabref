/*
 Copyright (C) 2005-2015 Andreas Rudert, Oscar Gustafsson extracted from CustomImportList

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
  Copyright (C) 2005-2014 JabRef contributors.

*/

package net.sf.jabref.importer;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.List;

import net.sf.jabref.importer.fileformat.ImportFormat;

/**
 * Object with data for a custom importer.
 *
 * <p>Is also responsible for instantiating the class loader.</p>
 */
public class CustomImporter implements Comparable<CustomImporter> {

    private String name;
    private String cliId;
    private String className;
    private String basePath;


    public CustomImporter() {
        super();
    }

    public CustomImporter(List<String> data) {
        this(data.get(0), data.get(1), data.get(2), data.get(3));
    }

    public CustomImporter(String name, String cliId, String className, String basePath) {
        super();
        this.name = name;
        this.cliId = cliId;
        this.className = className;
        this.basePath = basePath;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClidId() {
        return this.cliId;
    }

    public void setCliId(String cliId) {
        this.cliId = cliId;
    }

    public String getClassName() {
        return this.className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public File getFileFromBasePath() {
        return new File(basePath);
    }

    public URL getBasePathUrl() throws MalformedURLException {
        return getFileFromBasePath().toURI().toURL();
    }

    public List<String> getAsStringList() {
        return Arrays.asList(name, cliId, className, basePath);
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof CustomImporter) && this.getName().equals(((CustomImporter) o).getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public int compareTo(CustomImporter o) {
        return this.getName().compareTo(o.getName());
    }

    @Override
    public String toString() {
        return this.name;
    }

    public ImportFormat getInstance() throws IOException, MalformedURLException, ClassNotFoundException,
            InstantiationException, IllegalAccessException {
        try (URLClassLoader cl = new URLClassLoader(new URL[] {getBasePathUrl()})) {
            Class<?> clazz = Class.forName(className, true, cl);
            ImportFormat importFormat = (ImportFormat) clazz.newInstance();
            importFormat.setIsCustomImporter(true);
            return importFormat;
        }
    }
}