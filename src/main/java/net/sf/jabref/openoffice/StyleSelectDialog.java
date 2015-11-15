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
package net.sf.jabref.openoffice;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Iterator;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumnModel;

import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.Globals;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.JabRef;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.MetaData;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.UnknownExternalFileType;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.gui.keyboard.KeyBinds;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import ca.odell.glazedlists.swing.GlazedListsSwing;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class produces a dialog box for choosing a style file.
 */
class StyleSelectDialog {

    private static final String STYLE_FILE_EXTENSION = ".jstyle";
    private final JabRefFrame frame;
    private EventList<OOBibStyle> styles;
    private JDialog diag;
    private JTable table;
    private final JSplitPane contentPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    private DefaultEventTableModel<OOBibStyle> tableModel;
    private DefaultEventSelectionModel<OOBibStyle> selectionModel;
    private final JPopupMenu popup = new JPopupMenu();
    private final JMenuItem edit = new JMenuItem(Localization.lang("Edit"));
    private final JRadioButton useDefaultAuthoryear = new JRadioButton(Localization.lang("Default style (author-year citations)"));
    private final JRadioButton useDefaultNumerical = new JRadioButton(Localization.lang("Default style (numerical citations)"));
    private final JRadioButton chooseDirectly = new JRadioButton(Localization.lang("Choose style file directly") + ":");
    private final JRadioButton setDirectory = new JRadioButton(Localization.lang("Choose from a directory") + ":");
    private final JTextField directFile = new JTextField();
    private final JTextField styleDir = new JTextField();
    private final JButton browseDirectFile = new JButton(Localization.lang("Browse"));
    private final JButton browseStyleDir = new JButton(Localization.lang("Browse"));
    private final JButton showDefaultAuthoryearStyle = new JButton(Localization.lang("View"));
    private final JButton showDefaultNumericalStyle = new JButton(Localization.lang("View"));

    private PreviewPanel preview;

    private final Rectangle toRect = new Rectangle(0, 0, 1, 1);
    private final JButton ok = new JButton(Localization.lang("Ok"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final BibtexEntry prevEntry = new BibtexEntry(IdGenerator.next());

    private boolean okPressed;
    private String initSelection;


    public StyleSelectDialog(JabRefFrame frame, String initSelection) {

        this.frame = frame;
        setupPrevEntry();
        init(initSelection);
    }

    private void init(String inSelection) {
        this.initSelection = inSelection;

        ButtonGroup bg = new ButtonGroup();
        bg.add(useDefaultAuthoryear);
        bg.add(useDefaultNumerical);
        bg.add(chooseDirectly);
        bg.add(setDirectory);
        if (Globals.prefs.getBoolean("ooUseDefaultAuthoryearStyle")) {
            useDefaultAuthoryear.setSelected(true);
        } else if (Globals.prefs.getBoolean("ooUseDefaultNumericalStyle")) {
            useDefaultNumerical.setSelected(true);
        } else {
            if (Globals.prefs.getBoolean("ooChooseStyleDirectly")) {
                chooseDirectly.setSelected(true);
            } else {
                setDirectory.setSelected(true);
            }
        }

        directFile.setText(Globals.prefs.get("ooDirectFile"));
        styleDir.setText(Globals.prefs.get("ooStyleDirectory"));
        directFile.setEditable(false);
        styleDir.setEditable(false);

        popup.add(edit);

        BrowseAction dfBrowse = BrowseAction.buildForFile(directFile, directFile);
        browseDirectFile.addActionListener(dfBrowse);

        BrowseAction sdBrowse = BrowseAction.buildForDir(styleDir, setDirectory);
        browseStyleDir.addActionListener(sdBrowse);

        showDefaultAuthoryearStyle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                displayDefaultStyle(true);
            }
        });
        showDefaultNumericalStyle.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                displayDefaultStyle(false);
            }
        });
        // Add action listener to "Edit" menu item, which is supposed to open the style file in an external editor:
        edit.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int i = table.getSelectedRow();
                if (i == -1) {
                    return;
                }
                ExternalFileType type = Globals.prefs.getExternalFileTypeByExt("jstyle");
                String link = tableModel.getElementAt(i).getFile().getPath();
                try {
                    if (type != null) {
                        JabRefDesktop.openExternalFileAnyFormat(new MetaData(), link, type);
                    } else {
                        JabRefDesktop.openExternalFileUnknown(frame, null, new MetaData(), link,
                                new UnknownExternalFileType("jstyle"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        });

        diag = new JDialog(frame, Localization.lang("Styles"), true);

        styles = new BasicEventList<>();
        EventList<OOBibStyle> sortedStyles = new SortedList<>(styles);

        // Create a preview panel for previewing styles:
        preview = new PreviewPanel(null, new MetaData(), "");
        // Use the test entry from the Preview settings tab in Preferences:
        preview.setEntry(prevEntry);//PreviewPrefsTab.getTestEntry());

        tableModel = (DefaultEventTableModel<OOBibStyle>) GlazedListsSwing
                .eventTableModelWithThreadProxyList(sortedStyles, new StyleTableFormat());
        table = new JTable(tableModel);
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(100);
        cm.getColumn(1).setPreferredWidth(200);
        cm.getColumn(2).setPreferredWidth(80);
        selectionModel = (DefaultEventSelectionModel<OOBibStyle>) GlazedListsSwing
                .eventSelectionModelWithThreadProxyList(sortedStyles);
        table.setSelectionModel(selectionModel);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    tablePopup(mouseEvent);
                }
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                if (mouseEvent.isPopupTrigger()) {
                    tablePopup(mouseEvent);
                }
            }
        });

        selectionModel.getSelected().addListEventListener(new EntrySelectionListener());

        styleDir.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                readStyles();
                setDirectory.setSelected(true);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                readStyles();
                setDirectory.setSelected(true);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                readStyles();
                setDirectory.setSelected(true);
            }
        });
        directFile.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent documentEvent) {
                chooseDirectly.setSelected(true);
            }

            @Override
            public void removeUpdate(DocumentEvent documentEvent) {
                chooseDirectly.setSelected(true);
            }

            @Override
            public void changedUpdate(DocumentEvent documentEvent) {
                chooseDirectly.setSelected(true);
            }
        });

        contentPane.setTopComponent(new JScrollPane(table));
        contentPane.setBottomComponent(preview);

        readStyles();

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("fill:pref,4dlu,fill:150dlu,4dlu,fill:pref", ""));
        b.append(useDefaultAuthoryear, 3);
        b.append(showDefaultAuthoryearStyle);
        b.nextLine();
        b.append(useDefaultNumerical, 3);
        b.append(showDefaultNumericalStyle);
        b.nextLine();
        b.append(chooseDirectly);
        b.append(directFile);
        b.append(browseDirectFile);
        b.nextLine();
        b.append(setDirectory);
        b.append(styleDir);
        b.append(browseStyleDir);
        b.nextLine();
        DefaultFormBuilder b2 = new DefaultFormBuilder(new FormLayout("fill:1dlu:grow",
                "fill:pref, fill:pref, fill:270dlu:grow"));

        b2.nextLine();
        b2.append(new JLabel("<html>" + Localization.lang("This is the list of available styles. Select the one you want to use.") + "</html>"));
        b2.nextLine();
        b2.append(contentPane);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        b2.getPanel().setBorder(BorderFactory.createEmptyBorder(15, 5, 5, 5));
        diag.add(b.getPanel(), BorderLayout.NORTH);
        diag.add(b2.getPanel(), BorderLayout.CENTER);

        AbstractAction okListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                if (!useDefaultAuthoryear.isSelected() && !useDefaultNumerical.isSelected()) {
                    if (chooseDirectly.isSelected()) {
                        File f = new File(directFile.getText());
                        if (!f.exists()) {
                            JOptionPane.showMessageDialog(diag, Localization.lang("You must select either a valid style file, or use a default style."),
                                    Localization.lang("Style selection"), JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                    else {
                        if ((table.getRowCount() == 0) || (table.getSelectedRowCount() == 0)) {
                            JOptionPane.showMessageDialog(diag, Localization.lang("You must select either a valid style file, or use a default style."),
                                    Localization.lang("Style selection"), JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                    }
                }
                okPressed = true;
                storeSettings();
                diag.dispose();
            }
        };
        ok.addActionListener(okListener);

        Action cancelListener = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent event) {
                diag.dispose();
            }
        };
        cancel.addActionListener(cancelListener);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.add(bb.getPanel(), BorderLayout.SOUTH);

        ActionMap am = bb.getPanel().getActionMap();
        InputMap im = bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey(KeyBinds.CLOSE_DIALOG), "close");
        am.put("close", cancelListener);
        im.put(KeyStroke.getKeyStroke("ENTER"), "enterOk");
        am.put("enterOk", okListener);

        diag.pack();
        diag.setLocationRelativeTo(frame);
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                contentPane.setDividerLocation(contentPane.getSize().height - 150);
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
        if (!styleDir.getText().isEmpty()) {
            addStyles(styleDir.getText(), true);
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
            for (int i = 0; i < table.getRowCount(); i++) {
                if (tableModel.getElementAt(i).getFile().getPath().
                        equals(initSelection)) {
                    table.setRowSelectionInterval(i, i);
                    found = true;
                    break;
                }
            }
            if (!found && (table.getRowCount() > 0)) {
                table.setRowSelectionInterval(0, 0);
            }
        }
        else {
            if (table.getRowCount() > 0) {
                table.setRowSelectionInterval(0, 0);
            }
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
            for (File file : files) {
                // If the file looks like a style file, parse it:
                if (!file.isDirectory() && (file.getName().endsWith(StyleSelectDialog.STYLE_FILE_EXTENSION))) {
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
            if (style.isValid() && !styles.contains(style)) {
                styles.add(style);
            }
        } catch (Exception e) {
            System.out.println("Unable to read style file: '" + file.getPath() + "'");
            e.printStackTrace();
        }
    }

    private void storeSettings() {
        OOBibStyle selected = getSelectedStyle();
        Globals.prefs.putBoolean("ooUseDefaultAuthoryearStyle", useDefaultAuthoryear.isSelected());
        Globals.prefs.putBoolean("ooUseDefaultNumericalStyle", useDefaultNumerical.isSelected());
        Globals.prefs.putBoolean("ooChooseStyleDirectly", chooseDirectly.isSelected());
        Globals.prefs.put("ooDirectFile", directFile.getText());
        Globals.prefs.put("ooStyleDirectory", styleDir.getText());
        if (chooseDirectly.isSelected()) {
            Globals.prefs.put("ooBibliographyStyleFile", directFile.getText());
        }
        else if (setDirectory.isSelected() && (selected != null)) {
            Globals.prefs.put("ooBibliographyStyleFile", selected.getFile().getPath());
        }

    }

    /**
     * Get the currently selected style.
     * @return the selected style, or null if no style is selected.
     */
    private OOBibStyle getSelectedStyle() {
        if (!selectionModel.getSelected().isEmpty()) {
            return selectionModel.getSelected().get(0);
        }
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
        prevEntry.setField("www", "https://github.com/JabRef");
    }


    static class StyleTableFormat implements TableFormat<OOBibStyle> {

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int i) {
            switch (i) {
            case 0:
                return Localization.lang("Name");
            case 1:
                return Localization.lang("Journals");
            case 2:
                return Localization.lang("File");
            default:
                return "";
            }
        }

        @Override
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

        private static String formatJournals(Set<String> journals) {
            StringBuilder sb = new StringBuilder("");
            for (Iterator<String> i = journals.iterator(); i.hasNext();) {
                sb.append(i.next());
                if (i.hasNext()) {
                    sb.append(", ");
                }
            }
            return sb.toString();
        }
    }


    public boolean isOkPressed() {
        return okPressed;
    }

    private void tablePopup(MouseEvent e) {
        popup.show(e.getComponent(), e.getX(), e.getY());
    }

    private void displayDefaultStyle(boolean authoryear) {
        try {
            // Read the contents of the default style file:
            URL defPath = authoryear ? JabRef.class.getResource(OpenOfficePanel.defaultAuthorYearStylePath) :
                JabRef.class.getResource(OpenOfficePanel.defaultNumericalStylePath);
            BufferedReader r = new BufferedReader(new InputStreamReader(defPath.openStream()));
            String line;
            StringBuilder sb = new StringBuilder();
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }

            // Make a dialog box to display the contents:
            final JDialog dd = new JDialog(diag, Localization.lang("Default style"), true);
            JLabel header = new JLabel("<html>" + Localization.lang("The panel below shows the definition of the default style.")
            //+"<br>"
            + Localization.lang("If you want to use it as a template for a new style, you can copy the contents into a new .jstyle file")
            + "</html>");

            header.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            dd.getContentPane().add(header, BorderLayout.NORTH);
            JTextArea ta = new JTextArea(sb.toString());
            ta.setEditable(false);
            JScrollPane sp = new JScrollPane(ta);
            sp.setPreferredSize(new Dimension(700, 500));
            dd.getContentPane().add(sp, BorderLayout.CENTER);
            JButton okButton = new JButton(Localization.lang("Ok"));
            ButtonBarBuilder bb = new ButtonBarBuilder();
            bb.addGlue();
            bb.addButton(okButton);
            bb.addGlue();
            bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            dd.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
            okButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent actionEvent) {
                    dd.dispose();
                }
            });
            dd.pack();
            dd.setLocationRelativeTo(diag);
            dd.setVisible(true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }


    /**
     * The listener for the Glazed list monitoring the current selection.
     * When selection changes, we need to update the preview panel.
     */
    private class EntrySelectionListener implements ListEventListener<OOBibStyle> {

        @Override
        public void listChanged(ListEvent<OOBibStyle> listEvent) {
            if (listEvent.getSourceList().size() == 1) {
                OOBibStyle style = listEvent.getSourceList().get(0);
                initSelection = style.getFile().getPath();
                preview.setLayout(style.getReferenceFormat("default"));
                // Update the preview's entry:
                contentPane.setDividerLocation(contentPane.getSize().height - 150);
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        preview.update();
                        preview.scrollRectToVisible(toRect);
                    }
                });
            }
        }
    }

}
