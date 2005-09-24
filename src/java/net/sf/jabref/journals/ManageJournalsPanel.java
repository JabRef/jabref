package net.sf.jabref.journals;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;

import java.awt.event.ActionEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;

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
    JTable builtInTable, userTable;
    JPanel userPanel = new JPanel(),
        journalEditPanel;
    JTextField nameTf = new JTextField(),
        abbrTf = new JTextField();
    JDialog dialog;

    public ManageJournalsPanel(JabRefFrame frame) {
        this.frame = frame;

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, fill:180dlu, 4dlu, fill:pref", // 4dlu, left:pref, 4dlu",
                        "");
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
        builder.nextLine();

        builder.append(pan);
        builder.append(new JScrollPane(builtInTable));
        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);

        dialog = new JDialog(frame, Globals.lang("Journal abbrebiations"), false);
        dialog.getContentPane().add(this, BorderLayout.CENTER);
        dialog.pack();
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

    }

    public JDialog getDialog() {
        return dialog;
    }

    public void setValues() {
        System.out.println("Ju");
        personalFile.setText(Globals.prefs.get("personalJournalList"));
        JournalAbbreviations userAbbr = new JournalAbbreviations();
        try {
            userAbbr.readJournalList(new File(Globals.prefs.get("personalJournalList")));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AbbreviationsTableModel tableModel = new AbbreviationsTableModel(userAbbr.getJournals());
        userTable = new JTable(tableModel);
        userTable.addMouseListener(tableModel.getMouseListener());
        userPanel.add(new JScrollPane(userTable), BorderLayout.CENTER);
    }

    public void storeSettings() {
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

    class AbbreviationsTableModel extends AbstractTableModel {

        String[] names = new String[] {Globals.lang("Journal name"), Globals.lang("Abbreviation")};
        ArrayList journals;

        public AbbreviationsTableModel(Map journals) {
            this.journals = new ArrayList();
            for (Iterator i=journals.keySet().iterator(); i.hasNext();) {
                Object journal = i.next(),
                        abbr = journals.get(journal);
                this.journals.add(new Object[] {journal, abbr});
            }

        }

        public int getColumnCount() {
            return 2;
        }

        public int getRowCount() {
            return journals.size();
        }

        public Object getValueAt(int row, int col) {
            return ((Object[])journals.get(row))[col].toString();
        }

        public void setValueAt(Object object, int row, int col) {
            Object[] o = (Object[])journals.get(row);
            o[col] = object;
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
                        JOptionPane.showMessageDialog(dialog, journalEditPanel);
                    }
                }
            };
        }
    }
}


