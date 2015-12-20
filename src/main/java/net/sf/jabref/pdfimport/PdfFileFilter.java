/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General public static License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General public static License for more details.

    You should have received a copy of the GNU General public static License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.pdfimport;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 08.09.2010
 * Time: 15:03:36
 * To change this template use File | Settings | File Templates.
 */
public class PdfFileFilter implements FileFilter {

    @Override
    public boolean accept(File file) {
        String path = file.getPath();

        return isMatchingFileFilter(path);
    }

    public boolean accept(String path) {
        if ((path == null) || path.isEmpty() || !path.contains(".")) {
            return false;
        }

        return isMatchingFileFilter(path);
    }

    private static boolean isMatchingFileFilter(String path) {
        String dateiEndung = path.substring(path.lastIndexOf(".") + 1);
        return "pdf".equalsIgnoreCase(dateiEndung);
    }

}
