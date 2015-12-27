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
package net.sf.jabref.external;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.transform.TransformerException;

import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.OpenFileFilter;
import net.sf.jabref.gui.UrlDragDrop;
import net.sf.jabref.gui.entryeditor.EntryEditor;
import net.sf.jabref.gui.fieldeditors.FieldEditor;
import net.sf.jabref.gui.net.MonitoredURLDownload;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.*;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.logic.util.io.FileFinder;
import net.sf.jabref.logic.xmp.XMPUtil;

/**
 * Initial Version:
 *
 * @author alver
 * @version Date: May 7, 2005 Time: 7:17:42 PM
 *
 */
public class ExternalFilePanel extends JPanel {
    private EntryEditor entryEditor;

    private final JabRefFrame frame;

    private final OpenFileFilter off;

    private BibEntry entry;

    private BibDatabase database;

    private final MetaData metaData;

    private static final Log LOGGER = LogFactory.getLog(ExternalFilePanel.class);


    public ExternalFilePanel(final String fieldName, final MetaData metaData, final BibEntry entry, final FieldEditor editor, final OpenFileFilter off) {
        this(null, metaData, null, fieldName, off, editor);
        this.entry = entry;
        this.entryEditor = null;
    }

    public ExternalFilePanel(final JabRefFrame frame, final MetaData metaData,
            final EntryEditor entryEditor, final String fieldName, final OpenFileFilter off,
            final FieldEditor editor) {

        this.frame = frame;
        this.metaData = metaData;
        this.off = off;
        this.entryEditor = entryEditor;

        setLayout(new GridLayout(2, 2));

        JButton browseBut = new JButton(Localization.lang("Browse"));
        JButton download = new JButton(Localization.lang("Download"));
        JButton auto = new JButton(Localization.lang("Auto"));
        JButton xmp = new JButton(Localization.lang("Write XMP"));
        xmp.setToolTipText(Localization.lang("Write BibtexEntry as XMP-metadata to PDF."));

        browseBut.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                browseFile(fieldName, editor);
                // editor.setText(chosenValue);
                entryEditor.storeFieldAction.actionPerformed(new ActionEvent(editor, 0, ""));
            }
        });

        download.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                downLoadFile(fieldName, editor, frame);
            }
        });

        auto.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JabRefExecutorService.INSTANCE.execute(autoSetFile(fieldName, editor));
            }
        });
        xmp.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pushXMP(fieldName, editor);
            }
        });

        add(browseBut);
        add(download);
        add(auto);
        add(xmp);

        // Add drag and drop support to the field
        if (editor != null) {
            ((JComponent) editor).setDropTarget(new DropTarget((Component) editor,
                    DnDConstants.ACTION_NONE, new UrlDragDrop(entryEditor, frame, editor)));
        }
    }

    /**
     * Change which entry this panel is operating on. This is used only when
     * this panel is not attached to an entry editor.
     */
    public void setEntry(BibEntry entry, BibDatabase database) {
        this.entry = entry;
        this.database = database;
    }

    private BibDatabase getDatabase() {
        return database != null ? database : entryEditor.getDatabase();
    }

    private BibEntry getEntry() {
        return entry != null ? entry : entryEditor.getEntry();
    }

    private Object getKey() {
        return getEntry().getCiteKey();
    }

    private void output(String s) {
        if (frame != null) {
            frame.output(s);
        }
    }

    private void pushXMP(final String fieldName, final FieldEditor editor) {

        JabRefExecutorService.INSTANCE.execute(new Runnable() {

            @Override
            public void run() {

                output(Localization.lang("Looking for pdf..."));

                // Find the default directory for this field type, if any:
                String[] dirs = metaData.getFileDirectory(fieldName);
                File file = null;
                if (dirs.length > 0) {
                    File tmp = FileUtil.expandFilename(editor.getText(), dirs);
                    if (tmp != null) {
                        file = tmp;
                    }
                }

                if (file == null) {
                    file = new File(editor.getText());
                }

                final File finalFile = file;

                output(Localization.lang("Writing XMP to '%0'...", finalFile.getName()));
                try {
                    XMPUtil.writeXMP(finalFile, getEntry(), getDatabase());
                    output(Localization.lang("Wrote XMP to '%0'.", finalFile.getName()));
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(editor.getParent(),
                            Localization.lang("Error writing XMP to file: %0", e.getLocalizedMessage()),
                            Localization.lang("Writing XMP"), JOptionPane.ERROR_MESSAGE);
                    // String above and below
                    LOGGER.info("Error writing XMP to file: " + finalFile.getAbsolutePath(), e);
                    output(Localization.lang("Error writing XMP to file: %0", finalFile.getName()));

                } catch (TransformerException e) {
                    JOptionPane.showMessageDialog(editor.getParent(),
                            Localization.lang("Error converting BibTeX to XMP: %0", e.getLocalizedMessage()),
                            Localization.lang("Writing XMP"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.info("Error while converting BibEntry to XMP " + finalFile.getAbsolutePath(), e);
                    output(Localization.lang("Error converting XMP to '%0'...", finalFile.getName()));
                }
            }
        });
    }

    public void browseFile(final String fieldName, final FieldEditor editor) {

        String[] dirs = metaData.getFileDirectory(fieldName);
        String directory = null;
        if (dirs.length > 0) {
            directory = dirs[0]; // Default to the first directory in the list
        }

        String dir = editor.getText();
        String retVal;

        if ((directory == null) || !new File(dir).isAbsolute()) {
            if (directory != null) {
                dir = directory;
            } else {
                dir = Globals.prefs.get(fieldName + Globals.FILETYPE_PREFS_EXT, "");
            }
        }

        String chosenFile = FileDialogs.getNewFile(frame, new File(dir), '.' + fieldName, JFileChooser.OPEN_DIALOG, false);

        if (chosenFile != null) {
            File newFile = new File(chosenFile);
            String position = newFile.getParent();

            if ((directory != null) && position.startsWith(directory)) {
                // Construct path relative to pdf base dir
                String relPath = position.substring(directory.length(), position.length()) + File.separator + newFile.getName();

                // Remove leading path separator
                if (relPath.startsWith(File.separator)) {
                    relPath = relPath.substring(File.separator.length(), relPath.length());

                    // Set relative path as field value
                }

                retVal = relPath;
            } else {
                retVal = newFile.getPath();
            }

            editor.setText(retVal);
            Globals.prefs.put(fieldName + Globals.FILETYPE_PREFS_EXT, newFile.getPath());
        }
    }

    public void downLoadFile(final String fieldName, final FieldEditor fieldEditor,
            final Component parent) {

        final String res = JOptionPane.showInputDialog(parent,
                Localization.lang("Enter URL to download"));

        if ((res == null) || res.trim().isEmpty()) {
            return;
        }

        /*
         * If this panel belongs in an entry editor, note which entry is
         * currently shown:
         */
        final BibEntry targetEntry;
        if (entryEditor != null) {
            targetEntry = entryEditor.getEntry();
        } else {
            targetEntry = entry;
        }

        JabRefExecutorService.INSTANCE.execute(new Runnable() {

            public String getPlannedFileName(String result) {
                String suffix = off.getSuffix(result);
                if (suffix == null) {
                    suffix = '.' + fieldName.toLowerCase();
                }

                String plannedName;
                if (getKey() != null) {
                    plannedName = getKey() + suffix;
                } else {
                    plannedName = JOptionPane.showInputDialog(parent,
                            Localization.lang("BibTeX key not set. Enter a name for the downloaded file"));
                    if ((plannedName != null) && !off.accept(plannedName)) {
                        plannedName += suffix;
                    }
                }

                /*
                 * [ 1548875 ] download pdf produces unsupported filename
                 *
                 * http://sourceforge.net/tracker/index.php?func=detail&aid=1548875&group_id=92314&atid=600306
                 *
                 */
                if (OS.WINDOWS) {
                    plannedName = plannedName.replaceAll(
                            "\\?|\\*|\\<|\\>|\\||\\\"|\\:|\\.$|\\[|\\]", "");
                } else if (OS.OS_X) {
                    plannedName = plannedName.replaceAll(":", "");
                }

                return plannedName;
            }

            @Override
            public void run() {
                String originalText = fieldEditor.getText();
                fieldEditor.setEnabled(false);
                boolean updateEditor = true;

                try {
                    fieldEditor.setText(Localization.lang("Downloading..."));
                    output(Localization.lang("Downloading..."));
                    String plannedName = getPlannedFileName(res);

                    // Find the default directory for this field type:
                    String[] dirs = metaData.getFileDirectory(fieldName);
                    String directory = null;
                    // Look for the first one in the list that exists:
                    for (String dir : dirs) {
                        if (new File(dir).exists()) {
                            directory = dir;
                            break;
                        }
                    }
                    if (directory == null) {
                        if (dirs.length > 0) {
                            JOptionPane.showMessageDialog(parent, Localization.lang("Could not find directory for %0-files: %1", fieldName, dirs[0]),
                                    Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(parent, Localization.lang("No directory defined for %0-files", fieldName),
                                    Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                        }
                        return;
                    }
                    File file = new File(new File(directory), plannedName);

                    URL url = new URL(res);

                    try {
                        MonitoredURLDownload.buildMonitoredDownload(parent, url).downloadToFile(file);
                    } catch (IOException e2) {
                        JOptionPane.showMessageDialog(parent, Localization.lang("Invalid URL") + ": "
                                        + e2.getMessage(), Localization.lang("Download file"),
                                JOptionPane.ERROR_MESSAGE);
                        LOGGER.info("Error while downloading " + url, e2);
                        return;
                    }
                    output(Localization.lang("Download completed"));

                    String textToSet = file.getPath();
                    if (textToSet.startsWith(directory)) {
                        // Construct path relative to pdf base dir
                        textToSet = textToSet.substring(directory.length(), textToSet.length());

                        // Remove leading path separator
                        if (textToSet.startsWith(File.separator)) {
                            textToSet = textToSet.substring(File.separator.length());
                        }
                    }

                    /*
                     * Check if we should update the editor text field, or
                     * update the target entry directly:
                     */
                    if ((entryEditor == null) || (entryEditor.getEntry() != targetEntry)) {
                        /*
                         * Editor has probably changed to show a different
                         * entry. So we must update the target entry directly
                         * and not set the text of the editor.
                         */
                        targetEntry.setField(fieldName, textToSet);
                        fieldEditor.setText(textToSet);
                        fieldEditor.setEnabled(true);
                        updateEditor = false;
                    } else {
                        /*
                         * Need to set the fieldEditor first before running
                         * updateField-Action, because otherwise we might get a
                         * race condition.
                         *
                         * (Hopefully a) Fix for: [ 1545601 ] downloading pdf
                         * corrupts pdf field text
                         *
                         * http://sourceforge.net/tracker/index.php?func=detail&aid=1545601&group_id=92314&atid=600306
                         */
                        fieldEditor.setText(textToSet);
                        fieldEditor.setEnabled(true);
                        updateEditor = false;
                        SwingUtilities.invokeLater(new Runnable() {

                            @Override
                            public void run() {
                                entryEditor.updateField(fieldEditor);
                            }
                        });
                    }

                } catch (MalformedURLException e1) {
                    JOptionPane.showMessageDialog(parent, Localization.lang("Invalid URL"),
                            Localization.lang("Download file"), JOptionPane.ERROR_MESSAGE);
                } finally {
                    // If stuff goes wrong along the road, put back original
                    // value
                    if (updateEditor) {
                        fieldEditor.setText(originalText);
                        fieldEditor.setEnabled(true);
                    }
                }
            }
        });
    }

    /**
     * Creates a Runnable that searches the external file directory for the given
     * field name, including subdirectories, and looks for files named after the
     * current entry's bibtex key.
     *
     * @param fieldName
     *            The field to set.
     * @param editor
     *            An EntryEditor instance where to set the value found.
     * @return A reference to the Runnable that can perform the operation.
     */
    public Runnable autoSetFile(final String fieldName, final FieldEditor editor) {
        Object o = getKey();
        if ((o == null) || (Globals.prefs.get(fieldName + Globals.DIR_SUFFIX) == null)) {
            output(Localization.lang("You must set both BibTeX key and %0 directory", fieldName
                    .toUpperCase())
                    + '.');
            return null;
        }
        output(Localization.lang("Searching for %0 file", fieldName.toUpperCase()) + " '" + o + '.'
                + fieldName + "'...");

        return new Runnable() {

            @Override
            public void run() {
                /*
                 * Find the following directories to look in for:
                 *
                 * default directory for this field type.
                 *
                 * directory of bibtex-file. // NOT POSSIBLE at the moment.
                 *
                 * JabRef-directory.
                 */
                LinkedList<String> list = new LinkedList<>();
                String[] dirs = metaData.getFileDirectory(fieldName);
                Collections.addAll(list, dirs);

                String found = FileFinder.findPdf(getEntry(), fieldName, list
                        .toArray(new String[list.size()]));// , off);

                // To activate findFile:
                // String found = Util.findFile(getEntry(), null, dir,
                // ".*[bibtexkey].*");

                if (found != null) {
                    editor.setText(found);
                    if (entryEditor != null) {
                        entryEditor.updateField(editor);
                    }
                    output(Localization.lang("%0 field set", fieldName.toUpperCase()) + '.');
                } else {
                    output(Localization.lang("No %0 found", fieldName.toUpperCase()) + '.');
                }

            }
        };
    }
}
