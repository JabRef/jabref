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

class SearchManager extends JPanel
    implements ActionListener, KeyListener, ItemListener {
    
    GridBagLayout gbl = new GridBagLayout() ; 
    GridBagConstraints con = new GridBagConstraints() ; 

    private JabRefFrame frame;
    private JTextField searchField = new JTextField("", 30);
    private JLabel lab = new JLabel(Globals.lang("Search")+":");
    private JPopupMenu settings = new JPopupMenu();
    private JButton openset = new JButton(Globals.lang("Settings")),
	escape = new JButton(Globals.lang("Unshow"));
    private JabRefPreferences prefs;
    private JCheckBoxMenuItem searchReq, searchOpt, searchGen, 
	searchAll, caseSensitive, regExpSearch;
    private JCheckBox increment, select, reorder;
    private ButtonGroup types = new ButtonGroup();
    private SearchManager ths = this;

    public SearchManager(JabRefFrame frame, JabRefPreferences prefs_) {
	this.frame = frame;
	prefs = prefs_;

	setBorder(BorderFactory.createEtchedBorder());

        searchReq = new JCheckBoxMenuItem("Search required fields",
				  prefs.getBoolean("searchReq"));
	searchOpt = new JCheckBoxMenuItem("Search optional fields",
				  prefs.getBoolean("searchOpt"));
	searchGen = new JCheckBoxMenuItem("Search general fields",
				  prefs.getBoolean("searchGen"));
        searchAll = new JCheckBoxMenuItem("Search all fields",
				  prefs.getBoolean("searchAll"));
        regExpSearch = new JCheckBoxMenuItem("Use regular expressions",
					     prefs.getBoolean("regExpSearch"));
	increment = new JCheckBox(Globals.lang("Incremental"), false);
	select = new JCheckBox(Globals.lang("Highlight"), true);
	reorder = new JCheckBox(Globals.lang("Float"), false);

	// Add an item listener that makes sure we only listen for key events
	// when incremental search is turned on.
	increment.addItemListener(this);
	reorder.addItemListener(this);

	if (searchAll.isSelected()) {
	    searchReq.setEnabled(false);
	    searchOpt.setEnabled(false);
	    searchGen.setEnabled(false);
	}
        caseSensitive = new JCheckBoxMenuItem("Case sensitive",
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
	escape.addActionListener(this);

	openset.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JButton src = (JButton)e.getSource();
		    settings.show(src, 0, 0);
		}
	    });

	types.add(increment);
	types.add(select);
	types.add(reorder);
	if (prefs.getBoolean("incrementS"))
	    increment.setSelected(true);
	else if (!prefs.getBoolean("selectS"))
	    reorder.setSelected(true);
	

	setLayout(gbl);
	con.insets = new Insets(2, 2, 0,  2);
        con.anchor = GridBagConstraints.WEST;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.gridwidth = 1;

        gbl.setConstraints(lab, con);
        add(lab); 
	con.gridwidth = 3;
        gbl.setConstraints(searchField,con);
        add(searchField) ; 
        //con.anchor = GridBagConstraints.CENTER;
	//con.insets = new Insets(0, 10, 0, 10);
        //gbl.setConstraints(searchButton,con);
        //getContentPane().add(searchButton) ; 
	con.gridwidth = GridBagConstraints.REMAINDER;
	//	con.gridheight = 2;
	con.fill = GridBagConstraints.BOTH;
        gbl.setConstraints(openset,con);
        add(openset); 
	con.fill = GridBagConstraints.HORIZONTAL;
	//	con.gridheight = 1;
	con.gridwidth = 1;
	con.insets = new Insets(0, 0, 0,  0);
	JPanel empt = new JPanel();
	gbl.setConstraints(empt, con);
        add(empt); 
	gbl.setConstraints(increment, con);
        add(increment); 
	gbl.setConstraints(select, con);
        add(select); 
	gbl.setConstraints(reorder, con);
        add(reorder); 
	con.insets = new Insets(0, 2, 2,  2);
	gbl.setConstraints(escape, con);
        add(escape); 

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

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == escape)
	    frame.basePanel().stopShowingSearchResults();
	else if ((e.getSource() == searchField) 
		 && !increment.isSelected()) {
	    updatePrefs(); // Make sure the user's choices are recorded.

	    // Setup search parameters common to both highlight and float.
	    Hashtable searchOptions = new Hashtable();
	    searchOptions.put("option",searchField.getText()) ; 
	    SearchRuleSet searchRules = new SearchRuleSet() ; 
	    SearchRule rule1;
	    if (prefs.getBoolean("regExpSearch"))
		rule1 = new RegExpRule(prefs);
	    else
		rule1 = new SimpleSearchRule(prefs);	    
	    searchRules.addRule(rule1) ; 

	    if (reorder.isSelected()) {
		// Float search.
		DatabaseSearch search = new DatabaseSearch
		    (searchOptions,searchRules, frame.basePanel(),
		     DatabaseSearch.SEARCH, true); 
		search.start() ; 
	    }
	    else if (select.isSelected()) {
		// Highlight search.
		DatabaseSearch search = new DatabaseSearch
		    (searchOptions,searchRules, frame.basePanel(),
		     DatabaseSearch.SEARCH, false); 
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
   
    /**
     * Used for incremental search. Only activated when incremental
     * is selected.
     */
    public void keyTyped(KeyEvent e) {

    }

    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

}
