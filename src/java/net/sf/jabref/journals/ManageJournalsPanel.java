package net.sf.jabref.journals;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.GUIGlobals;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import com.jgoodies.forms.layout.FormLayout;
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

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 19, 2005
 * Time: 7:57:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class ManageJournalsPanel extends JPanel{

    JabRefFrame frame;
    JTextField personalFile = new JTextField();
    AbbreviationsTableModel tableModel = null;
    JTable builtInTable, userTable;
    JPanel userPanel = new JPanel(),
        journalEditPanel;
    JTextField nameTf = new JTextField(),
        abbrTf = new JTextField();
    JDialog dialog;
    JButton add = new JButton(new ImageIcon(GUIGlobals.addIconFile)),
        remove = new JButton(new ImageIcon(GUIGlobals.removeIconFile)),
        ok = new JButton(Globals.lang("Ok")),
        cancel = new JButton(Globals.lang("Cancel"));

    public ManageJournalsPanel(JabRefFrame frame) {
        this.frame = frame;

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, fill:250dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                        "40dlu, 20dlu, 200dlu, 200dlu");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        JPanel pan = new JPanel();

        JLabel description = new JLabel("<HTML>"+Globals.lang("JabRef can switch journal names between "
            +"abbreviated and full form. Since it knows only a limited number of journal names, "
            +"you may need to add your own definitions.")+"</HTML>");

        builder.append(pan);
        builder.append(description);
        builder.nextLine();
        builder.append(pan);
        builder.append(personalFile);
        BrowseAction action = new BrowseAction(personalFile, false);
        JButton browse = new JButton(Globals.lang("Browse"));
        browse.addActionListener(action);
        builder.append(browse);
        builder.nextLine();

        userPanel.setLayout(new BorderLayout());
        builtInTable = new JTable(Globals.journalAbbrev.getTableModel());
        builder.append(pan);
        builder.append(new JScrollPane(userPanel));
        ButtonStackBuilder butBul = new ButtonStackBuilder();
        butBul.addGridded(add);
        butBul.addGridded(remove);

        butBul.addGlue();
        builder.append(butBul.getPanel());

        builder.nextLine();

        builder.append(pan);
        builder.append(new JScrollPane(builtInTable));

        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(ok);
        bb.addGridded(cancel);
        bb.addGlue();

        dialog = new JDialog(frame, Globals.lang("Journal abbrebiations"), false);
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
        builder2.append(Globals.lang("Abbreviation"));
        builder2.append(abbrTf);
        journalEditPanel = builder2.getPanel();

        ok.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                storeSettings();
                dialog.dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        dialog.dispose();
                    }
                });
        dialog.pack();
                
    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setValues() {
        System.out.println("Ju");
        personalFile.setText(Globals.prefs.get("personalJournalList"));
        JournalAbbreviations userAbbr = new JournalAbbreviations();
        if (Globals.prefs.get("personalJournalList") != null) {
            try {
                userAbbr.readJournalList(new File(Globals.prefs.get("personalJournalList")));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        tableModel = new AbbreviationsTableModel(userAbbr.getJournals());
        add.addActionListener(tableModel);
        remove.addActionListener(tableModel);
        userTable = new JTable(tableModel);
        userTable.addMouseListener(tableModel.getMouseListener());
        userPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
    }

    public void storeSettings() {
        if (personalFile.getText().length() > 0) {
            File f = new File(personalFile.getText());
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


        }
        if (!personalFile.getText().trim().equals(Globals.prefs.get("personalJournalList"))) {
            Globals.prefs.put("personalJournalList", personalFile.getText());
            Globals.initializeJournalNames();
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
        ArrayList journals;

        public AbbreviationsTableModel(Map journals) {
            this.journals = new ArrayList();
            for (Iterator i=journals.keySet().iterator(); i.hasNext();) {
                String journal = (String)i.next(),
                        abbr = (String)journals.get(journal);
                this.journals.add(new JournalEntry(journal, abbr));
            }

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
                int sel = userTable.getSelectedRow();
                if (sel < 0)
                    sel = 0;
                journals.add(sel, new JournalEntry("", ""));
                nameTf.setText("");
                abbrTf.setText("");
                if (JOptionPane.showConfirmDialog(dialog, journalEditPanel, Globals.lang("Edit journal"),
                        JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                    setValueAt(nameTf.getText(), sel, 0);
                    setValueAt(abbrTf.getText(), sel, 1);
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


