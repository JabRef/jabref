/**
 * Copyright (C) 2015 JabRef contributors
 *
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
package net.sf.jabref;

import java.io.File;

public class FileBasedTestHelper {
    /**
     * Creates a temp directory in the System temp directory.
     * <p/>
     * Taken from
     * http://forum.java.sun.com/thread.jspa?threadID=470197&messageID=2169110
     * <p/>
     * Author: jfbriere
     *
     * @return returns null if directory could not created.
     */
    public static File createTempDir(String prefix) {
        return createTempDir(prefix, null);
    }

    /**
     * Creates a temp directory in a given directory.
     * <p/>
     * Taken from
     * http://forum.java.sun.com/thread.jspa?threadID=470197&messageID=2169110
     * <p/>
     * Author: jfbriere
     *
     * @param directory MayBeNull - null indicates that the system tmp directory
     *                  should be used.
     * @return returns null if directory could not created.
     */
    public static File createTempDir(String prefix, File directory) {
        try {
            File tempFile = File.createTempFile(prefix, "", directory);

            if (!tempFile.delete()) {
                return null;
            }
            if (!tempFile.mkdir()) {
                return null;
            }

            return tempFile;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deletes a directory or file
     * <p/>
     * Taken from
     * http://forum.java.sun.com/thread.jspa?threadID=470197&messageID=2169110
     * <p/>
     * Author: jfbriere
     *
     * @param file
     */
    public static void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] fileArray = file.listFiles();

            if (fileArray != null) {
                for (File aFileArray : fileArray) {
                    deleteRecursive(aFileArray);
                }
            }
        }
        file.delete();
    }
}
