package net.sf.jabref.gui;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;

import net.sf.jabref.Globals;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;

/**
 * Data structure to contain a list of file links, parseable from a coded string.
 * Doubles as a table model for the file list editor.
*/
public class FileListTableModel extends AbstractTableModel {

    private final ArrayList<FileListEntry> list = new ArrayList<FileListEntry>();

    public FileListTableModel() {
    }

    public int getRowCount() {
        synchronized (list) {
            return list.size();
        }
    }

    public int getColumnCount() {
        return 3;
    }

    public Class<String> getColumnClass(int columnIndex) {
        return String.class;
    }

    public Object getValueAt(int rowIndex, int columnIndex) {
        synchronized (list) {
            FileListEntry entry = list.get(rowIndex);
            switch (columnIndex) {
                case 0: return entry.getDescription();
                case 1: return entry.getLink();
                default: return entry.getType() != null ?
                        entry.getType().getName() : "";
            }
        }
    }

    public FileListEntry getEntry(int index) {
        synchronized (list) {
            return list.get(index);
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
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        fireTableRowsInserted(index, index);
                    }
                });
            } else
                fireTableRowsInserted(index, index);
        }

    }

    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
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
        if (value == null)
            value = "";
        ArrayList<FileListEntry> newList = new ArrayList<FileListEntry>();
        StringBuilder sb = new StringBuilder();
        ArrayList<String> thisEntry = new ArrayList<String>();
        boolean inXmlChar = false;
        boolean escaped = false;
        for (int i=0; i<value.length(); i++) {
            char c = value.charAt(i);
            if (!escaped && (c == '\\')) {
                escaped = true;
                continue;
            }
            // Check if we are entering an XML special character construct such
            // as "&#44;", because we need to know in order to ignore the semicolon.
            else if (!escaped && (c == '&') && !inXmlChar) {
                sb.append(c);
                if ((value.length() > i+1) && (value.charAt(i+1) == '#'))
                    inXmlChar = true;
            }
            // Check if we are exiting an XML special character construct:
            else if (!escaped && inXmlChar && (c == ';')) {
                sb.append(c);
                inXmlChar = false;
            }
            else if (!escaped && (c == ':')) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
            }
            else if (!escaped && (c == ';') && !inXmlChar) {
                thisEntry.add(sb.toString());
                sb = new StringBuilder();
                if (firstOnly)
                    return decodeEntry(thisEntry, deduceUnknownTypes);
                else {
                    newList.add(decodeEntry(thisEntry, deduceUnknownTypes));
                    thisEntry.clear();
                }
            }
            else sb.append(c);
            escaped = false;
        }
        if (sb.length() > 0)
            thisEntry.add(sb.toString());
        if (thisEntry.size() > 0) {
            if (firstOnly)
                return decodeEntry(thisEntry, deduceUnknownTypes);
            else
                newList.add(decodeEntry(thisEntry, deduceUnknownTypes));
        }
          
        synchronized (list) {
            list.clear();
            list.addAll(newList);
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
        if (entry == null || entry.getType()==null )
            return null;
        return entry.getType().getIconLabel();
    }

    
    private FileListEntry decodeEntry(ArrayList<String> contents, boolean deduceUnknownType) {
        ExternalFileType type = Globals.prefs.getExternalFileTypeByName
                        (getElementIfAvailable(contents, 2));
        if (deduceUnknownType && (type instanceof UnknownExternalFileType)) {
            // No file type was recognized. Try to find a usable file type based
            // on the extension:
            ExternalFileType typeGuess = null;
            String link = getElementIfAvailable(contents, 1);
            int index = link.lastIndexOf('.');
            if ((index >= 0) && (index < link.length()-1)) {
                String extension = link.substring(index+1);
                typeGuess = Globals.prefs.getExternalFileTypeByExt(extension);
            }
            if (typeGuess != null)
                type = typeGuess;

        }
        return new FileListEntry(getElementIfAvailable(contents, 0),
                getElementIfAvailable(contents, 1),
                type);
    }


    private String getElementIfAvailable(ArrayList<String> contents, int index) {
        if (index < contents.size())
            return contents.get(index);
        else return "";
    }

    /**
     * Transform the file list shown in the table into a flat string representable
     * as a BibTeX field:
     * @return String representation.
     */
    public String getStringRepresentation() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<FileListEntry> iterator = list.iterator(); iterator.hasNext();) {
            FileListEntry entry = iterator.next();
            sb.append(encodeEntry(entry));
            if (iterator.hasNext())
                sb.append(';');
        }
        return sb.toString();
    }

    /**
     * Transform the file list shown in the table into a HTML string representation
     * suitable for displaying the contents in a tooltip.
     * @return Tooltip representation.
     */
    public String getToolTipHTMLRepresentation() {
        StringBuilder sb = new StringBuilder("<html>");
        for (Iterator<FileListEntry> iterator = list.iterator(); iterator.hasNext();) {
            FileListEntry entry = iterator.next();
            sb.append(entry.getDescription()).append(" (").append(entry.getLink()).append(')');
            if (iterator.hasNext())
                sb.append("<br>");
        }
        return sb.append("</html>").toString();
    }

    private String encodeEntry(FileListEntry entry) {
        StringBuilder sb = new StringBuilder();
        sb.append(encodeString(entry.getDescription()));
        sb.append(':');
        sb.append(encodeString(entry.getLink()));
        sb.append(':');
        sb.append(encodeString(entry.getType() != null ? entry.getType().getName() : ""));
        return sb.toString();
    }

    private String encodeString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            char c = s.charAt(i);
            if ((c == ';') || (c == ':') || (c == '\\'))
                sb.append('\\');
            sb.append(c);
        }
        return sb.toString();
    }

    public void print() {
        System.out.println("----");
        for (Iterator<FileListEntry> iterator = list.iterator(); iterator.hasNext();) {
            FileListEntry fileListEntry = iterator.next();
            System.out.println(fileListEntry);
        }
        System.out.println("----");
    }

   
}
