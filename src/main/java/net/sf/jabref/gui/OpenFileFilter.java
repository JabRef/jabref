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
package net.sf.jabref.gui;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashSet;

public class OpenFileFilter extends javax.swing.filechooser.FileFilter implements FilenameFilter {

    private final HashSet<String> extSet = new HashSet<String>();
    private final String desc;


    public OpenFileFilter(String[] extensions) {
        StringBuilder buf = new StringBuilder();
        int numExt = extensions.length;

        if (numExt > 0) {
            buf.append('*');
            buf.append(extensions[0]);

            extSet.add(extensions[0]);
        }

        for (int curExt = 1; curExt < numExt; curExt++) {
            buf.append(", *");
            buf.append(extensions[curExt]);

            extSet.add(extensions[curExt]);
        }

        desc = buf.toString();
    }

    public OpenFileFilter() {
        this(new String[] {
                ".bib",
                ".dat", // silverplatter ending
                ".txt", // windows puts ".txt" extentions and for scifinder
                ".ris",
                ".ref", // refer/endnote format
                ".fcgi", // default for pubmed
                ".bibx", // default for BibTeXML
                ".xml"
        });
    }

    public OpenFileFilter(String s) {
        this(s.split("[, ]+", 0));
    }

    @Override
    public boolean accept(File file) {
        if (file.isDirectory()) {
            return true;
        }

        return accept(file.getName());
    }

    public boolean accept(String filenm) {

        filenm = filenm.toLowerCase();
        int dotPos = filenm.lastIndexOf(".");

        if (dotPos == -1) {
            return false;
        }

        int dotDotPos = filenm.lastIndexOf(".", dotPos - 1); // for dot.dot extensions

        return extSet.contains(filenm.substring(dotPos)) ||
                dotDotPos >= 0 && extSet.contains(filenm.substring(dotDotPos));
    }

    public String getSuffix(String filenm) {

        int dotPos;
        String suffix;

        dotPos = filenm.lastIndexOf(".");
        if (dotPos == -1) {
            return null;
        }

        suffix = filenm.substring(dotPos);
        if (extSet.contains(suffix)) {
            return suffix;
        }

        dotPos = filenm.lastIndexOf(".", dotPos - 1); // for dot.dot extensions
        if (dotPos == -1) {
            return null;
        }

        suffix = filenm.substring(dotPos);
        if (extSet.contains(suffix)) {
            return suffix;
        }

        return null;
    }

    @Override
    public String getDescription() {
        return desc;
    }

    @Override
    public boolean accept(File dir, String name) {
        return accept(new File(dir.getPath() + name));
    }
}
