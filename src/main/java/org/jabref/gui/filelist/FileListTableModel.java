package org.jabref.gui.filelist;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.model.entry.FileFieldParser;
import org.jabref.model.entry.FileFieldWriter;
import org.jabref.model.entry.LinkedFile;

/**
 * Data structure to contain a list of file links, parseable from a coded string.
 * Doubles as a table model for the file list editor.
 *
 * @deprecated use {@link org.jabref.model.entry.LinkedFile} instead
 */
@Deprecated
public class FileListTableModel extends AbstractTableModel {

    private final List<FileListEntry> list = new ArrayList<>();

    /**
     * Convenience method for finding a label corresponding to the type of the
     * first file link in the given field content. The difference between using
     * this method and using setContent() on an instance of FileListTableModel
     * is a slight optimization: with this method, parsing is discontinued after
     * the first entry has been found.
     *
     * @param content The file field content, as fed to this class' setContent() method.
     * @return A JLabel set up with no text and the icon of the first entry's file type, or null if no entry was found
     * or the entry had no icon.
     */
    public static JLabel getFirstLabel(String content) {
        FileListTableModel tm = new FileListTableModel();
        FileListEntry entry = tm.setContent(content, true, true);
        if ((entry == null) || (!entry.getType().isPresent())) {
            return null;
        }
        return entry.getType().get().getIconLabel();
    }

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
                return entry.getDescription();
            case 1:
                return entry.getLink();
            default:
                return entry.getType().isPresent() ? entry.getType().get().getName() : "";
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
            if (SwingUtilities.isEventDispatchThread()) {
                fireTableRowsInserted(index, index);
            } else {
                SwingUtilities.invokeLater(() -> fireTableRowsInserted(index, index));
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

    private FileListEntry setContent(String val, boolean firstOnly, boolean deduceUnknownTypes) {
        String value = val;
        if (value == null) {
            value = "";
        }

        List<LinkedFile> fields = FileFieldParser.parse(value);
        List<FileListEntry> files = new ArrayList<>();

        for (LinkedFile entry : fields) {

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

    private FileListEntry decodeEntry(LinkedFile entry, boolean deduceUnknownType) {
        Optional<ExternalFileType> type = ExternalFileTypes.getInstance().fromLinkedFile(entry, deduceUnknownType);

        return new FileListEntry(entry.getDescription(), entry.getLink(), type);
    }

    /**
     * Transform the file list shown in the table into a flat string representable
     * as a BibTeX field:
     * @return String representation.
     */
    public String getStringRepresentation() {
        synchronized (list) {
            String[][] array = new String[list.size()][];
            int i = 0;
            for (FileListEntry entry : list) {
                array[i] = entry.getStringArrayRepresentation();
                i++;
            }
            return FileFieldWriter.encodeStringArray(array);
        }
    }

    /**
     * Transform the file list shown in the table into a HTML string representation
     * suitable for displaying the contents in a tooltip.
     * @return Tooltip representation.
     */
    public String getToolTipHTMLRepresentation() {
        StringJoiner sb = new StringJoiner("<br>", "<html>", "</html>");

        synchronized (list) {
            for (FileListEntry entry : list) {
                sb.add(String.format("%s (%s)", entry.getDescription(), entry.getLink()));
            }
        }

        return sb.toString();
    }

}
