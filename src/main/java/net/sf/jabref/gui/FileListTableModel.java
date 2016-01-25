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

import java.util.*;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.logic.util.io.FileUtil;
import net.sf.jabref.model.entry.FileField;

/**
 * Data structure to contain a list of file links, parseable from a coded string.
 * Doubles as a table model for the file list editor.
 */
public class FileListTableModel extends AbstractTableModel {

    private final ArrayList<FileListEntry> list = new ArrayList<>();

    @Override
    public int getRowCount() {
        synchronized (list) {
            return list.size();
        }
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Class<String> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (list) {
            FileListEntry entry = list.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return entry.description;
            case 1:
                return entry.link;
            default:
                return entry.type != null ? entry.type.getName() : "";
            }
        }
    }

    public FileListEntry getEntry(int index) {
        synchronized (list) {
            return list.get(index);
        }
    }

    public void setEntry(int index, FileListEntry entry) {
        synchronized (list) {
            list.set(index, entry);
            fireTableRowsUpdated(index, index);
        }
    }

    public void removeEntry(int index) {
        synchronized (list) {
            list.remove(index);
            fireTableRowsDeleted(index, index);
        }

    }

    /**
     * Add an entry to the table model, and fire a change event. The change event
     * is fired on the event dispatch thread.
     * @param index The row index to insert the entry at.
     * @param entry The entry to insert.
     */
    public void addEntry(final int index, final FileListEntry entry) {
        synchronized (list) {
            list.add(index, entry);
            if (!SwingUtilities.isEventDispatchThread()) {
                SwingUtilities.invokeLater(() -> fireTableRowsInserted(index, index));
            } else {
                fireTableRowsInserted(index, index);
            }
        }

    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        // Do nothing
    }

    /**
     * Set up the table contents based on the flat string representation of the file list
     * @param value The string representation
     */
    public void setContent(String value) {
        setContent(value, false, true);
    }

    public void setContentDontGuessTypes(String value) {
        setContent(value, false, false);
    }

    private FileListEntry setContent(String value, boolean firstOnly, boolean deduceUnknownTypes) {
        if (value == null) {
            value = "";
        }

        List<FileField.ParsedFileField> fields = FileField.parse(value);
        ArrayList<FileListEntry> files = new ArrayList<>();

        for(FileField.ParsedFileField entry : fields) {
            if (entry.isEmpty()) {
                continue;
            }

            if (firstOnly) {
                return decodeEntry(entry, deduceUnknownTypes);
            } else {
                files.add(decodeEntry(entry, deduceUnknownTypes));
            }
        }

        synchronized (list) {
            list.clear();
            list.addAll(files);
        }
        fireTableChanged(new TableModelEvent(this));
        return null;
    }

    /**
     * Convenience method for finding a label corresponding to the type of the
     * first file link in the given field content. The difference between using
     * this method and using setContent() on an instance of FileListTableModel
     * is a slight optimization: with this method, parsing is discontinued after
     * the first entry has been found.
     * @param content The file field content, as fed to this class' setContent() method.
     * @return A JLabel set up with no text and the icon of the first entry's file type,
     *  or null if no entry was found or the entry had no icon.
     */
    public static JLabel getFirstLabel(String content) {
        FileListTableModel tm = new FileListTableModel();
        FileListEntry entry = tm.setContent(content, true, true);
        if ((entry == null) || (entry.type == null)) {
            return null;
        }
        return entry.type.getIconLabel();
    }

    private FileListEntry decodeEntry(FileField.ParsedFileField entry, boolean deduceUnknownType) {
        ExternalFileType type = ExternalFileTypes.getInstance().getExternalFileTypeByName(entry.fileType);

        if (deduceUnknownType && (type instanceof UnknownExternalFileType)) {
            // No file type was recognized. Try to find a usable file type based
            // on mime type:
            type = ExternalFileTypes.getInstance().getExternalFileTypeByMimeType(entry.fileType);
            if (type == null) {
                // No type could be found from mime type on the extension:
                Optional<String> extension = FileUtil.getFileExtension(entry.link);
                if (extension.isPresent()) {
                    ExternalFileType typeGuess = ExternalFileTypes.getInstance().getExternalFileTypeByExt(extension.get());

                    if (typeGuess != null) {
                        type = typeGuess;
                    }
                }
            }
        }

        return new FileListEntry(entry.description, entry.link, type);
    }

    /**
     * Transform the file list shown in the table into a flat string representable
     * as a BibTeX field:
     * @return String representation.
     */
    public String getStringRepresentation() {
        String[][] array = new String[list.size()][];
        int i = 0;
        for (FileListEntry entry : list) {
            array[i] = entry.getStringArrayRepresentation();
            i++;
        }
        return FileField.encodeStringArray(array);
    }

    /**
     * Transform the file list shown in the table into a HTML string representation
     * suitable for displaying the contents in a tooltip.
     * @return Tooltip representation.
     */
    public String getToolTipHTMLRepresentation() {
        StringJoiner sb = new StringJoiner("<br>", "<html>", "</html>");

        for(FileListEntry entry : list) {
            sb.add(String.format("%s (%s)", entry.description, entry.link));
        }

        return sb.toString();
    }

}
