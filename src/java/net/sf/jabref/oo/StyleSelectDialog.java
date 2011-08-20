package net.sf.jabref.oo;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventSelectionModel;
import ca.odell.glazedlists.swing.EventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.uif_lite.component.UIFSplitPane;
import net.sf.jabref.*;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;

import javax.swing.*;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

/**
 * This class produces a dialog box for choosing a style file.
 */
public class StyleSelectDialog {

    public static final String STYLE_FILE_EXTENSION = ".jstyle";
    private JabRefFrame frame;
    private EventList<OOBibStyle> styles, sortedStyles;
    private JDialog diag;
    private JTable table;
    private UIFSplitPane contentPane = new UIFSplitPane(UIFSplitPane.VERTICAL_SPLIT);
    private EventTableModel tableModel;
    private EventSelectionModel<OOBibStyle> selectionModel;
    private JPopupMenu popup = new JPopupMenu();
    private JMenuItem edit = new JMenuItem(Globals.lang("Edit"));

    PreviewPanel preview;
    StyleDirectoriesPanel dirsPanel;

    private Rectangle toRect = new Rectangle(0, 0, 1, 1);
    private JButton ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));
    private BibtexEntry prevEntry = new BibtexEntry(Util.createNeutralId());

    private boolean okPressed = false;
    private String initSelection;

    public StyleSelectDialog(JabRefFrame frame, String initSelection) {

        this.frame = frame;
        setupPrevEntry();
        init(initSelection);
    }

    private void init(String initSelection) {
        this.initSelection = initSelection;

        popup.add(edit);
        // Add action listener to "Edit" menu item, which is supposed to open the style file in an external editor:
        edit.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                int i = table.getSelectedRow();
                if (i == -1)
                    return;
                ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("jstyle");
                String link = ((OOBibStyle)tableModel.getElementAt(i)).getFile().getPath();
                try {
                    if (type != null)
                        Util.openExternalFileAnyFormat(new MetaData(), link, type);
                    else
                        Util.openExternalFileUnknown(frame, null, new MetaData(), link,
                                new UnknownExternalFileType("jstyle"));
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        });

        diag = new JDialog(frame, Globals.lang("Styles"), true);
        dirsPanel = new StyleDirectoriesPanel(diag, Globals.prefs.getStringArray("ooStyleFileDirectories"));
        // The dirs panel is used to change the list of files and directories to include.
        // We register an ActionListener to update the style table when the list changes:
        dirsPanel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                readStyles();
            }
        });
        styles = new BasicEventList<OOBibStyle>();
        sortedStyles = new SortedList<OOBibStyle>(styles);

        // Create a preview panel for previewing styles:
        preview = new PreviewPanel(null, new MetaData(), "");
        // Use the test entry from the Preview settings tab in Preferences:
        preview.setEntry(prevEntry);//PreviewPrefsTab.getTestEntry());


        tableModel = new EventTableModel<OOBibStyle>(sortedStyles, new StyleTableFormat());
        table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(1).setPreferredWidth(200);
        cm.getColumn(2).setPreferredWidth(80);
        selectionModel = new EventSelectionModel<OOBibStyle>(sortedStyles);
        table.setSelectionModel(selectionModel);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger())
                    tablePopup(mouseEvent);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger())
                    tablePopup(mouseEvent);
            }
        });
        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());
        contentPane.setTopComponent(new JScrollPane(table));
        contentPane.setBottomComponent(preview);

        readStyles();
        

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("fill:1dlu:grow",//""));
                //"fill:1dlu:grow,
                 "fill:pref, fill:pref, fill:270dlu:grow"));
        b.appendSeparator(Globals.lang("Select style"));
        b.nextLine();
        b.append(new JLabel("<html>"+Globals.lang("This is the list of available styles. Select the one you want to use.")+"</html>"));
        b.nextLine();
        b.append(contentPane);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.add(b.getPanel(), BorderLayout.CENTER);
        diag.add(dirsPanel.getPanel(), BorderLayout.NORTH);

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                okPressed = true;
                storeSettings();
                diag.dispose();
            }
        });
        Action cancelListener = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelListener);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        diag.add(bb.getPanel(), BorderLayout.SOUTH);

        ActionMap am = bb.getPanel().getActionMap();
        InputMap im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelListener);
        
        diag.pack();
        diag.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                contentPane.setDividerLocation(contentPane.getSize().height-150);
            }
        });

    }

    public void setVisible(boolean visible) {
        okPressed = false;
        diag.setVisible(visible);
    }

    /**
     * Read all style files or directories of style files indicated by the current
     * settings, and add the styles to the list of styles.
     */
    private void readStyles() {
        table.clearSelection();

        styles.getReadWriteLock().writeLock().lock();
        styles.clear();
        for (StyleDirectoriesPanel.DirElement elm : dirsPanel.getDirElements()) {
            addStyles(elm.path, elm.recursive);
        }
        styles.getReadWriteLock().writeLock().unlock();

        selectLastUsed();
    }

    /**
     * This method scans the current list of styles, and looks for the styles
     * that was last used. If found, that style is selected. If not found,
     * the first style is selected provided there are >0 styles.
     */
    private void selectLastUsed() {
        // Set the initial selection of the table:
        if (initSelection != null) {
            boolean found = false;
            for (int i=0; i < table.getRowCount(); i++) {
                if (((OOBibStyle)tableModel.getElementAt(i)).getFile().getPath().
                        equals(initSelection)) {
                    table.setRowSelectionInterval(i,i);
                    found = true;
                    break;
                }
            }
            if (!found && (table.getRowCount() > 0))
                table.setRowSelectionInterval(0,0);
        }
        else {
            if (table.getRowCount() > 0)
                table.setRowSelectionInterval(0,0);
        }
    }

    /**
     * If the string dir indicates a file, parse it and add it to the list of styles if
     * successful. If the string dir indicates a directory, parse all files looking like
     * style files, and add them. The parameter recurse determines whether we should
     * recurse into subdirectories.
     * @param dir the directory or file to handle.
     * @param recurse true indicates that we should recurse into subdirectories.
     */
    private void addStyles(String dir, boolean recurse) {
        File dirF = new File(dir);
        if (dirF.isDirectory()) {
            File[] files = dirF.listFiles();
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                // If the file looks like a style file, parse it:
                if (!file.isDirectory() && (file.getName().endsWith(STYLE_FILE_EXTENSION))) {
                    addSingleFile(file);
                }
                // If the file is a directory, and we should recurse, do:
                else if (file.isDirectory() && recurse) {
                    addStyles(file.getPath(), recurse);
                }
            }
        }
        else {
            // The file wasn't a directory, so we simply parse it:
            addSingleFile(dirF);
        }
    }

    /**
     * Parse a single file, and add it to the list of styles if parse was successful.
     * @param file the file to parse.
     */
    private void addSingleFile(File file) {
        try {
            OOBibStyle style = new OOBibStyle(file);
            // Check if the parse was successful before adding it:
            if (style.isValid() && !styles.contains(style))
                styles.add(style);
        } catch (Exception e) {
            System.out.println("Unable to read style file: '"+file.getPath()+"'");
            e.printStackTrace();
        }
    }

    public void storeSettings() {
        String[] dirs = dirsPanel.getStringArray();
        Globals.prefs.putStringArray("ooStyleFileDirectories", dirs);
        OOBibStyle selected = getSelectedStyle();
        if (selected != null)
            Globals.prefs.put("ooBibliographyStyleFile", selected.getFile().getPath());
    }

    /**
     * Get the currently selected style.
     * @return the selected style, or null if no style is selected.
     */
    public OOBibStyle getSelectedStyle() {
        if (selectionModel.getSelected().size() > 0)
            return selectionModel.getSelected().get(0);
        else
            return null;
    }


    private void setupPrevEntry() {
        prevEntry.setField("author", "Smith, Bill and Jones, Bob and Williams, Jeff");
        prevEntry.setField("editor", "Taylor, Phil");
        prevEntry.setField("title", "Title of the test entry for reference styles");
        prevEntry.setField("volume", "34");
        prevEntry.setField("year", "2008");
        prevEntry.setField("journal", "BibTeX journal");
        prevEntry.setField("publisher", "JabRef publishing");
        prevEntry.setField("address", "Trondheim");
        prevEntry.setField("www", "http://jabref.sf.net");
    }

    static class StyleTableFormat implements TableFormat<OOBibStyle> {

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int i) {
            switch (i) {
                case 0:
                    return Globals.lang("Name");
                case 1:
                    return Globals.lang("Journals");
                case 2:
                    return Globals.lang("File");
                default:
                    return "";
            }
        }

        public Object getColumnValue(OOBibStyle style, int i) {
            switch (i) {
                case 0:
                    return style.getName();
                case 1:
                    return formatJournals(style.getJournals());
                case 2:
                    return style.getFile().getName();
                default:
                    return "";
            }
        }


        private String formatJournals(Set<String> journals) {
            StringBuilder sb = new StringBuilder("");
            for (Iterator<String> i = journals.iterator(); i.hasNext();) {
                sb.append(i.next());
                if (i.hasNext())
                    sb.append(", ");
            }
            return sb.toString();    
        }
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    protected void tablePopup(MouseEvent e) {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    /**
     * The listener for the Glazed list monitoring the current selection.
     * When selection changes, we need to update the preview panel.
     */
    class EntrySelectionListener implements ListEventListener<OOBibStyle> {

        public void listChanged(ListEvent<OOBibStyle> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                OOBibStyle style = listEvent.getSourceList().get(0);
                initSelection = style.getFile().getPath();
                preview.setLayout(style.getReferenceFormat("default"));
                // Update the preview's entry:
                contentPane.setDividerLocation(contentPane.getSize().height-150); 
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        preview.update();
                        preview.scrollRectToVisible(toRect);
                    }
                });
            }
        }
    }

}
