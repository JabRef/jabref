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
            if(tempFile == null) {
                return null;
            }

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
        if (!file.delete()) {
            System.err.println("Cannot delete file");
        }
    }
}
