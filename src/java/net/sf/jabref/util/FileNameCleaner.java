/*  Copyright (C) 2012 Sarel Botha
    This class is based on http://stackoverflow.com/a/5626340/873282

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.sf.jabref.util;

import java.util.Arrays;

public class FileNameCleaner {
final static int[] illegalChars = {34, 60, 62, 124, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 58, 42, 63, 92, 47};
static {
    Arrays.sort(illegalChars);
}

/**
 * Replaces illegal characters in given fileName by '_'
 * 
 * @param badFileName the fileName to clean
 * @return a clean filename
 */
public static String cleanFileName(String badFileName) {
    StringBuilder cleanName = new StringBuilder();
    for (int i = 0; i < badFileName.length(); i++) {
        int c = (int)badFileName.charAt(i);
        if (Arrays.binarySearch(illegalChars, c) < 0) {
            cleanName.append((char)c);
        } else {
        	cleanName.append('_');
        }
    }
    return cleanName.toString();
}
}