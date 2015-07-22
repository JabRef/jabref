/*  Copyright (C) 2003-2014 JabRef contributors.
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
package net.sf.jabref.journals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.help.HelpAction;
import net.sf.jabref.journals.logic.Abbreviation;
import net.sf.jabref.journals.logic.JournalAbbreviationRepository;
import net.sf.jabref.net.URLDownload;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 19, 2005
 * Time: 7:57:29 PM
 * To browseOld this template use File | Settings | File Templates.
 */
class ManageJournalsPanel extends JPanel {

    private final JabRefFrame frame;
    private final JTextField personalFile = new JTextField();
    private final AbbreviationsTableModel tableModel = new AbbreviationsTableModel();
    private JTable userTable; // builtInTable
    private final JPanel userPanel = new JPanel();
    private final JPanel journalEditPanel;
    private final JPanel externalFilesPanel = new JPanel();
    private final JPanel addExtPan = new JPanel();
    private final JTextField nameTf = new JTextField();
    private final JTextField newNameTf = new JTextField();
    private final JTextField abbrTf = new JTextField();
    private final List<ExternalFileEntry> externals = new ArrayList<ExternalFileEntry>(); // To hold references to external journal lists.
    private final JDialog dialog;
    private final JRadioButton newFile = new JRadioButton(Globals.lang("New file"));
    private final JRadioButton oldFile = new JRadioButton(Globals.lang("Existing file"));

    private final JButton add = new JButton(GUIGlobals.getImage("add"));
    private final JButton remove = new JButton(GUIGlobals.getImage("remove"));


    public ManageJournalsPanel(final JabRefFrame frame) {
        this.frame = frame;

        personalFile.setEditable(false);

        ButtonGroup group = new ButtonGroup();
        group.add(newFile);
        group.add(oldFile);
        addExtPan.setLayout(new BorderLayout());
        JButton addExt = new JButton(GUIGlobals.getImage("add"));
        addExtPan.add(addExt, BorderLayout.EAST);
        addExtPan.setToolTipText(Globals.lang("Add"));
        //addExtPan.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:200dlu:grow, 4dlu, fill:pref",// 4dlu, left:pref, 4dlu",
                "pref, pref, pref, 20dlu, 20dlu, fill:200dlu, 4dlu, pref");//150dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        CellConstraints cc = new CellConstraints();

        /*JLabel description = new JLabel("<HTML>"+Glbals.lang("JabRef can switch journal names between "
            +"abbreviated and full form. Since it knows only a limited number of journal names, "
            +"you may need to add your own definitions.")+"</HTML>");*/
        builder.addSeparator(Globals.lang("Built-in journal list"), cc.xyw(2, 1, 6));
        JLabel description = new JLabel("<HTML>" + Globals.lang("JabRef includes a built-in list of journal abbreviations.")
                + "<br>" + Globals.lang("You can add additional journal names by setting up a personal journal list,<br>as "
                        + "well as linking to external journal lists.") + "</HTML>");
        description.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        builder.add(description, cc.xyw(2, 2, 6));
        JButton viewBuiltin = new JButton(Globals.lang("View"));
        builder.add(viewBuiltin, cc.xy(7, 2));
        builder.addSeparator(Globals.lang("Personal journal list"), cc.xyw(2, 3, 6));

        //builder.add(description, cc.xyw(2,1,6));
        builder.add(newFile, cc.xy(3, 4));
        builder.add(newNameTf, cc.xy(5, 4));
        JButton browseNew = new JButton(Globals.lang("Browse"));
        builder.add(browseNew, cc.xy(7, 4));
        builder.add(oldFile, cc.xy(3, 5));
        builder.add(personalFile, cc.xy(5, 5));
        //BrowseAction action = new BrowseAction(personalFile, false);
        //JButton browse = new JButton(Globals.lang("Browse"));
        //browse.addActionListener(action);
        JButton browseOld = new JButton(Globals.lang("Browse"));
        builder.add(browseOld, cc.xy(7, 5));

        userPanel.setLayout(new BorderLayout());
        //builtInTable = new JTable(Globals.journalAbbrev.getTableModel());
        builder.add(userPanel, cc.xyw(2, 6, 4));
        ButtonStackBuilder butBul = new ButtonStackBuilder();
        butBul.addButton(add);
        butBul.addButton(remove);

        butBul.addGlue();
        builder.add(butBul.getPanel(), cc.xy(7, 6));

        builder.addSeparator(Globals.lang("External files"), cc.xyw(2, 8, 6));
        externalFilesPanel.setLayout(new BorderLayout());
        //builder.add(/*new JScrollPane(*/externalFilesPanel/*)*/, cc.xyw(2,8,6));

        setLayout(new BorderLayout());
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));//createMatteBorder(1,1,1,1,Color.green));
        add(builder.getPanel(), BorderLayout.NORTH);
        add(externalFilesPanel, BorderLayout.CENTER);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton ok = new JButton(Globals.lang("Ok"));
        bb.addButton(ok);
        JButton cancel = new JButton(Globals.lang("Cancel"));
        bb.addButton(cancel);
        bb.addUnrelatedGap();
        JButton help = new JButton(Globals.lang("Help"));
        bb.addButton(help);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dialog = new JDialog(frame, Globals.lang("Journal abbreviations"), false);
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        //add(new JScrollPane(builtInTable), BorderLayout.CENTER);

        // Set up panel for editing a single journal, to be used in a dialog box:
        FormLayout layout2 = new FormLayout
                ("right:pref, 4dlu, fill:180dlu", "");
        DefaultFormBuilder builder2 = new DefaultFormBuilder(layout2);
        builder2.append(Globals.lang("Journal name"));
        builder2.append(nameTf);
        builder2.nextLine();
        builder2.append(Globals.lang("ISO abbreviation"));
        builder2.append(abbrTf);
        journalEditPanel = builder2.getPanel();

        viewBuiltin.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JournalAbbreviationRepository abbr = new JournalAbbreviationRepository();
                abbr.readJournalListFromResource(Globals.JOURNALS_FILE_BUILTIN);
                JTable table = new JTable(JournalAbbreviationsUtil.getTableModel(Globals.journalAbbrev));
                JScrollPane pane = new JScrollPane(table);
                JOptionPane.showMessageDialog(null, pane, Globals.lang("Journal list preview"), JOptionPane.INFORMATION_MESSAGE);
            }
        });

        browseNew.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                File old = null;
                if (!newNameTf.getText().equals("")) {
                    old = new File(newNameTf.getText());
                }
                String name = FileDialogs.getNewFile(frame, old, null, JFileChooser.SAVE_DIALOG, false);
                if (name != null) {
                    newNameTf.setText(name);
                    newFile.setSelected(true);
                }
            }
        });
        browseOld.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                File old = null;
                if (!personalFile.getText().equals("")) {
                    old = new File(personalFile.getText());
                }
                String name = FileDialogs.getNewFile(frame, old, null, JFileChooser.OPEN_DIALOG, false);
                if (name != null) {
                    personalFile.setText(name);
                    oldFile.setSelected(true);
                    oldFile.setEnabled(true);
                    setupUserTable();
                }
            }
        });

        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (readyToClose()) {
                    try {
                        storeSettings();
                        dialog.dispose();
                    } catch (FileNotFoundException ex) {
                        JOptionPane.showMessageDialog(null, Globals.lang("Error opening file") + ": " + ex.getMessage(), Globals.lang("Error opening file"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.journalAbbrHelp));

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        add.addActionListener(tableModel);
        remove.addActionListener(tableModel);
        addExt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                externals.add(new ExternalFileEntry());
                buildExternalsPanel();
            }
        });

        // Key bindings:
        ActionMap am = getActionMap();
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", cancelAction);

        //dialog.pack();
        int xSize = getPreferredSize().width;
        dialog.setSize(xSize + 10, 700);
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setValues() {
        personalFile.setText(Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST));
        if (personalFile.getText().length() == 0) {
            newFile.setSelected(true);
            oldFile.setEnabled(false);
        } else {
            oldFile.setSelected(true);
            oldFile.setEnabled(true);
        }
        setupUserTable();
        setupExternals();
        buildExternalsPanel();

    }

    private void buildExternalsPanel() {

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:pref:grow", ""));
        for (ExternalFileEntry efe : externals) {
            builder.append(efe.getPanel());
            builder.nextLine();
        }
        builder.append(Box.createVerticalGlue());
        builder.nextLine();
        builder.append(addExtPan);
        builder.nextLine();
        builder.append(Box.createVerticalGlue());

        //builder.getPanel().setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.green));
        //externalFilesPanel.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        JScrollPane pane = new JScrollPane(builder.getPanel());
        pane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        externalFilesPanel.setMinimumSize(new Dimension(400, 400));
        externalFilesPanel.setPreferredSize(new Dimension(400, 400));
        externalFilesPanel.removeAll();
        externalFilesPanel.add(pane, BorderLayout.CENTER);
        externalFilesPanel.revalidate();
        externalFilesPanel.repaint();

    }

    private void setupExternals() {
        String[] externalFiles = Globals.prefs.getStringArray(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        if ((externalFiles == null) || (externalFiles.length == 0)) {
            ExternalFileEntry efe = new ExternalFileEntry();
            externals.add(efe);
        } else {
            for (String externalFile : externalFiles) {
                ExternalFileEntry efe = new ExternalFileEntry(externalFile);
                externals.add(efe);

            }

        }

        //efe = new ExternalFileEntry();
        //externals.add(efe);

    }

    private void setupUserTable() {
        JournalAbbreviationRepository userAbbr = new JournalAbbreviationRepository();
        String filename = personalFile.getText();
        if (!filename.equals("") && (new File(filename)).exists()) {
            try {
                userAbbr.readJournalListFromFile(new File(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        tableModel.setJournals(userAbbr.getAbbreviations());
        userTable = new JTable(tableModel);
        userTable.addMouseListener(tableModel.getMouseListener());
        userPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
    }

    private boolean readyToClose() {
        File f;
        if (newFile.isSelected()) {
            if (newNameTf.getText().length() > 0) {
                f = new File(newNameTf.getText());
                return (!f.exists() || (JOptionPane.showConfirmDialog
                        (this, "'" + f.getName() + "' " + Globals.lang("exists. Overwrite file?"),
                                Globals.lang("Store journal abbreviations"), JOptionPane.OK_CANCEL_OPTION)
                == JOptionPane.OK_OPTION));
            } else {
                if (tableModel.getRowCount() > 0) {
                    JOptionPane.showMessageDialog(this, Globals.lang("You must choose a file name to store journal abbreviations"),
                            Globals.lang("Store journal abbreviations"), JOptionPane.ERROR_MESSAGE);
                    return false;
                } else {
                    return true;
                }

            }
        }
        return true;
    }

    private void storeSettings() throws FileNotFoundException {
        File f = null;
        if (newFile.isSelected()) {
            if (newNameTf.getText().length() > 0) {
                f = new File(newNameTf.getText());
            }// else {
             //    return; // Nothing to do.
             //}
        } else {
            f = new File(personalFile.getText());
        }

        if (f != null) {
            if (!f.exists()) {
                throw new FileNotFoundException(f.getAbsolutePath());
            }
            FileWriter fw = null;
            try {
                fw = new FileWriter(f, false);
                for (JournalEntry entry : tableModel.getJournals()) {
                    fw.write(entry.name);
                    fw.write(" = ");
                    fw.write(entry.abbreviation);
                    fw.write(Globals.NEWLINE);
                }

            } catch (IOException e) {
                e.printStackTrace();

            } finally {
                if (fw != null) {
                    try {
                        fw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            String filename = f.getPath();
            if (filename.equals("")) {
                filename = null;
            }
            Globals.prefs.put(JabRefPreferences.PERSONAL_JOURNAL_LIST, filename);
        }

        // Store the list of external files set up:
        ArrayList<String> extFiles = new ArrayList<String>();
        for (ExternalFileEntry efe : externals) {
            if (!efe.getValue().equals("")) {
                extFiles.add(efe.getValue());
            }
        }
        if (extFiles.size() == 0) {
            Globals.prefs.put(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, "");
        } else {
            String[] list = extFiles.toArray(new String[extFiles.size()]);
            Globals.prefs.putStringArray(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, list);
        }

        Globals.initializeJournalNames();

        // Update the autocompleter for the "journal" field in all base panels,
        // so added journal names are available:
        for (int i = 0; i < frame.baseCount(); i++) {
            frame.baseAt(i).getAutoCompleters().addJournalListToAutoCompleter();
        }

    }


    class DownloadAction extends AbstractAction {

        final JTextField comp;


        public DownloadAction(JTextField tc) {
            super(Globals.lang("Download"));
            comp = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String chosen;
            chosen = JOptionPane.showInputDialog(Globals.lang("Choose the URL to download. The default value points to a list provided by the JabRef developers."),
                    "http://jabref.sf.net/journals/journal_abbreviations_general.txt");
            if (chosen == null) {
                return;
            }
            File toFile;
            try {
                URL url = new URL(chosen);
                String toName = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")),
                        null, JFileChooser.SAVE_DIALOG, false);
                if (toName == null) {
                    return;
                } else {
                    toFile = new File(toName);
                }
                URLDownload.buildMonitoredDownload(comp, url).downloadToFile(toFile);
                comp.setText(toFile.getPath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, Globals.lang("Error downloading file '%0'", chosen),
                        Globals.lang("Download failed"), JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    class BrowseAction extends AbstractAction {

        final JTextField comp;
        final boolean dir;


        public BrowseAction(JTextField tc, boolean dir) {
            super(Globals.lang("Browse"));
            this.dir = dir;
            comp = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String chosen;
            if (dir) {
                chosen = FileDialogs.getNewDir(frame, new File(comp.getText()), Globals.NONE,
                        JFileChooser.OPEN_DIALOG, false);
            } else {
                chosen = FileDialogs.getNewFile(frame, new File(comp.getText()), Globals.NONE,
                        JFileChooser.OPEN_DIALOG, false);
            }
            if (chosen != null) {
                File newFile = new File(chosen);
                comp.setText(newFile.getPath());
            }
        }
    }

    class AbbreviationsTableModel extends AbstractTableModel implements ActionListener {

        final String[] names = new String[] {Globals.lang("Journal name"), Globals.lang("Abbreviation")};
        List<JournalEntry> journals = null;


        public AbbreviationsTableModel() {

        }

        public void setJournals(SortedSet<Abbreviation> journals) {
            this.journals = new ArrayList<JournalEntry>();
            for (Abbreviation abbreviation : journals) {
                this.journals.add(new JournalEntry(abbreviation.getName(), abbreviation.getIsoAbbreviation()));
            }
            fireTableDataChanged();
        }

        public List<JournalEntry> getJournals() {
            return journals;
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public int getRowCount() {
            return journals.size();
        }

        @Override
        public Object getValueAt(int row, int col) {
            if (col == 0) {
                return journals.get(row).name;
            } else {
                return journals.get(row).abbreviation;
            }
        }

        @Override
        public void setValueAt(Object object, int row, int col) {
            JournalEntry entry = journals.get(row);
            if (col == 0) {
                entry.name = (String) object;
            } else {
                entry.abbreviation = (String) object;
            }

        }

        @Override
        public String getColumnName(int i) {
            return names[i];
        }

        @Override
        public boolean isCellEditable(int i, int i1) {
            return false;
        }

        public MouseListener getMouseListener() {
            return new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        JTable table = (JTable) e.getSource();
                        int row = table.rowAtPoint(e.getPoint());
                        nameTf.setText((String) getValueAt(row, 0));
                        abbrTf.setText((String) getValueAt(row, 1));
                        if (JOptionPane.showConfirmDialog(dialog, journalEditPanel, Globals.lang("Edit journal"),
                                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                            setValueAt(nameTf.getText(), row, 0);
                            setValueAt(abbrTf.getText(), row, 1);
                            Collections.sort(journals);
                            fireTableDataChanged();
                        }

                    }
                }
            };
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == add) {
                //int sel = userTable.getSelectedRow();
                //if (sel < 0)
                //    sel = 0;

                nameTf.setText("");
                abbrTf.setText("");
                if (JOptionPane.showConfirmDialog(dialog, journalEditPanel, Globals.lang("Edit journal"),
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    journals.add(new JournalEntry(nameTf.getText(), abbrTf.getText()));
                    //setValueAt(nameTf.getText(), sel, 0);
                    //setValueAt(abbrTf.getText(), sel, 1);
                    Collections.sort(journals);
                    fireTableDataChanged();
                }
            }
            else if (e.getSource() == remove) {
                int[] rows = userTable.getSelectedRows();
                if (rows.length > 0) {
                    for (int i = rows.length - 1; i >= 0; i--) {
                        journals.remove(rows[i]);
                    }
                    fireTableDataChanged();
                }
            }
        }
    }

    class ExternalFileEntry {

        private JPanel pan;
        private final JTextField tf;
        private final JButton browse = new JButton(Globals.lang("Browse"));
        private final JButton view = new JButton(Globals.lang("Preview"));
        private final JButton clear = new JButton(GUIGlobals.getImage("delete"));
        private final JButton download = new JButton(Globals.lang("Download"));


        public ExternalFileEntry() {
            tf = new JTextField();
            setupPanel();
        }

        public ExternalFileEntry(String filename) {
            tf = new JTextField(filename);
            setupPanel();
        }

        private void setupPanel() {
            tf.setEditable(false);
            BrowseAction browseA = new BrowseAction(tf, false);
            browse.addActionListener(browseA);
            DownloadAction da = new DownloadAction(tf);
            download.addActionListener(da);
            DefaultFormBuilder builder = new DefaultFormBuilder
                    (new FormLayout("fill:pref:grow, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, fill:pref", ""));
            builder.append(tf);
            builder.append(browse);
            builder.append(download);
            builder.append(view);
            builder.append(clear);

            pan = builder.getPanel();

            view.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        JournalAbbreviationRepository abbr = new JournalAbbreviationRepository();
                        abbr.readJournalListFromFile(new File(tf.getText()));
                        JTable table = new JTable(JournalAbbreviationsUtil.getTableModel(Globals.journalAbbrev));
                        JScrollPane pane = new JScrollPane(table);
                        JOptionPane.showMessageDialog(null, pane, Globals.lang("Journal list preview"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (FileNotFoundException ex) {
                        JOptionPane.showMessageDialog(null, Globals.lang("File '%0' not found", tf.getText()),
                                Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            clear.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    externals.remove(ExternalFileEntry.this);
                    buildExternalsPanel();
                }
            });
            clear.setToolTipText(Globals.lang("Remove"));
        }

        public JPanel getPanel() {
            return pan;
        }

        public String getValue() {
            return tf.getText();
        }
    }

    static class JournalEntry implements Comparable<JournalEntry> {

        String name, abbreviation;

        public JournalEntry(String name, String abbreviation) {
            this.name = name;
            this.abbreviation = abbreviation;
        }

        @Override
        public int compareTo(JournalEntry other) {
            return this.name.compareTo(other.name);
        }
    }
}
