/*
Copyright (C) 2003  Nathan Dunn, Morten O. Alver

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

// This pane has all of the search-options available for a single bibtex
// category initially this is done with one category/field, but can be expanded
// via standard search expansion options, by adding more categoryies.  
//
// Will use java's regexp (standard) in each category.
//
// Output can be a highlighting/sorting of the original table (so will need a 
// reference to the table, obviously, with accessor methods), or a duplication
// of the table, or hiding performed directly on the original table.

import javax.swing.* ; 
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.* ; 
import java.awt.event.* ; 
import java.util.Vector ; 
import java.util.Hashtable ; 

public class SearchPane extends JDialog {

    JButton searchButton = null ; 
    JTextField searchField = null ; 
    JTextArea searchArea = null ; 
    Vector fields = null ; 
    Hashtable searchOptions = new Hashtable() ; 
    JabRefFrame thisOwner = null ; 
    BasePanel panel;
    JabRefPreferences prefs ;
    HelpAction helpAction;

    JCheckBoxMenuItem searchReq, searchOpt, searchGen, searchAll, caseSensitive, regExpSearch;
    JPopupMenu settings = new JPopupMenu();
    JButton openset = new JButton("Settings");


    SearchPane(JabRefFrame owner, BasePanel panel,
	       JabRefPreferences prefs, 
	       String title, boolean modal){
		super((Frame) owner,title,modal) ; 
        thisOwner = owner ;
	this.panel = panel;
	this.prefs = prefs;
	helpAction = new HelpAction(thisOwner.helpDiag, 
				    GUIGlobals.searchHelp, "Help");
        init() ; 
    }

    public void init(){
        //setDefaultCloseOperation(HIDE_ON_CLOSE) ; 
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    close();
		}
	    });
        searchButton = new JButton("Search") ; 
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

	openset.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    JButton src = (JButton)e.getSource();
		    settings.show(src, 0, 0);
		}
	    });

//        searchButton.setDefaultCapable(true) ; 
//        getRootPane().setDefaultButton(searchButton) ; 
        searchButton.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e) {
                if(searchField.getText().length()>0){
                    searchOptions.clear() ; 
                    searchOptions.put("option",searchField.getText()) ; 
                    doSearch(searchOptions) ; 
                }
            }
        }) ; 
        searchButton.addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){
                int keyStroke = e.getKeyCode() ; 
                if(keyStroke == e.VK_ENTER){
                    if(searchField.getText().length()>0){
                        searchOptions.clear() ; 
                        searchOptions.put("option",searchField.getText()) ; 
                        doSearch(searchOptions) ; 
                    }
                }
                else 
                if(keyStroke == e.VK_ESCAPE){
					close() ; 
                }
		else if (keyStroke == e.VK_F1) {
		    helpAction.actionPerformed(null);
		}

            }
        }) ; 

        searchField= new JTextField(110); 
	searchField.setMinimumSize(GUIGlobals.searchFieldSize) ; 
        searchField.addKeyListener(new KeyAdapter(){
            public void keyPressed(KeyEvent e){
                int keyStroke = e.getKeyCode() ; 
                if(keyStroke == e.VK_ENTER){
                    if(searchField.getText().length()>0){
                        searchOptions.clear() ; 
                        searchOptions.put("option",searchField.getText()) ; 
                        doSearch(searchOptions) ; 
                    }
                }
                else 
                if(keyStroke == e.VK_ESCAPE){
					close() ; 
                }
		else if (keyStroke == e.VK_F1) {
		    helpAction.actionPerformed(null);
		}
            }
        }) ; 

	searchAll.addChangeListener(new ChangeListener() {
		public void stateChanged(ChangeEvent e) {
		    boolean set = !searchAll.isSelected();
		    searchReq.setEnabled(set);
		    searchOpt.setEnabled(set);
		    searchGen.setEnabled(set);
		    searchField.requestFocus();
		}
	    });

        GridBagLayout gbl = new GridBagLayout() ; 
        GridBagConstraints con = new GridBagConstraints() ; 
	//        gbl.setConstraints(this,con);
        getContentPane().setLayout(gbl);

        con.anchor = GridBagConstraints.WEST;
	con.fill = GridBagConstraints.HORIZONTAL;
	con.gridwidth = 1;
        gbl.setConstraints(searchField,con);
        getContentPane().add(searchField) ; 
        //con.anchor = GridBagConstraints.CENTER;
	con.insets = new Insets(0, 10, 0, 10);
        gbl.setConstraints(searchButton,con);
        getContentPane().add(searchButton) ; 
	con.gridwidth = GridBagConstraints.REMAINDER;
	con.insets = new Insets(0, 0, 0,  0);
        gbl.setConstraints(openset,con);
        getContentPane().add(openset); 
				  /*				 
        gbl.setConstraints(caseSensitive,con);
        getContentPane().add(caseSensitive); 
	con.insets = new Insets(10, 0, 0, 0);
        gbl.setConstraints(searchReq,con);
        getContentPane().add(searchReq); 
	con.insets = new Insets(0, 0, 0, 0);
        gbl.setConstraints(searchOpt,con);
        getContentPane().add(searchOpt); 
        gbl.setConstraints(searchGen,con);
        getContentPane().add(searchGen); 
	con.insets = new Insets(10, 0, 0, 0);
        gbl.setConstraints(searchAll,con);
        getContentPane().add(searchAll);        
				  */
	setSize(GUIGlobals.searchPaneSize) ; 
	setLocation(prefs.getInt("searchPanePosX"),
		    prefs.getInt("searchPanePosY"));
	//pack();
	setVisible(true) ; 
        searchField.requestFocus() ; 
        searchField.select(0,searchField.getText().length()) ; 

    }


    //  runs a thread in DatabaseSearch
    public void doSearch(Hashtable searchOptions){

	updatePrefs(); // Make sure the user's choices are recorded.

        // this is where we switch the search-type, if we want another 
        // search-type as default
	SearchRuleSet searchRules = new SearchRuleSet() ; 
        SearchRule rule1;
	if (prefs.getBoolean("regExpSearch"))
	    rule1 = new RegExpRule(prefs);
	else
	    rule1 = new SimpleSearchRule(prefs);
 
        searchRules.addRule(rule1) ; 
        DatabaseSearch search = new DatabaseSearch(searchOptions,searchRules,
						   panel, DatabaseSearch.SEARCH) ; 
        search.start() ; 
        searchField.select(0,searchField.getText().length()) ; 
    }

    protected void updatePrefs() {
	prefs.putBoolean("searchReq", searchReq.isSelected());
	prefs.putBoolean("searchOpt", searchOpt.isSelected());
	prefs.putBoolean("searchGen", searchGen.isSelected());
	prefs.putBoolean("searchAll", searchAll.isSelected());
	prefs.putBoolean("caseSensitiveSearch", 
			 caseSensitive.isSelected());
	prefs.putBoolean("regExpSearch", regExpSearch.isSelected());

    }

    protected void close() {
	updatePrefs();
	prefs.putInt("searchPanePosX", getLocation().x);
	prefs.putInt("searchPanePosY", getLocation().x);

	panel.stopShowingSearchResults();
	dispose();
    }

}

