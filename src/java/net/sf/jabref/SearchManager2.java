/*
Copyright (C) 2003 JabRef team

All programs in this directory and
subdirectories are published under the GNU General Public License as
described below.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
USA

Further information about the GNU GPL is available at:
http://www.gnu.org/copyleft/gpl.ja.html

*/
package net.sf.jabref;

import java.util.Hashtable;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import net.sf.jabref.search.SearchExpression;

class SearchManager2 extends SidePaneComponent
    implements ActionListener, KeyListener, ItemListener {

    GridBagLayout gbl = new GridBagLayout() ;
    GridBagConstraints con = new GridBagConstraints() ;

    IncrementalSearcher incSearcher;

    private JabRefFrame frame;
    private JTextField searchField = new JTextField("", 12);
    private JLabel lab = //new JLabel(Globals.lang("Search")+":");
	new JLabel(new ImageIcon(GUIGlobals.searchIconFile));
    private JPopupMenu settings = new JPopupMenu();
    private JButton openset = new JButton(Globals.lang("Settings")),
	escape = new JButton(Globals.lang("Clear search")),
        help = new JButton(new ImageIcon(GUIGlobals.helpIconFile));
    private JabRefPreferences prefs;
    private JCheckBoxMenuItem searchReq, searchOpt, searchGen,
	searchAll, caseSensitive, regExpSearch;
    private JCheckBox increment, select, reorder;
    private ButtonGroup types = new ButtonGroup();
    private SearchManager2 ths = this;
    private boolean incSearch = false;

    private int incSearchPos = -1; // To keep track of where we are in
				   // an incremental search. -1 means
				   // that the search is inactive.

    public SearchManager2(JabRefFrame frame, JabRefPreferences prefs_,
			 SidePaneManager manager) {
	super(manager);
	this.frame = frame;
	prefs = prefs_;
	incSearcher = new IncrementalSearcher(prefs);

	//setBorder(BorderFactory.createEtchedBorder());

        searchReq = new JCheckBoxMenuItem
	    (Globals.lang("Search required fields"),
	     prefs.getBoolean("searchReq"));
	searchOpt = new JCheckBoxMenuItem
	    (Globals.lang("Search optional fields"),
	     prefs.getBoolean("searchOpt"));
	searchGen = new JCheckBoxMenuItem
	    (Globals.lang("Search general fields"),
	     prefs.getBoolean("searchGen"));
        searchAll = new JCheckBoxMenuItem
	    (Globals.lang("Search all fields"),
	     prefs.getBoolean("searchAll"));
        regExpSearch = new JCheckBoxMenuItem
	    (Globals.lang("Use regular expressions"),
	     prefs.getBoolean("regExpSearch"));
	increment = new JCheckBox(Globals.lang("Incremental"), false);
	select = new JCheckBox(Globals.lang("Highlight"), true);
	reorder = new JCheckBox(Globals.lang("Float"), false);

	// Add an item listener that makes sure we only listen for key events
	// when incremental search is turned on.
	increment.addItemListener(this);
	reorder.addItemListener(this);

        // Add the global focus listener, so a menu item can see if this field was focused when
        // an action was called.
        searchField.addFocusListener(Globals.focusListener);


	if (searchAll.isSelected()) {
	    searchReq.setEnabled(false);
	    searchOpt.setEnabled(false);
	    searchGen.setEnabled(false);
	}
        caseSensitive = new JCheckBoxMenuItem(Globals.lang("Case sensitive"),
				      prefs.getBoolean("caseSensitiveSearch"));
	settings.add(caseSensitive);
	settings.addSeparator();
	settings.add(searchReq);
	settings.add(searchOpt);
	settings.add(searchGen);
	settings.addSeparator();
	settings.add(searchAll);
	settings.addSeparator();
	settings.add(regExpSearch);

	searchField.addActionListener(this);
	searchField.addFocusListener(new FocusAdapter() {
		public void focusLost(FocusEvent e) {
		    incSearch = false;
		    incSearchPos = -1; // Reset incremental
				       // search. This makes the
				       // incremental search reset
				       // once the user moves focus to
				       // somewhere else.
		    if (increment.isSelected())
			searchField.setText("");
		}
	    });
	escape.addActionListener(this);

	openset.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JButton src = (JButton)e.getSource();
		    settings.show(src, 0, 0);
		}
	    });

            help.addActionListener(new HelpAction(frame.helpDiag, GUIGlobals.searchHelp, "Help"));
	types.add(increment);
	types.add(select);
	types.add(reorder);
	if (prefs.getBoolean("incrementS"))
	    increment.setSelected(true);
	else if (!prefs.getBoolean("selectS"))
	    reorder.setSelected(true);


	setLayout(gbl);
	SidePaneHeader header = new SidePaneHeader
	    ("Search", GUIGlobals.searchIconFile, this);
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;
	con.insets = new Insets(0, 0, 2,  0);
	gbl.setConstraints(header, con);
	add(header);
        con.insets = new Insets(0, 0, 0,  0);
        gbl.setConstraints(searchField,con);
        add(searchField) ;
	gbl.setConstraints(increment, con);
        add(increment);
	gbl.setConstraints(select, con);
        add(select);
	gbl.setConstraints(reorder, con);
        add(reorder);
	con.gridwidth = 1;
        gbl.setConstraints(openset,con);
        add(openset);
	//JPanel empt = new JPanel();
	//con.insets = new Insets(0, 0, 2, 0);
	con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(help,con);
        add(help);
        con.gridwidth = 2;
	gbl.setConstraints(escape, con);
        add(escape);

	searchField.getInputMap().put(prefs.getKey("Repeat incremental search"),
				      "repeat");

	searchField.getActionMap().put("repeat", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    if (increment.isSelected())
			repeatIncremental();
		}
	    });
	searchField.getInputMap().put(prefs.getKey("Clear search"), "escape");
	searchField.getActionMap().put("escape", new AbstractAction() {
		public void actionPerformed(ActionEvent e) {
		    ths.actionPerformed(new ActionEvent(escape, 0, ""));
		}
	    });
    }

    protected void updatePrefs() {
	prefs.putBoolean("searchReq", searchReq.isSelected());
	prefs.putBoolean("searchOpt", searchOpt.isSelected());
	prefs.putBoolean("searchGen", searchGen.isSelected());
	prefs.putBoolean("searchAll", searchAll.isSelected());
	prefs.putBoolean("incrementS", increment.isSelected());
	prefs.putBoolean("selectS", select.isSelected());
	prefs.putBoolean("caseSensitiveSearch",
			 caseSensitive.isSelected());
	prefs.putBoolean("regExpSearch", regExpSearch.isSelected());

    }

    public void startIncrementalSearch() {
	increment.setSelected(true);
	searchField.setText("");
	searchField.requestFocus();
    }

    /**
     * Clears and focuses the search field if it is not
     * focused. Otherwise, cycles to the next search type.
     */
    public void startSearch() {
	if (increment.isSelected() && incSearch) {
	    repeatIncremental();
	    return;
	}
	if (!searchField.hasFocus()) {
	    searchField.setText("");
	    searchField.requestFocus();
	} else {
	    if (increment.isSelected())
		select.setSelected(true);
	    else if (select.isSelected())
		reorder.setSelected(true);
	    else {
		increment.setSelected(true);
	    }
	    increment.revalidate();
	    increment.repaint();
	    searchField.requestFocus();
	}
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == escape) {
	    incSearch = false;
	    if (frame.basePanel() != null)
		frame.basePanel().stopShowingSearchResults();
	}
	else if ((e.getSource() == searchField)
		 && !increment.isSelected()
		 && (frame.basePanel() != null)) {
	    updatePrefs(); // Make sure the user's choices are recorded.
            if (searchField.getText().equals("")) {
              // An empty search field should cause the search to be cleared.
              frame.stopShowingSearchResults();
              return;
            }
	    // Setup search parameters common to both highlight and float.
	    Hashtable searchOptions = new Hashtable();
	    searchOptions.put("option",searchField.getText()) ;
	    SearchRuleSet searchRules = new SearchRuleSet() ;
	    SearchRule rule1;
	    if (prefs.getBoolean("regExpSearch"))
		rule1 = new RegExpRule(prefs);
	    else
		rule1 = new SimpleSearchRule(prefs);

		try {
			rule1 = new SearchExpression(prefs,searchOptions);
		} catch (Exception ex) {
                  ex.printStackTrace();
			// JZ: for testing; this does the new search if the
			// search text is in correct syntax, and the regular search otherwise
		}
//		} catch (PatternSyntaxException ex) {
//			System.out.println(ex);
//			return;
//		} catch (TokenStreamException ex) {
//			System.out.println(ex);
//			return;
//		} catch (RecognitionException ex) {
//			System.out.println(ex);
//			return;
//		}

	    searchRules.addRule(rule1) ;

	    if (reorder.isSelected()) {
		// Float search.
		DatabaseSearch search = new DatabaseSearch
		    (searchOptions,searchRules, frame.basePanel(),
		     Globals.SEARCH, true);
		search.start() ;
	    }
	    else if (select.isSelected()) {
		// Highlight search.
		DatabaseSearch search = new DatabaseSearch
		    (searchOptions,searchRules, frame.basePanel(),
		     Globals.SEARCH, false);
		search.start() ;
	    }

	    // Afterwards, select all text in the search field.
	    searchField.select(0,searchField.getText().length()) ;
	}
    }

    public void itemStateChanged(ItemEvent e) {
	if (e.getSource() == increment) {
	    if (increment.isSelected())
		searchField.addKeyListener(ths);
	    else
		searchField.removeKeyListener(ths);
	} else if (e.getSource() == reorder) {
	    // If this search type is disabled, remove reordering from
	    // all databases.
	    if (!reorder.isSelected()) {
		frame.stopShowingSearchResults();
	    }
	}
    }

    private void repeatIncremental() {
	incSearchPos++;
	if (frame.basePanel() != null)
	    goIncremental();
    }

    /**
     * Used for incremental search. Only activated when incremental
     * is selected.
     *
     * The variable incSearchPos keeps track of which entry was last
     * checked.
     */
    public void keyTyped(KeyEvent e) {
	if (e.isControlDown()) {
	    return;
	}
	if (frame.basePanel() != null)
	    goIncremental();
    }

    private void goIncremental() {
	incSearch = true;
	SwingUtilities.invokeLater(new Thread() {
		public void run() {
		    String text = searchField.getText();
		    BasePanel bp = frame.basePanel();

		    if (incSearchPos >= bp.getDatabase().getEntryCount()) {
			frame.output("'"+text+"' : "+Globals.lang
				     ("Incremental search failed. Repeat to search from top.")+".");
			incSearchPos = -1;
			return;
		    }

		    if (searchField.getText().equals("")) return;
		    if (incSearchPos < 0)
			incSearchPos = 0;
		    BibtexEntry be = bp.getDatabase().getEntryById
			(bp.tableModel.getNameFromNumber(incSearchPos));
		    while (!incSearcher.search(text, be)) {
			incSearchPos++;
			if (incSearchPos < bp.getDatabase().getEntryCount())
			    be = bp.getDatabase().getEntryById
				(bp.tableModel.getNameFromNumber(incSearchPos));
			else {
			    frame.output("'"+text+"' : "+Globals.lang
					 ("Incremental search failed. Repeat to search from top."));
			    incSearchPos = -1;
			    return;
			}
		    }
		    if (incSearchPos >= 0) {
			bp.selectSingleEntry(incSearchPos);
			frame.output("'"+text+"' "+Globals.lang
				     ("found")+".");
		    }
		}
	    });
    }

    public void componentClosing() {
	BasePanel bp = frame.basePanel();
	if (bp != null)
	    bp.stopShowingSearchResults();
    }


    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

}
