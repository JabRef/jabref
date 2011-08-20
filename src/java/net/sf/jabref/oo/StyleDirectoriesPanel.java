package net.sf.jabref.oo;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.Globals;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Panel for setting up the list of directories and files to search for style files.
 * Each directory entry can be set to be searched recursively or not.
 */
public class StyleDirectoriesPanel {

    List<ActionListener> listeners = new ArrayList<ActionListener>();
    EventList<DirElement> entries = new BasicEventList<DirElement>();
    EventList<DirElement> sortedEntries;
    EventTableModel<DirElement> tableModel;
    EventSelectionModel<DirElement> selectionModel;
    JPanel panel = new JPanel();
    JButton addFile = new JButton(Globals.lang("Add file")),
        addDir = new JButton(Globals.lang("Add directory")),
        remove = new JButton(Globals.lang("Remove"));
    FileFilter styleFileFilter = null;

    static final int TABLE_VISIBLE_ROWS = 5;
    private JDialog parent;

    /**
     *
     * @param dirs A String array containing two elements per file/directory:
     *   first the path, then the string "true" or "false", determining whether
     *   the element should be handled recursively.
     */
    public StyleDirectoriesPanel(JDialog parent, String[] dirs) {
        this.parent = parent;
        sortedEntries = new SortedList<DirElement>(entries);
        if (dirs != null)
            setValues(dirs);
        panel.setLayout(new BorderLayout());
        tableModel = new EventTableModel<DirElement>(sortedEntries, new DirElementFormat());
        selectionModel = new EventSelectionModel<DirElement>(sortedEntries);
        JTable table = new JTable(tableModel);
        table.setSelectionModel(selectionModel);
        table.setPreferredScrollableViewportSize(new Dimension(
            table.getPreferredScrollableViewportSize().width,
            TABLE_VISIBLE_ROWS*table.getRowHeight()));
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(200);
        cm.getColumn(1).setPreferredWidth(40);

        JScrollPane sp = new JScrollPane(table);
        ButtonStackBuilder bb = new ButtonStackBuilder();
        bb.addGridded(addFile);
        bb.addGridded(addDir);
        bb.addGridded(remove);
        bb.addGlue();
        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("fill:pref:grow, 2dlu, fill:pref",
                "pref, fill:pref"));
        b.appendSeparator(Globals.lang("Directories and files"));
        b.nextLine();
        b.append(new JLabel("<html>"+Globals.lang("Here you set up which single files and directories (with or without subdirectories)<br>"
                +" should be searched to build the list of available styles.")+"</html>"), 3);
        b.nextLine();
        b.append(sp);
        b.append(bb.getPanel());
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        panel.add(b.getPanel(), BorderLayout.CENTER);

        addFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addEntry(false);
            }
        });
        addDir.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                addEntry(true);
            }
        });
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                removeSelected();
            }
        });
    }

    public JPanel getPanel() {
        return panel;
    }

    private void addEntry(boolean directory) {
        File initValue = new File(Globals.prefs.get("ooStyleFileLastDir"));
        JFileChooser jfc = new JFileChooser(initValue);
        if (directory)
            jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else {
            // Set file filter:
            if (styleFileFilter == null) {
                styleFileFilter = new FileFilter() {
                    public boolean accept(File file) {
                        return file.isDirectory() ||
                                file.getName().endsWith(StyleSelectDialog.STYLE_FILE_EXTENSION);
                    }
                    public String getDescription() {
                        return StyleSelectDialog.STYLE_FILE_EXTENSION;
                    }
                };
            }
            jfc.addChoosableFileFilter(styleFileFilter);
        }
        if (jfc.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            Globals.prefs.put("ooStyleFileLastDir", jfc.getSelectedFile().getPath());
            DirElement elm = new DirElement(jfc.getSelectedFile().getPath(), false, directory);
            entries.getReadWriteLock().writeLock().lock();
            if (!entries.contains(elm)) // prevent dupes
                entries.add(elm);
            entries.getReadWriteLock().writeLock().unlock();
            notifyListeners();
        }
    }

    private void removeSelected() {
        entries.getReadWriteLock().writeLock().lock();
        for (DirElement elm : selectionModel.getSelected()) {
            entries.remove(elm);
        }
        entries.getReadWriteLock().writeLock().unlock();
        notifyListeners();
    }

    /**
     * Get a list of dir elements describing the current selection.
     * @return
     */
    public List<DirElement> getDirElements() {
        return Collections.unmodifiableList(entries);
    }

    private void setValues(String[] dirs) {
        entries.getReadWriteLock().writeLock().lock();
        entries.clear();
        for (int i=0; i<dirs.length-1; i+=2) {
            File tmp = new File(dirs[i]);
            entries.add(new DirElement(dirs[i], Boolean.parseBoolean(dirs[i+1]),
                    tmp.isDirectory()));
        }
        entries.getReadWriteLock().writeLock().unlock();
    }

    /**
     * Get a String array describing the current selection.
     * @return A String array containing two elements per file/directory:
     *   first the path, then the string "true" or "false", determining whether
     *   the element should be handled recursively.
     */
    public String[] getStringArray() {
        entries.getReadWriteLock().readLock().lock();
        String[] res = new String[entries.size()*2];
        int i=0;
        for (DirElement elm : entries) {
            res[i++] = elm.path;
            res[i++] = elm.recursive ? "true" : "false";
        }
        entries.getReadWriteLock().readLock().unlock();
        return res;
    }

    /**
     * Add a listener that will be notified that the directory setup has
     * been changed.
     * @param listener The listener.
     */
    public void addActionListener(ActionListener listener) {
        listeners.add(listener);
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (ActionListener listener : listeners)
            listener.actionPerformed(new ActionEvent(StyleDirectoriesPanel.this, 0, "list change"));
    }

    /**
     * Wrapper class for presenting the entries as a table with checkboxes for
     * recursiveness.
     */
    public static class DirElement implements Comparable {
        public String path;
        public boolean recursive, canBeRecursive;
        public DirElement(String path, boolean recursive, boolean canBeRecursive) {
            this.path = path;
            this.recursive = recursive;
            this.canBeRecursive = canBeRecursive;
        }

        public int compareTo(Object o) {
            DirElement other = (DirElement)o;
            return path.compareToIgnoreCase(other.path);
        }

        public boolean equals(Object o) {
            DirElement other = (DirElement)o;
            return other.path.equals(path);
        }
    }

    class DirElementFormat implements WritableTableFormat<DirElement>,
            AdvancedTableFormat<DirElement> {

        public Class getColumnClass(int i) {
            if (i == 0) return String.class;
            else return Boolean.class;
        }

        public Comparator getColumnComparator(int i) {
            return null;
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int i) {
            if (i == 0)
                return Globals.lang("Directory or file");
            else return Globals.lang("Include subdirectories");
        }

        public Object getColumnValue(DirElement dirElement, int i) {
            if (i == 0) return dirElement.path;
            else return dirElement.recursive;
        }

        public boolean isEditable(DirElement elm, int i) {
            return (i == 1) && (elm.canBeRecursive);
        }

        public DirElement setColumnValue(DirElement elm, Object o, int col) {
            if (col == 1) {
                elm.recursive = (Boolean)o;
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        notifyListeners();
                    }
                });
            }
            return elm;
        }

    }

}
