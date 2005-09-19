package net.sf.jabref.journals;

import net.sf.jabref.PrefsTab;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;

import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.builder.DefaultFormBuilder;

import java.awt.event.ActionEvent;
import java.awt.*;
import java.io.File;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Sep 19, 2005
 * Time: 7:57:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class JournalsTab extends JPanel implements PrefsTab {

    JabRefFrame frame;
    JTextField personalFile = new JTextField();
    JTable table;

    public JournalsTab(JabRefFrame frame) {
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
        table = new JTable(Globals.journalAbbrev.getTableModel());
        builder.append(pan);
        builder.append(new JScrollPane(table));
        setLayout(new BorderLayout());
        add(builder.getPanel(), BorderLayout.CENTER);


        //add(new JScrollPane(table), BorderLayout.CENTER);
    }

    public void setValues() {
        personalFile.setText(Globals.prefs.get("personalJournalList"));
    }

    public void storeSettings() {
        if (!personalFile.getText().trim().equals(Globals.prefs.get("personalJournalList"))) {
            Globals.prefs.put("personalJournalList", personalFile.getText());
            Globals.initializeJournalNames();
        }

    }

    public boolean readyToClose() {
        return true;
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
}


