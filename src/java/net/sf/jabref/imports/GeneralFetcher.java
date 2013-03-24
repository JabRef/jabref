/*  Copyright (C) 2003-2011 JabRef contributors.
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
package net.sf.jabref.imports;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.swing.*;

import net.sf.jabref.BibtexFields;
import net.sf.jabref.FocusRequester;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.SidePaneComponent;
import net.sf.jabref.SidePaneManager;
import net.sf.jabref.Util;
import net.sf.jabref.gui.FetcherPreviewDialog;
import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.help.HelpAction;

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
    JComboBox fetcherChoice;
    CardLayout optionsCards = new CardLayout();
    JPanel optionsPanel = new JPanel(optionsCards);
    JPanel optPanel = new JPanel(new BorderLayout());
    HelpAction help;

    SidePaneManager sidePaneManager;
    Action action;
    JabRefFrame frame;
    EntryFetcher activeFetcher;
    EntryFetcher[] fetcherArray;

    public GeneralFetcher(SidePaneManager p0, JabRefFrame frame, final List<EntryFetcher> fetchers) {
        super(p0, GUIGlobals.getIconUrl("www"), Globals.lang("Web search"));
        this.sidePaneManager = p0;
        this.frame = frame;
        fetcherArray = fetchers.toArray(new EntryFetcher[fetchers.size()]);
        Arrays.sort(fetcherArray, new EntryFetcherComparator());
        //JLabel[] choices = new JLabel[fetchers.size()];
        String[] choices = new String[fetcherArray.length];
        for (int i=0; i<fetcherArray.length; i++) {
            choices[i] = fetcherArray[i].getTitle();
            //choices[i] = new JLabel(fetchers.get(i).getTitle(), new ImageIcon(fetchers.get(i).getIcon()),
            //        JLabel.HORIZONTAL);
            /*if (fetchers.get(i).getOptionsPanel() != null)
                optionsPanel.add(fetchers.get(i).getOptionsPanel(), String.valueOf(i));
            else
                optionsPanel.add(new JPanel(), String.valueOf(i));*/
        }
        fetcherChoice = new JComboBox(choices);
        int defaultFetcher = Globals.prefs.getInt("selectedFetcherIndex");
        if (defaultFetcher >= fetcherArray.length)
            defaultFetcher = 0;
        this.activeFetcher = fetcherArray[defaultFetcher];
        fetcherChoice.setSelectedIndex(defaultFetcher);
        if (this.activeFetcher.getOptionsPanel() != null)
            optPanel.add(this.activeFetcher.getOptionsPanel(), BorderLayout.CENTER);
        helpBut.setEnabled(activeFetcher.getHelpPage() != null);

        //optionsCards.show(optionsPanel, String.valueOf(defaultFetcher));

        /*fetcherChoice.setRenderer(new ListCellRenderer() {
            JLabel label = new JLabel();
            public Component getListCellRendererComponent(JList jList, Object o, int i, boolean isSelected,
                boolean cellHasFocus) {
                JLabel theLab = (JLabel)o;
                label.setIcon(theLab.getIcon());
                label.setText(theLab.getText());
                if (cellHasFocus) {
                    label.setBackground(UIManager.getDefaults().getColor("ComboBox.selectionBackground").darker());
                    label.setForeground(UIManager.getDefaults().getColor("ComboBox.foreground"));
                } else {
                    label.setBackground(UIManager.getDefaults().getColor("ComboBox.background"));
                    label.setForeground(UIManager.getDefaults().getColor("ComboBox.foreground"));
                }
                return label;
            }
        });*/
        fetcherChoice.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                activeFetcher = fetcherArray[fetcherChoice.getSelectedIndex()];
                Globals.prefs.putInt("selectedFetcherIndex", fetcherChoice.getSelectedIndex());
                if (activeFetcher.getHelpPage() != null) {
                    help.setHelpFile(activeFetcher.getHelpPage());
                    helpBut.setEnabled(true);
                } else {
                    helpBut.setEnabled(false);
                }
                optionsCards.show(optionsPanel, String.valueOf(fetcherChoice.getSelectedIndex()));
                optPanel.removeAll();
                if (activeFetcher.getOptionsPanel() != null)
                    optPanel.add(activeFetcher.getOptionsPanel(), BorderLayout.CENTER);
                revalidate();
            }
        });

        action = new FetcherAction();

        help = new HelpAction(Globals.helpDiag, activeFetcher.getHelpPage(), "Help");

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
        con.insets = new Insets(1, 0, 1, 0);
        con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(fetcherChoice, con);
        main.add(fetcherChoice);
        con.insets = new Insets(0, 0, 0, 0);
        gbl.setConstraints(tf, con);
        main.add(tf);

        // Go Button
        con.weighty = 0;
        con.gridwidth = 1;
        gbl.setConstraints(go, con);
        main.add(go);
        
        // Reset Button
		reset.setName("reset");
		gbl.setConstraints(reset, con);
		main.add(reset);
        
        // Help Button
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(helpBut, con);
        main.add(helpBut);

        if (pan != null) {
            gbl.setConstraints(optPanel, con);
            main.add(optPanel);
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
        if (tf.getText().trim().length() == 0) {    
	    frame.output(Globals.lang("Please enter a search string")); 
	    return;
	}

	if(frame.basePanel() == null) {
	    frame.output(Globals.lang("Please open or start a new database before searching")); 
	    return;
	} 

        // We have two categories of fetchers. One category can show previews first and ask the
        // user which ones to download:
	if (activeFetcher instanceof PreviewEntryFetcher) {
	    frame.output(Globals.lang("Searching..."));
	    frame.setProgressBarIndeterminate(true);
	    frame.setProgressBarVisible(true);
	    final PreviewEntryFetcher pFetcher = (PreviewEntryFetcher)activeFetcher;
	    final FetcherPreviewDialog dialog = new FetcherPreviewDialog(frame,
									 pFetcher.getWarningLimit(), pFetcher.getPreferredPreviewHeight());
	    new Thread(new Runnable(){
		    public void run(){
			final boolean result = pFetcher.processQueryGetPreview(tf.getText().trim(), dialog, dialog);
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    frame.setProgressBarVisible(false);
				    frame.output("");
				    if (!result)
					return;
				    dialog.setLocationRelativeTo(frame);
				    dialog.setVisible(true);
				    if (dialog.isOkPressed()) {
					final ImportInspectionDialog d2 = new ImportInspectionDialog(frame, frame.basePanel(),
												     BibtexFields.DEFAULT_INSPECTION_FIELDS, activeFetcher.getTitle(), false);
					d2.addCallBack(activeFetcher);
					Util.placeDialog(d2, frame);
					d2.setVisible(true);
					new Thread(new Runnable() {
						public void run() {
						    pFetcher.getEntries(dialog.getSelection(), d2);
						    d2.entryListComplete();
						}
					    }).start();
					
				    }
				}
			    });
			
			
		    }
		}).start();
	}
	
	// The other category downloads the entries first, then asks the user which ones to keep:
	else {
	    final ImportInspectionDialog dialog = new ImportInspectionDialog(frame, frame.basePanel(),
									     BibtexFields.DEFAULT_INSPECTION_FIELDS, activeFetcher.getTitle(), false);
	    dialog.addCallBack(activeFetcher);
	    Util.placeDialog(dialog, frame);
	    dialog.setVisible(true);
	    
	    new Thread(new Runnable(){
		    public void run(){
			
			if (activeFetcher.processQuery(tf.getText().trim(), dialog, dialog)){
			    dialog.entryListComplete();
			} else {
			    dialog.dispose();
			}
		    }
		}).start();
	}
    }


    class FetcherAction extends AbstractAction {
        public FetcherAction() {
            super(Globals.lang("Web search"), GUIGlobals.getImage("www"));
            //if ((activeFetcher.getKeyName() != null) && (activeFetcher.getKeyName().length() > 0))
            putValue(ACCELERATOR_KEY, Globals.prefs.getKey("Fetch Medline"));
        }
        public void actionPerformed(ActionEvent e) {
        	if (!sidePaneManager.hasComponent(GeneralFetcher.this.getTitle())){
        		sidePaneManager.register(GeneralFetcher.this.getTitle(), GeneralFetcher.this);
        	}
        	
        	if (frame.getTabbedPane().getTabCount() > 0) {
				sidePaneManager.toggle(GeneralFetcher.this.getTitle());
				if (sidePaneManager.isComponentVisible(GeneralFetcher.this.getTitle())) {
					new FocusRequester(getTextField());
				}
			}        	
        }
    }

    @Override
    public void componentClosing() {
        super.componentClosing();
        Globals.prefs.putBoolean("webSearchVisible", Boolean.FALSE);
    }

    @Override
    public void componentOpening() {
        super.componentOpening();
        Globals.prefs.putBoolean("webSearchVisible", Boolean.TRUE);
    }

    class EntryFetcherComparator implements Comparator<EntryFetcher> {
        public int compare(EntryFetcher entryFetcher, EntryFetcher entryFetcher1) {
            return entryFetcher.getTitle().compareTo(entryFetcher1.getTitle());
        }
    }
}
