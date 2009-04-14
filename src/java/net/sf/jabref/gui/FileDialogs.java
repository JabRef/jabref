package net.sf.jabref.gui;

import net.sf.jabref.OpenFileFilter;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.io.File;
import java.io.FilenameFilter;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Apr 14, 2009
 * Time: 7:24:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileDialogs {

    /**
     * Will return the names of multiple files selected in the given directory
     * and the given extensions.
     *
     * Will return an empty String array if no entry is found.
     *
     * @param owner
     * @param directory
     * @param extension
     * @param updateWorkingdirectory
     * @return an array of selected file paths, or an empty array if no selection is made.
     */
    public static String[] getMultipleFiles(JFrame owner, File directory, String extension,
                                            boolean updateWorkingdirectory) {

        OpenFileFilter off = null;
        if (extension == null)
            off = new OpenFileFilter();
        else if (!extension.equals(Globals.NONE))
            off = new OpenFileFilter(extension);

        Object files = getNewFileImpl(owner, directory, extension, null, off,
                JFileChooser.OPEN_DIALOG, updateWorkingdirectory, false, true, null);

        if (files instanceof String[]) {
            return (String[]) files;
        }
        // Fix for:
        // http://sourceforge.net/tracker/index.php?func=detail&aid=1538769&group_id=92314&atid=600306
        if (files != null) {
            return new String[] { (String) files };
        }
        return new String[0];
    }

    public static String getNewFile(JFrame owner, File directory, String extension, int dialogType,
                                    boolean updateWorkingDirectory) {
        return getNewFile(owner, directory, extension, null, dialogType, updateWorkingDirectory,
                false, null);
    }

    public static String getNewFile(JFrame owner, File directory, String extension, int dialogType,
                                    boolean updateWorkingDirectory, JComponent accessory) {
        return getNewFile(owner, directory, extension, null, dialogType, updateWorkingDirectory,
                false, accessory);
    }

    public static String getNewFile(JFrame owner, File directory, String extension,
                                    String description, int dialogType, boolean updateWorkingDirectory) {
        return getNewFile(owner, directory, extension, description, dialogType,
                updateWorkingDirectory, false, null);
    }

    public static String getNewDir(JFrame owner, File directory, String extension, int dialogType,
                                   boolean updateWorkingDirectory) {
        return getNewFile(owner, directory, extension, null, dialogType, updateWorkingDirectory,
                true, null);
    }

    public static String getNewDir(JFrame owner, File directory, String extension,
                                   String description, int dialogType, boolean updateWorkingDirectory) {
        return getNewFile(owner, directory, extension, description, dialogType,
                updateWorkingDirectory, true, null);
    }

    public static String getNewFile(JFrame owner, File directory, String extension,
                                    String description, int dialogType, boolean updateWorkingDirectory, boolean dirOnly,
                                    JComponent accessory) {

        OpenFileFilter off = null;

        if (extension == null)
            off = new OpenFileFilter();
        else if (!extension.equals(Globals.NONE))
            off = new OpenFileFilter(extension);

        return (String) getNewFileImpl(owner, directory, extension, description, off, dialogType,
                updateWorkingDirectory, dirOnly, false, accessory);
    }

    public static Object getNewFileImpl(JFrame owner, File directory, String extension,
                                        String description, OpenFileFilter off, int dialogType, boolean updateWorkingDirectory,
                                        boolean dirOnly, boolean multipleSelection, JComponent accessory) {

// Added the !dirOnly condition below as a workaround to the native file dialog
// not supporting directory selection:
        if (!dirOnly && Globals.prefs.getBoolean("useNativeFileDialogOnMac")) {

            return getNewFileForMac(owner, directory, extension, dialogType,
                    updateWorkingDirectory, dirOnly, off);
        }

        JFileChooser fc;
        try {
            fc = new JFileChooser(directory);//JabRefFileChooser(directory);
            if (accessory != null)
                fc.setAccessory(accessory);
        } catch (InternalError errl) {
            // This try/catch clause was added because a user reported an
            // InternalError getting thrown on WinNT, presumably because of a
            // bug in JGoodies Windows PLAF. This clause can be removed if the
            // bug is fixed, but for now we just resort to the native file
            // dialog, using the same method as is always used on Mac:
            return getNewFileForMac(owner, directory, extension, dialogType,
                    updateWorkingDirectory, dirOnly, off);
        }

        if (dirOnly) {
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        }

        fc.setMultiSelectionEnabled(multipleSelection);

        fc.addChoosableFileFilter(off);
        fc.setDialogType(dialogType);
        int dialogResult;
        if (dialogType == JFileChooser.OPEN_DIALOG) {
            dialogResult = fc.showOpenDialog(owner);
        } else if (dialogType == JFileChooser.SAVE_DIALOG) {
            dialogResult = fc.showSaveDialog(owner);
        } else {
            dialogResult = fc.showDialog(owner, description);
        }

        // the getSelectedFile method returns a valid fileselection
        // (if something is selected) indepentently from dialog return status
        if (dialogResult != JFileChooser.APPROVE_OPTION)
            return null;

        // okay button
        File selectedFile = fc.getSelectedFile();
        if (selectedFile == null) { // cancel
            return null;
        }

        // If this is a save dialog, and the user has not chosen "All files" as
        // filter
        // we enforce the given extension. But only if extension is not null.
        if ((extension != null) && (dialogType == JFileChooser.SAVE_DIALOG)
                && (fc.getFileFilter() == off) && !off.accept(selectedFile)) {

            // add the first extension if there are multiple extensions
            selectedFile = new File(selectedFile.getPath() + extension.split("[, ]+", 0)[0]);
        }

        if (updateWorkingDirectory) {
            Globals.prefs.put("workingDirectory", selectedFile.getPath());
        }

        if (!multipleSelection)
            return selectedFile.getAbsolutePath();
        else {
            File[] files = fc.getSelectedFiles();
            String[] filenames = new String[files.length];
            for (int i = 0; i < files.length; i++)
                filenames[i] = files[i].getAbsolutePath();
            return filenames;
        }
    }

    public static String getNewFileForMac(JFrame owner, File directory, String extensions,
                                          int dialogType, boolean updateWorkingDirectory, boolean dirOnly, FilenameFilter filter) {

        java.awt.FileDialog fc = new java.awt.FileDialog(owner);

        // fc.setFilenameFilter(filter);
        if (directory != null) {
            fc.setDirectory(directory.getParent());
        }
        if (dialogType == JFileChooser.OPEN_DIALOG) {
            fc.setMode(java.awt.FileDialog.LOAD);
        } else {
            fc.setMode(java.awt.FileDialog.SAVE);
        }

        fc.setVisible(true); // fc.show(); -> deprecated since 1.5

        if (fc.getFile() != null) {
            Globals.prefs.put("workingDirectory", fc.getDirectory() + fc.getFile());
            return fc.getDirectory() + fc.getFile();
        } else {
            return null;
        }
    }
}
