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
package net.sf.jabref.gui;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.logic.util.OS;

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
    public static List<String> getMultipleFiles(JFrame owner, File directory, String extension,
            boolean updateWorkingdirectory) {

        OpenFileFilter off = null;
        if (extension == null) {
            off = new OpenFileFilter();
        } else if (!extension.equals(Globals.NONE)) {
            off = new OpenFileFilter(extension);
        }

        Object files = FileDialogs.getNewFileImpl(owner, directory, extension, null, off, JFileChooser.OPEN_DIALOG, updateWorkingdirectory, false, true, null);

        if (files instanceof String[]) {
            return Arrays.asList((String[]) files);
        }
        // Fix for:
        // http://sourceforge.net/tracker/index.php?func=detail&aid=1538769&group_id=92314&atid=600306
        if (files != null) {
            return Arrays.asList((String) files);
        }
        return Collections.emptyList();
    }

    public static String getNewFile(JFrame owner, File directory, String extension, int dialogType, boolean updateWorkingDirectory) {
        return FileDialogs.getNewFile(owner, directory, extension, null, dialogType, updateWorkingDirectory, false, null);
    }

    public static String getNewFile(JFrame owner, File directory, String extension, int dialogType, boolean updateWorkingDirectory, JComponent accessory) {
        return FileDialogs.getNewFile(owner, directory, extension, null, dialogType, updateWorkingDirectory, false, accessory);
    }

    public static String getNewFile(JFrame owner, File directory, String extension, String description, int dialogType, boolean updateWorkingDirectory) {
        return FileDialogs.getNewFile(owner, directory, extension, description, dialogType, updateWorkingDirectory, false, null);
    }

    public static String getNewDir(JFrame owner, File directory, String extension, int dialogType, boolean updateWorkingDirectory) {
        return FileDialogs.getNewFile(owner, directory, extension, null, dialogType, updateWorkingDirectory, true, null);
    }

    public static String getNewDir(JFrame owner, File directory, String extension, String description, int dialogType, boolean updateWorkingDirectory) {
        return FileDialogs.getNewFile(owner, directory, extension, description, dialogType, updateWorkingDirectory, true, null);
    }

    private static String getNewFile(JFrame owner, File directory, String extension, String description, int dialogType, boolean updateWorkingDirectory, boolean dirOnly, JComponent accessory) {

        OpenFileFilter off = null;

        if (extension == null) {
            off = new OpenFileFilter();
        } else if (!extension.equals(Globals.NONE)) {
            off = new OpenFileFilter(extension);
        }

        return (String) FileDialogs.getNewFileImpl(owner, directory, extension, description, off, dialogType, updateWorkingDirectory, dirOnly, false, accessory);
    }

    private static Object getNewFileImpl(JFrame owner, File directory, String extension, String description, OpenFileFilter off, int dialogType, boolean updateWorkingDirectory, boolean dirOnly, boolean multipleSelection, JComponent accessory) {

        // Added the !dirOnly condition below as a workaround to the native file dialog
        // not supporting directory selection:
        if (!dirOnly && OS.OS_X) {
            return FileDialogs.getNewFileForMac(owner, directory, dialogType, updateWorkingDirectory);
        }

        JFileChooser fc;
        try {
            fc = new JFileChooser(directory);//JabRefFileChooser(directory);
            if (accessory != null) {
                fc.setAccessory(accessory);
            }
        } catch (InternalError errl) {
            // This try/catch clause was added because a user reported an
            // InternalError getting thrown on WinNT, presumably because of a
            // bug in JGoodies Windows PLAF. This clause can be removed if the
            // bug is fixed, but for now we just resort to the native file
            // dialog, using the same method as is always used on Mac:
            return FileDialogs.getNewFileForMac(owner, directory, dialogType, updateWorkingDirectory);
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
        if (dialogResult != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        // okay button
        File selectedFile = fc.getSelectedFile();
        if (selectedFile == null) { // cancel
            return null;
        }

        // If this is a save dialog, and the user has not chosen "All files" as
        // filter
        // we enforce the given extension. But only if extension is not null.
        if ((extension != null) && (dialogType == JFileChooser.SAVE_DIALOG) && (fc.getFileFilter() == off) && !off.accept(selectedFile)) {

            // add the first extension if there are multiple extensions
            selectedFile = new File(selectedFile.getPath() + extension.split("[, ]+", 0)[0]);
        }

        if (updateWorkingDirectory) {
            Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, selectedFile.getPath());
        }

        if (multipleSelection) {
            File[] files = fc.getSelectedFiles();
            String[] filenames = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                filenames[i] = files[i].getAbsolutePath();
            }
            return filenames;
        } else {
            return selectedFile.getAbsolutePath();
        }
    }

    private static String getNewFileForMac(JFrame owner, File directory, int dialogType,
            boolean updateWorkingDirectory) {

        java.awt.FileDialog fc = new java.awt.FileDialog(owner);

        if (directory != null) {
            fc.setDirectory(directory.getParent());
        }
        if (dialogType == JFileChooser.OPEN_DIALOG) {
            fc.setMode(java.awt.FileDialog.LOAD);
        } else {
            fc.setMode(java.awt.FileDialog.SAVE);
        }

        fc.setVisible(true);

        if (fc.getFile() == null) {
            return null;
        } else {
            if (updateWorkingDirectory) {
                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, fc.getDirectory() + fc.getFile());
            }
            return fc.getDirectory() + fc.getFile();
        }
    }
}
