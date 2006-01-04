package net.sf.jabref.journals;

import net.sf.jabref.*;
import net.sf.jabref.net.URLDownload;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.ButtonStackBuilder;

import java.awt.event.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 19, 2005
 * Time: 7:57:29 PM
 * To browseOld this template use File | Settings | File Templates.
 */
public class ManageJournalsPanel extends JPanel{

    JabRefFrame frame;
    JTextField personalFile = new JTextField();
    AbbreviationsTableModel tableModel = new AbbreviationsTableModel();
    JTable userTable; // builtInTable
    JPanel userPanel = new JPanel(),
        journalEditPanel,
        externalFilesPanel = new JPanel(),
        addExtPan = new JPanel();
    JTextField nameTf = new JTextField(),
        newNameTf = new JTextField(),
        abbrTf = new JTextField();
    java.util.List externals = new ArrayList(); // To hold references to external journal lists.
    JDialog dialog;
    JRadioButton newFile = new JRadioButton(Globals.lang("New file")),
        oldFile = new JRadioButton(Globals.lang("Existing file"));

    JButton add = new JButton(new ImageIcon(GUIGlobals.addIconFile)),
        remove = new JButton(new ImageIcon(GUIGlobals.removeIconFile)),
        ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel")),
        help = new JButton(Globals.lang("Help")),
        browseOld = new JButton(Globals.lang("Browse")),
        browseNew = new JButton(Globals.lang("Browse")),
        addExt = new JButton(new ImageIcon(GUIGlobals.addIconFile));


    public ManageJournalsPanel(final JabRefFrame frame) {
        this.frame = frame;

        personalFile.setEditable(false);

        ButtonGroup group = new ButtonGroup();
        group.add(newFile);
        group.add(oldFile);
        addExtPan.setLayout(new BorderLayout());
        addExtPan.add(addExt, BorderLayout.EAST);
        addExtPan.setToolTipText(Globals.lang("Add"));
        //addExtPan.setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.red));
        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:200dlu:grow, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                        "pref, 20dlu, 20dlu, fill:200dlu, 4dlu, pref");//150dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        CellConstraints cc = new CellConstraints();

        JPanel pan = new JPanel();

        /*JLabel description = new JLabel("<HTML>"+Globals.lang("JabRef can switch journal names between "
            +"abbreviated and full form. Since it knows only a limited number of journal names, "
            +"you may need to add your own definitions.")+"</HTML>");*/
        builder.addSeparator(Globals.lang("Personal journal list"), cc.xyw(2,1,6));

        //builder.add(description, cc.xyw(2,1,6));
        builder.add(newFile, cc.xy(3,2));
        builder.add(newNameTf, cc.xy(5,2));
        builder.add(browseNew, cc.xy(7,2));
        builder.add(oldFile, cc.xy(3,3));
        builder.add(personalFile, cc.xy(5,3));
        //BrowseAction action = new BrowseAction(personalFile, false);
        //JButton browse = new JButton(Globals.lang("Browse"));
        //browse.addActionListener(action);
        builder.add(browseOld, cc.xy(7,3));

        userPanel.setLayout(new BorderLayout());
        //builtInTable = new JTable(Globals.journalAbbrev.getTableModel());
        builder.add(userPanel, cc.xyw(2,4,4));
        ButtonStackBuilder butBul = new ButtonStackBuilder();
        butBul.addGridded(add);
        butBul.addGridded(remove);

        butBul.addGlue();
        builder.add(butBul.getPanel(), cc.xy(7,4));

        builder.addSeparator(Globals.lang("External files"), cc.xyw(2,6,6));
        externalFilesPanel.setLayout(new BorderLayout());
        //builder.add(/*new JScrollPane(*/externalFilesPanel/*)*/, cc.xyw(2,8,6));

        setLayout(new BorderLayout());
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));//createMatteBorder(1,1,1,1,Color.green));
        add(builder.getPanel(), BorderLayout.NORTH);
        add(externalFilesPanel, BorderLayout.CENTER);
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGridded((JComponent)Box.createHorizontalStrut(10));
        bb.addGridded(help);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
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

        browseNew.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File old = null;
                if (!newNameTf.getText().equals(""))
                    old = new File(newNameTf.getText());
                String name = Globals.getNewFile(frame, Globals.prefs, old, null, JFileChooser.SAVE_DIALOG, false);
                if (name != null) {
                    if ((old != null) && (tableModel.getRowCount() > 0)) {
                    }
                    newNameTf.setText(name);
                    newFile.setSelected(true);
                }
            }
        });
        browseOld.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File old = null;
                if (!personalFile.getText().equals(""))
                    old = new File(personalFile.getText());
                String name = Globals.getNewFile(frame, Globals.prefs, old, null, JFileChooser.OPEN_DIALOG, false);
                if (name != null) {
                    if ((old != null) && (tableModel.getRowCount() > 0)) {
                    }
                    personalFile.setText(name);
                    oldFile.setSelected(true);
                    oldFile.setEnabled(true);
                    setupUserTable();
                }
            }
        });


        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (readyToClose()) {
                    storeSettings();
                    dialog.dispose();
                }
            }
        });
        help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.journalAbbrHelp));
        cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });

        add.addActionListener(tableModel);
        remove.addActionListener(tableModel);
        addExt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                externals.add(new ExternalFileEntry());
                buildExternalsPanel();
            }
        });

        //dialog.pack();
        dialog.setSize(600,700);
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setValues() {
        personalFile.setText(Globals.prefs.get("personalJournalList"));
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

        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("fill:pref:grow",""));
        for (Iterator i=externals.iterator(); i.hasNext();) {
            ExternalFileEntry efe = (ExternalFileEntry)i.next();
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
        pane.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        externalFilesPanel.setMinimumSize(new Dimension(400,400));
        externalFilesPanel.removeAll();
        externalFilesPanel.add(pane, BorderLayout.CENTER);
        externalFilesPanel.revalidate();
        externalFilesPanel.repaint();

    }

    private void setupExternals() {
        String[] externalFiles = Globals.prefs.getStringArray("externalJournalLists");
        if ((externalFiles == null) || (externalFiles.length == 0)) {
            ExternalFileEntry efe = new ExternalFileEntry();
            externals.add(efe);
        } else {
            for (int i=0; i<externalFiles.length; i++) {
                ExternalFileEntry efe = new ExternalFileEntry(externalFiles[i]);
                externals.add(efe);

            }

        }

        //efe = new ExternalFileEntry();
        //externals.add(efe);

    }

    public void setupUserTable() {
        JournalAbbreviations userAbbr = new JournalAbbreviations();
        String filename = personalFile.getText();
        if (!filename.equals("") && (new File(filename)).exists()) {
            try {
                userAbbr.readJournalList(new File(filename));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        tableModel.setJournals(userAbbr.getJournals());
        userTable = new JTable(tableModel);
        userTable.addMouseListener(tableModel.getMouseListener());
        userPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
    }

    public boolean readyToClose() {
        File f;
        if (newFile.isSelected()) {
            if (newNameTf.getText().length() > 0) {
                f = new File(newNameTf.getText());
                return (!f.exists() ||
                    (JOptionPane.showConfirmDialog
                     (this, "'"+f.getName()+"' "+Globals.lang("exists. Overwrite file?"),
                      Globals.lang("Store journal abbreviations"), JOptionPane.OK_CANCEL_OPTION)
                     == JOptionPane.OK_OPTION));
            } else {
                if (tableModel.getRowCount() > 0) {
                    JOptionPane.showMessageDialog(this, Globals.lang("You must choose a file name to store journal abbreviations"),
                            Globals.lang("Store journal abbreviations"), JOptionPane.ERROR_MESSAGE);
                        return false;
                }
                else return true;

            }
        }
        return true;
    }

    public void storeSettings() {
        File f;
        if (newFile.isSelected()) {
            if (newNameTf.getText().length() > 0) {
                f = new File(newNameTf.getText());
            } else {
                return; // Nothing to do.
            }
        } else
            f = new File(personalFile.getText());

        FileWriter fw = null;
        try {
            fw = new FileWriter(f, false);
            for (Iterator i=tableModel.getJournals().iterator(); i.hasNext();) {
                JournalEntry entry = (JournalEntry)i.next();
                fw.write(entry.name);
                fw.write(" = ");
                fw.write(entry.abbreviation);
                fw.write(Globals.NEWLINE);
            }

        } catch (IOException e) {
            e.printStackTrace();

        } finally {
            if (fw != null)
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }

        String filename = f.getPath();
        if (filename.equals(""))
            filename = null;
        Globals.prefs.put("personalJournalList", filename);

        // Store the list of external files set up:
        ArrayList extFiles = new ArrayList();
        for (Iterator i=externals.iterator(); i.hasNext();) {
            ExternalFileEntry efe = (ExternalFileEntry)i.next();
            if (!efe.getValue().equals("")) {
                extFiles.add(efe.getValue());
            }
        }
        if (extFiles.size() == 0)
            Globals.prefs.put("externalJournalLists", "");
        else {
            String[] list = (String[])extFiles.toArray(new String[extFiles.size()]);
            Globals.prefs.putStringArray("externalJournalLists", list);
        }


        Globals.initializeJournalNames();

    }

    class DownloadAction extends AbstractAction {
        JTextField comp;

        public DownloadAction(JTextField tc) {
            super(Globals.lang("Download"));
            comp = tc;
        }

        public void actionPerformed(ActionEvent e) {
            String chosen = null;
            chosen = JOptionPane.showInputDialog(Globals.lang("Choose the URL to download. The default value points to a list provided by the JabRef developers."),
                    "http://jabref.sf.net/journals/journal_abbreviations_general.txt");
            if (chosen == null)
                return;
            File toFile;
            try {
                URL url = new URL(chosen);
                String toName = Globals.getNewFile(frame, Globals.prefs, new File(System.getProperty("user.home")),
                        null, JFileChooser.SAVE_DIALOG, false);
                if (toName == null)
                    return;
                else toFile = new File(toName);
                URLDownload ud = new URLDownload(comp, url, toFile);
                ud.download();
                comp.setText(toFile.getPath());
            } catch (MalformedURLException ex) {
                ex.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException ex2) {
                ex2.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

    class BrowseAction extends AbstractAction {
        JTextField comp;
        boolean dir;

        public BrowseAction(JTextField tc, boolean dir) {
            super(Globals.lang("Browse"));
            this.dir = dir;
            comp = tc;
        }

        public void actionPerformed(ActionEvent e) {
            String chosen = null;
            if (dir)
                chosen = Globals.getNewDir(frame, Globals.prefs, new File(comp.getText()), Globals.NONE,
                        JFileChooser.OPEN_DIALOG, false);
            else
                chosen = Globals.getNewFile(frame, Globals.prefs, new File(comp.getText()), Globals.NONE,
                        JFileChooser.OPEN_DIALOG, false);
            if (chosen != null) {
                File newFile = new File(chosen);
                comp.setText(newFile.getPath());
            }
        }
    }

    class AbbreviationsTableModel extends AbstractTableModel implements ActionListener {

        String[] names = new String[] {Globals.lang("Journal name"), Globals.lang("Abbreviation")};
        ArrayList journals = null;

        public AbbreviationsTableModel() {


        }

        public void setJournals(Map journals) {
            this.journals = new ArrayList();
            for (Iterator i=journals.keySet().iterator(); i.hasNext();) {
                String journal = (String)i.next(),
                        abbr = (String)journals.get(journal);
                this.journals.add(new JournalEntry(journal, abbr));
            }
            fireTableDataChanged();
        }

        public ArrayList getJournals() {
            return journals;
        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return journals.size();
        }

        public Object getValueAt(int row, int col) {
            if (col == 0)
                return ((JournalEntry)journals.get(row)).name;
            else
                return ((JournalEntry)journals.get(row)).abbreviation;
        }

        public void setValueAt(Object object, int row, int col) {
            JournalEntry entry = (JournalEntry)journals.get(row);
            if (col == 0)
                entry.name = (String)object;
            else
                entry.abbreviation = (String)object;

        }

        public String getColumnName(int i) {
            return names[i];
        }

        public boolean isCellEditable(int i, int i1) {
            return false;
        }

        public MouseListener getMouseListener() {
            return new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        JTable table = (JTable)e.getSource();
                        int row = table.rowAtPoint(e.getPoint());
                        nameTf.setText((String)getValueAt(row,0));
                        abbrTf.setText((String)getValueAt(row,1));
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
                    for (int i=rows.length-1; i>=0; i--) {
                        journals.remove(rows[i]);
                    }
                    fireTableDataChanged();
                }
            }
        }
    }

    class ExternalFileEntry {
        private JPanel pan;
        private JTextField tf;
        private JButton browse = new JButton(Globals.lang("Browse")),
            view = new JButton(Globals.lang("Preview")),
            clear = new JButton(new ImageIcon(GUIGlobals.removeIconFile)),
            download = new JButton(Globals.lang("Download"));
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
                public void actionPerformed(ActionEvent e) {
                    try {
                        JournalAbbreviations abbr = new JournalAbbreviations(new File(tf.getText()));
                        JTable table = new JTable(abbr.getTableModel());
                        JScrollPane pane = new JScrollPane(table);
                        JOptionPane.showMessageDialog(null, pane, Globals.lang("Journal list preview"), JOptionPane.INFORMATION_MESSAGE);
                    } catch (FileNotFoundException ex) {
                        JOptionPane.showMessageDialog(null, Globals.lang("File '%0' not found", tf.getText()),
                                Globals.lang("Error"), JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            clear.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    externals.remove(ExternalFileEntry.this);
                    buildExternalsPanel();
                }
            });
            clear.setToolTipText(Globals.lang("Remove"));

        }
        public JPanel getPanel() { return pan; }
        public String getValue() { return tf.getText(); }
    }

    class JournalEntry implements Comparable {
        String name, abbreviation;
        public JournalEntry(String name, String abbreviation) {
            this.name = name;
            this.abbreviation = abbreviation;
        }
        public int compareTo(Object other) {
            JournalEntry entry = (JournalEntry)other;
            return this.name.compareTo(entry.name);
        }
    }
}


