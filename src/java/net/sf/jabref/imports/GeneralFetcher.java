package net.sf.jabref.imports;

import net.sf.jabref.*;
import net.sf.jabref.undo.NamedCompound;
import net.sf.jabref.gui.ImportInspectionDialog;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.util.Hashtable;
import java.util.Arrays;

/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */

public class GeneralFetcher extends SidePaneComponent implements ActionListener {

    JTextField tf = new JTextField();
    JPanel pan = new JPanel();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JButton go = new JButton(Globals.lang("Fetch")),
    helpBut = new JButton(new ImageIcon(GUIGlobals.helpIconFile));
    HelpAction help;
    EntryFetcher fetcher;
    SidePaneManager sidePaneManager;
    Action action;
    JabRefFrame frame;

    public GeneralFetcher(SidePaneManager p0, JabRefFrame frame, final EntryFetcher fetcher) {
        super(p0, fetcher.getIcon(), fetcher.getTitle());
        this.sidePaneManager = p0;
        this.frame = frame;
        this.fetcher = fetcher;

        action = new FetcherAction();

        help = new HelpAction(Globals.helpDiag, fetcher.getHelpPage(), "Help");

        helpBut.addActionListener(help);
        helpBut.setMargin(new Insets(0, 0, 0, 0));
        JPanel main = new JPanel();
        main.setLayout(gbl);
        con.fill = GridBagConstraints.BOTH;
        con.insets = new Insets(0, 0, 2, 0);
        con.gridwidth = GridBagConstraints.REMAINDER;
        con.weightx = 1;
        con.weighty = 0;
        con.weighty = 1;
        con.insets = new Insets(0, 0, 0, 0);
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(tf, con);
        main.add(tf);
        con.weighty = 0;
        con.gridwidth = 1;
        gbl.setConstraints(go, con);
        main.add(go);
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(helpBut, con);
        main.add(helpBut);

        JPanel pan = fetcher.getOptionsPanel();
        if (pan != null) {
            gbl.setConstraints(pan, con);
            main.add(pan);
        }

        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        add(main, BorderLayout.CENTER);
        go.addActionListener(this);
        tf.addActionListener(this);
    }

    public JTextField getTextField() {
        return tf;
    }

    public Action getAction() {
        return action;
    }

    public void actionPerformed(ActionEvent e) {
        if (tf.getText().trim().length() == 0)
            return;
        ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.basePanel(),
                BibtexFields.DEFAULT_INSPECTION_FIELDS, fetcher.getTitle(), false);
        dialog.addCallBack(fetcher);
        Util.placeDialog(dialog, frame);
        fetcher.processQuery(tf.getText().trim(), dialog, frame);

    }

    class FetcherAction extends AbstractAction {
        public FetcherAction() {
            super(fetcher.getTitle(), new ImageIcon(fetcher.getIcon()));
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey(fetcher.getKeyName()));
        }
        public void actionPerformed(ActionEvent e) {
            sidePaneManager.add(fetcher.getTitle(), GeneralFetcher.this);
        }
    }

}
