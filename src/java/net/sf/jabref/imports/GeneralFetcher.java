package net.sf.jabref.imports;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.sf.jabref.BibtexFields;
import net.sf.jabref.FocusRequester;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.HelpAction;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.Util;
import net.sf.jabref.gui.ImportInspectionDialog;

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
    JButton go = new JButton(Globals.lang("Fetch")), helpBut = new JButton(
			GUIGlobals.getImage("helpSmall")), reset = new JButton(Globals
			.lang("Reset"));
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
        tf.setPreferredSize(new Dimension(1,tf.getPreferredSize().height));
        
        tf.setName("tf");
		// add action to reset-button. resets tf and requests focus
		reset.addActionListener(new AbstractAction() {
			public void actionPerformed(ActionEvent event) {
				tf.setText("");
				new FocusRequester(tf);
			}
		});

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
        
        // Go Button
        con.weighty = 0;
        con.gridwidth = 1;
        gbl.setConstraints(go, con);
        main.add(go);
        
        // Reset Button
		if (fetcher.getHelpPage() != null) {
			con.gridwidth = 1;
		} else {
			con.gridwidth = GridBagConstraints.REMAINDER;
		}
		reset.setName("reset");
		gbl.setConstraints(reset, con);
		main.add(reset);
        
        // Help Button
		if (fetcher.getHelpPage() != null) {
			con.gridwidth = GridBagConstraints.REMAINDER;
			gbl.setConstraints(helpBut, con);
			main.add(helpBut);
		}
        
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

    public void setHelpResourceOwner(Class c) {
        help.setResourceOwner(c);
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
        final ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.basePanel(),
                BibtexFields.DEFAULT_INSPECTION_FIELDS, fetcher.getTitle(), false);
        dialog.addCallBack(fetcher);
        Util.placeDialog(dialog, frame);
        dialog.setVisible(true);
        
        new Thread(new Runnable(){
            public void run(){
                
                if (fetcher.processQuery(tf.getText().trim(), dialog, frame)){
                    dialog.entryListComplete();
                } else {
                    dialog.dispose();
                }
            }
        }).start();
    }

    class FetcherAction extends AbstractAction {
        public FetcherAction() {
            super(fetcher.getTitle(), new ImageIcon(fetcher.getIcon()));
            if ((fetcher.getKeyName() != null) && (fetcher.getKeyName().length() > 0))
                putValue(ACCELERATOR_KEY, Globals.prefs.getKey(fetcher.getKeyName()));
        }
        public void actionPerformed(ActionEvent e) {
        	String fetcherTitle = fetcher.getTitle();
        	
        	if (!sidePaneManager.hasComponent(fetcherTitle)){
        		sidePaneManager.register(fetcherTitle, GeneralFetcher.this);
        	}
        	
        	if (frame.getTabbedPane().getTabCount() > 0) {
				sidePaneManager.toggle(fetcherTitle);
				if (sidePaneManager.isComponentVisible(fetcherTitle)) {
					new FocusRequester(getTextField());
				}
			}        	
        }
    }
}
