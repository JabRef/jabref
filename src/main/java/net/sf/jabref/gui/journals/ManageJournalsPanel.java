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
package net.sf.jabref.gui.journals;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.*;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.help.HelpAction;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.journals.Abbreviation;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.gui.net.MonitoredURLDownload;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Created by IntelliJ IDEA. User: alver Date: Sep 19, 2005 Time: 7:57:29 PM To browseOld this template use File |
 * Settings | File Templates.
 */
class ManageJournalsPanel extends JPanel {

    private static final Log LOGGER = LogFactory.getLog(ManageJournalsPanel.class);

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
    private final List<ExternalFileEntry> externals = new ArrayList<>(); // To hold references to external journal lists.
    private final JDialog dialog;
    private final JRadioButton newFile = new JRadioButton(Localization.lang("New file"));
    private final JRadioButton oldFile = new JRadioButton(Localization.lang("Existing file"));

    private final JButton add = new JButton(IconTheme.JabRefIcon.ADD_NOBOX.getIcon());
    private final JButton remove = new JButton(IconTheme.JabRefIcon.REMOVE_NOBOX.getIcon());


    public ManageJournalsPanel(final JabRefFrame frame) {
        this.frame = frame;

        personalFile.setEditable(false);

        ButtonGroup group = new ButtonGroup();
        group.add(newFile);
        group.add(oldFile);
        addExtPan.setLayout(new BorderLayout());
        JButton addExt = new JButton(IconTheme.JabRefIcon.ADD.getIcon());
        addExtPan.add(addExt, BorderLayout.EAST);
        addExtPan.setToolTipText(Localization.lang("Add"));
        FormLayout layout = new FormLayout("1dlu, 8dlu, left:pref, 4dlu, fill:200dlu:grow, 4dlu, fill:pref",
                "pref, pref, pref, 20dlu, 20dlu, fill:200dlu, 4dlu, pref");
        FormBuilder builder = FormBuilder.create().layout(layout);

        builder.addSeparator(Localization.lang("Built-in journal list")).xyw(2, 1, 6);
        JLabel description = new JLabel(
                "<HTML>" + Localization.lang("JabRef includes a built-in list of journal abbreviations.") + "<br>"
                        + Localization
                                .lang("You can add additional journal names by setting up a personal journal list,<br>as "
                                        + "well as linking to external journal lists.")
                        + "</HTML>");
        description.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        builder.add(description).xyw(2, 2, 6);
        JButton viewBuiltin = new JButton(Localization.lang("View"));
        builder.add(viewBuiltin).xy(7, 2);
        builder.addSeparator(Localization.lang("Personal journal list")).xyw(2, 3, 6);

        builder.add(newFile).xy(3, 4);
        builder.add(newNameTf).xy(5, 4);
        JButton browseNew = new JButton(Localization.lang("Browse"));
        builder.add(browseNew).xy(7, 4);
        builder.add(oldFile).xy(3, 5);
        builder.add(personalFile).xy(5, 5);
        JButton browseOld = new JButton(Localization.lang("Browse"));
        builder.add(browseOld).xy(7, 5);

        userPanel.setLayout(new BorderLayout());
        builder.add(userPanel).xyw(2, 6, 4);
        ButtonStackBuilder butBul = new ButtonStackBuilder();
        butBul.addButton(add);
        butBul.addButton(remove);

        butBul.addGlue();
        builder.add(butBul.getPanel()).xy(7, 6);

        builder.addSeparator(Localization.lang("External files")).xyw(2, 8, 6);
        externalFilesPanel.setLayout(new BorderLayout());

        setLayout(new BorderLayout());
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(builder.getPanel(), BorderLayout.NORTH);
        add(externalFilesPanel, BorderLayout.CENTER);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        JButton ok = new JButton(Localization.lang("OK"));
        bb.addButton(ok);
        JButton cancel = new JButton(Localization.lang("Cancel"));
        bb.addButton(cancel);
        bb.addUnrelatedGap();

        JButton help = new HelpAction(HelpFiles.journalAbbrHelp).getHelpButton();
        bb.addButton(help);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        dialog = new JDialog(frame, Localization.lang("Journal abbreviations"), false);
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        // Set up panel for editing a single journal, to be used in a dialog box:
        FormLayout layout2 = new FormLayout("right:pref, 4dlu, fill:180dlu", "p, 2dlu, p");
        FormBuilder builder2 = FormBuilder.create().layout(layout2);
        builder2.add(Localization.lang("Journal name")).xy(1, 1);
        builder2.add(nameTf).xy(3, 1);
        builder2.add(Localization.lang("ISO abbreviation")).xy(1, 3);
        builder2.add(abbrTf).xy(3, 3);
        journalEditPanel = builder2.getPanel();

        viewBuiltin.addActionListener(e -> {
            JTable table = new JTable(JournalAbbreviationsUtil
                    .getTableModel(Globals.journalAbbreviationLoader.getRepository().getAbbreviations()));
            JScrollPane pane = new JScrollPane(table);
            JOptionPane.showMessageDialog(null, pane, Localization.lang("Journal list preview"),
                    JOptionPane.INFORMATION_MESSAGE);
        });

        browseNew.addActionListener(e -> {
            File old = null;
            if (!"".equals(newNameTf.getText())) {
                old = new File(newNameTf.getText());
            }
            String name = FileDialogs.getNewFile(frame, old, null, JFileChooser.SAVE_DIALOG, false);
            if (name != null) {
                newNameTf.setText(name);
                newFile.setSelected(true);
            }
        });

        browseOld.addActionListener(e -> {
            File old = null;
            if (!"".equals(personalFile.getText())) {
                old = new File(personalFile.getText());
            }
            String name = FileDialogs.getNewFile(frame, old, null, JFileChooser.OPEN_DIALOG, false);
            if (name != null) {
                personalFile.setText(name);
                oldFile.setSelected(true);
                oldFile.setEnabled(true);
                setupUserTable();
            }
        });

        ok.addActionListener(e -> {
            if (readyToClose()) {
                try {
                    storeSettings();
                    dialog.dispose();
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null,
                            Localization.lang("Error opening file") + ": " + ex.getMessage(),
                            Localization.lang("Error opening file"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.debug("Cannot find abbreviation file", ex);
                }
            }
        });

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dialog.dispose();
            }
        };
        cancel.addActionListener(cancelAction);

        add.addActionListener(tableModel);
        remove.addActionListener(tableModel);
        addExt.addActionListener(e -> {
            externals.add(new ExternalFileEntry());
            buildExternalsPanel();
        });

        // Key bindings:
        ActionMap am = getActionMap();
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        int xSize = getPreferredSize().width;
        dialog.setSize(xSize + 10, 700);
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setValues() {
        personalFile.setText(Globals.prefs.get(JabRefPreferences.PERSONAL_JOURNAL_LIST));
        if (personalFile.getText().isEmpty()) {
            newFile.setSelected(true);
            newFile.setEnabled(true);
            oldFile.setSelected(false);
            oldFile.setEnabled(false);
        } else {
            newFile.setSelected(false);
            newFile.setEnabled(false);
            oldFile.setSelected(true);
            oldFile.setEnabled(true);
        }
        setupUserTable();
        setupExternals();
        buildExternalsPanel();

    }

    private void buildExternalsPanel() {

        FormBuilder builder = FormBuilder.create().layout(new FormLayout("fill:pref:grow", "p"));
        int row = 1;
        for (ExternalFileEntry efe : externals) {
            builder.add(efe.getPanel()).xy(1, row);
            builder.appendRows("2dlu, p");
            row += 2;
        }
        builder.add(Box.createVerticalGlue()).xy(1, row);
        builder.appendRows("2dlu, p, 2dlu, p");
        builder.add(addExtPan).xy(1, row + 2);
        builder.add(Box.createVerticalGlue()).xy(1, row + 2);

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
        List<String> externalFiles = Globals.prefs.getStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS);
        if (externalFiles.isEmpty()) {
            ExternalFileEntry efe = new ExternalFileEntry();
            externals.add(efe);
        } else {
            for (String externalFile : externalFiles) {
                ExternalFileEntry efe = new ExternalFileEntry(externalFile);
                externals.add(efe);
            }
        }
    }

    private void setupUserTable() {
        List<Abbreviation> userAbbreviations = new ArrayList<>();
        String filename = personalFile.getText();
        if ((!filename.isEmpty()) && new File(filename).exists()) {
            try {
                userAbbreviations = JournalAbbreviationLoader.readJournalListFromFile(new File(filename),
                        Globals.prefs.getDefaultEncoding());
            } catch (FileNotFoundException e) {
                LOGGER.warn("Problem reading abbreviation file", e);
            }
        }

        tableModel.setJournals(userAbbreviations);
        userTable = new JTable(tableModel);
        userTable.addMouseListener(tableModel.getMouseListener());
        userPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
    }

    private boolean readyToClose() {
        File f;
        if (newFile.isSelected()) {
            if (newNameTf.getText().isEmpty()) {
                if (tableModel.getRowCount() > 0) {
                    JOptionPane.showMessageDialog(this,
                            Localization.lang("You must choose a filename to store journal abbreviations"),
                            Localization.lang("Store journal abbreviations"), JOptionPane.ERROR_MESSAGE);
                    return false;
                } else {
                    return true;
                }
            } else {
                f = new File(newNameTf.getText());
                return !f.exists() || (JOptionPane.showConfirmDialog(this,
                        Localization.lang("'%0' exists. Overwrite file?", f.getName()),
                        Localization.lang("Store journal abbreviations"),
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION);
            }
        }
        return true;
    }

    private void storeSettings() throws FileNotFoundException {
        File f = null;
        if (newFile.isSelected()) {
            if (!newNameTf.getText().isEmpty()) {
                f = new File(newNameTf.getText());
            }
        } else {
            f = new File(personalFile.getText());
        }

        if (f != null) {
            if (!f.exists()) {
                throw new FileNotFoundException(f.getAbsolutePath());
            }
            try (FileOutputStream stream = new FileOutputStream(f, false);
                    OutputStreamWriter writer = new OutputStreamWriter(stream, Globals.prefs.getDefaultEncoding())) {
                for (JournalEntry entry : tableModel.getJournals()) {
                    writer.write(entry.getName());
                    writer.write(" = ");
                    writer.write(entry.getAbbreviation());
                    writer.write(Globals.NEWLINE);
                }
            } catch (IOException e) {
                LOGGER.warn("Problem writing abbreviation file", e);
            }
            String filename = f.getPath();
            if ("".equals(filename)) {
                filename = null;
            }
            Globals.prefs.put(JabRefPreferences.PERSONAL_JOURNAL_LIST, filename);
        }

        // Store the list of external files set up:
        List<String> extFiles = new ArrayList<>();
        for (ExternalFileEntry efe : externals) {
            if (!"".equals(efe.getValue())) {
                extFiles.add(efe.getValue());
            }
        }
        Globals.prefs.putStringList(JabRefPreferences.EXTERNAL_JOURNAL_LISTS, extFiles);

        // Update the autocompleter for the "journal" field in all base panels,
        // so added journal names are available:
        for (BasePanel basePanel : frame.getBasePanelList()) {
            basePanel.getAutoCompleters().addJournalListToAutoCompleter();
        }

    }


    class DownloadAction extends AbstractAction {

        private final JTextField comp;


        public DownloadAction(JTextField tc) {
            super(Localization.lang("Download"));
            comp = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String chosen;
            chosen = JOptionPane.showInputDialog(Localization.lang("Choose the URL to download."), "");
            if (chosen == null) {
                return;
            }
            File toFile;
            try {
                String toName = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")), null,
                        JFileChooser.SAVE_DIALOG, false);
                if (toName == null) {
                    return;
                } else {
                    toFile = new File(toName);
                }
                URL url = new URL(chosen);
                MonitoredURLDownload.buildMonitoredDownload(comp, url).downloadToFile(toFile);
                comp.setText(toFile.getPath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(null, Localization.lang("Error downloading file '%0'", chosen),
                        Localization.lang("Download failed"), JOptionPane.ERROR_MESSAGE);
                LOGGER.debug("Error downloading file", ex);
            }
        }
    }

    class BrowseAction extends AbstractAction {

        private final JTextField comp;
        private final boolean dir;


        public BrowseAction(JTextField tc, boolean dir) {
            super(Localization.lang("Browse"));
            this.dir = dir;
            comp = tc;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String chosen;
            if (dir) {
                chosen = FileDialogs.getNewDir(frame, new File(comp.getText()), Globals.NONE, JFileChooser.OPEN_DIALOG,
                        false);
            } else {
                chosen = FileDialogs.getNewFile(frame, new File(comp.getText()), Globals.NONE, JFileChooser.OPEN_DIALOG,
                        false);
            }
            if (chosen != null) {
                File nFile = new File(chosen);
                comp.setText(nFile.getPath());
            }
        }
    }

    class AbbreviationsTableModel extends AbstractTableModel implements ActionListener {

        private final String[] names = new String[] {Localization.lang("Journal name"),
                Localization.lang("Abbreviation")};
        private List<JournalEntry> journals;

        public void setJournals(List<Abbreviation> abbreviations) {
            this.journals = new ArrayList<>();
            for (Abbreviation abbreviation : abbreviations) {
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
                return journals.get(row).getName();
            } else {
                return journals.get(row).getAbbreviation();
            }
        }

        @Override
        public void setValueAt(Object object, int row, int col) {
            JournalEntry entry = journals.get(row);
            if (col == 0) {
                entry.setName((String) object);
            } else {
                entry.setAbbreviation((String) object);
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
                        if (JOptionPane.showConfirmDialog(dialog, journalEditPanel, Localization.lang("Edit journal"),
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
                nameTf.setText("");
                abbrTf.setText("");
                if (JOptionPane.showConfirmDialog(dialog, journalEditPanel, Localization.lang("Edit journal"),
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    journals.add(new JournalEntry(nameTf.getText(), abbrTf.getText()));
                    Collections.sort(journals);
                    fireTableDataChanged();
                }
            } else if (e.getSource() == remove) {
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
        private final JButton browse = new JButton(Localization.lang("Browse"));
        private final JButton view = new JButton(Localization.lang("Preview"));
        private final JButton clear = new JButton(IconTheme.JabRefIcon.DELETE_ENTRY.getIcon());
        private final JButton download = new JButton(Localization.lang("Download"));


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
            FormBuilder builder = FormBuilder.create().layout(new FormLayout(
                    "fill:pref:grow, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, fill:pref, 4dlu, fill:pref", "p"));
            builder.add(tf).xy(1, 1);
            builder.add(browse).xy(3, 1);
            builder.add(download).xy(5, 1);
            builder.add(view).xy(7, 1);
            builder.add(clear).xy(9, 1);

            pan = builder.getPanel();

            view.addActionListener(e -> {
                try {
                    List<Abbreviation> abbreviations = JournalAbbreviationLoader
                            .readJournalListFromFile(new File(tf.getText()));

                    JTable table = new JTable(JournalAbbreviationsUtil.getTableModel(abbreviations));
                    JScrollPane pane = new JScrollPane(table);
                    JOptionPane.showMessageDialog(null, pane, Localization.lang("Journal list preview"),
                            JOptionPane.INFORMATION_MESSAGE);
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(null, Localization.lang("File '%0' not found", tf.getText()),
                            Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.debug("File not found", ex);
                }
            });
            clear.addActionListener(e -> {
                externals.remove(ExternalFileEntry.this);
                buildExternalsPanel();
            });
            clear.setToolTipText(Localization.lang("Remove"));
        }

        public JPanel getPanel() {
            return pan;
        }

        public String getValue() {
            return tf.getText();
        }
    }

    static class JournalEntry implements Comparable<JournalEntry> {

        private String name;
        private String abbreviation;


        public JournalEntry(String name, String abbreviation) {
            this.name = name;
            this.abbreviation = abbreviation;
        }

        @Override
        public int compareTo(JournalEntry other) {
            return this.name.compareTo(other.name);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof JournalEntry) {
                return this.name.equals(((JournalEntry) o).name);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAbbreviation() {
            return abbreviation;
        }

        public void setAbbreviation(String abbreviation) {
            this.abbreviation = abbreviation;
        }
    }
}
