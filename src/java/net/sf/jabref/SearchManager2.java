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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.Hashtable;

import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.sf.jabref.search.BasicSearch;
import net.sf.jabref.search.SearchExpression;
import net.sf.jabref.search.SearchExpressionParser;
import net.sf.jabref.search.SearchMatcher;
import net.sf.jabref.gui.SearchResultsDialog;

class SearchManager2 extends SidePaneComponent
    implements ActionListener, KeyListener, ItemListener, CaretListener, ErrorMessageDisplay {

    private JabRefFrame frame;

    GridBagLayout gbl = new GridBagLayout() ;
    GridBagConstraints con = new GridBagConstraints() ;

    IncrementalSearcher incSearcher;
    SearchResultsDialog searchDialog = null;

    //private JabRefFrame frame;
    private JTextField searchField = new JTextField("", 12);
    private JPopupMenu settings = new JPopupMenu();
    private JButton openset = new JButton(Globals.lang("Settings"));
    private JButton escape = new JButton(Globals.lang("Clear"));
    private JButton help = new JButton(GUIGlobals.getImage("help"));
    /** This button's text will be set later. */
    private JButton search = new JButton();
    private JCheckBoxMenuItem searchReq, searchOpt, searchGen,
    searchAll, caseSensitive, regExpSearch;

    private JRadioButton increment, floatSearch, hideSearch, showResultsInDialog,
        searchAllBases;
    private JCheckBoxMenuItem select;
    private ButtonGroup types = new ButtonGroup();
    private boolean incSearch = false, startedFloatSearch=false, startedFilterSearch=false;

    private int incSearchPos = -1; // To keep track of where we are in
                   // an incremental search. -1 means
                   // that the search is inactive.


    public SearchManager2(JabRefFrame frame, SidePaneManager manager) {
    super(manager, GUIGlobals.getIconUrl("search"), Globals.lang("Search"));

        this.frame = frame;
    incSearcher = new IncrementalSearcher(Globals.prefs);



    //setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.magenta));

        searchReq = new JCheckBoxMenuItem
        (Globals.lang("Search required fields"),
         Globals.prefs.getBoolean("searchReq"));
    searchOpt = new JCheckBoxMenuItem
        (Globals.lang("Search optional fields"),
         Globals.prefs.getBoolean("searchOpt"));
    searchGen = new JCheckBoxMenuItem
        (Globals.lang("Search general fields"),
         Globals.prefs.getBoolean("searchGen"));
        searchAll = new JCheckBoxMenuItem
        (Globals.lang("Search all fields"),
         Globals.prefs.getBoolean("searchAll"));
        regExpSearch = new JCheckBoxMenuItem
        (Globals.lang("Use regular expressions"),
         Globals.prefs.getBoolean("regExpSearch"));

    increment = new JRadioButton(Globals.lang("Incremental"), false);
    floatSearch = new JRadioButton(Globals.lang("Float"), true);
    hideSearch = new JRadioButton(Globals.lang("Filter"), true);
    showResultsInDialog = new JRadioButton(Globals.lang("Show results in dialog"), true);
    searchAllBases = new JRadioButton(Globals.lang("Global search"),
            Globals.prefs.getBoolean("searchAllBases"));
    types.add(increment);
    types.add(floatSearch);
    types.add(hideSearch);
    types.add(showResultsInDialog);
    types.add(searchAllBases);

        select = new JCheckBoxMenuItem(Globals.lang("Select matches"), false);
        increment.setToolTipText(Globals.lang("Incremental search"));
        floatSearch.setToolTipText(Globals.lang("Gray out non-matching entries"));
        hideSearch.setToolTipText(Globals.lang("Hide non-matching entries"));
        showResultsInDialog.setToolTipText(Globals.lang("Show search results in a window"));

    // Add an item listener that makes sure we only listen for key events
    // when incremental search is turned on.
    increment.addItemListener(this);
    floatSearch.addItemListener(this);
    hideSearch.addItemListener(this);
    showResultsInDialog.addItemListener(this);
        // Add the global focus listener, so a menu item can see if this field was focused when
        // an action was called.
        searchField.addFocusListener(Globals.focusListener);


    if (searchAll.isSelected()) {
        searchReq.setEnabled(false);
        searchOpt.setEnabled(false);
        searchGen.setEnabled(false);
    }
    searchAll.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent event) {
            boolean state = !searchAll.isSelected();
            searchReq.setEnabled(state);
            searchOpt.setEnabled(state);
            searchGen.setEnabled(state);
        }
    });

        caseSensitive = new JCheckBoxMenuItem(Globals.lang("Case sensitive"),
                      Globals.prefs.getBoolean("caseSensitiveSearch"));

    settings.add(select);

    // 2005.03.29, trying to remove field category searches, to simplify
        // search usability.
    //settings.addSeparator();
    //settings.add(searchReq);
    //settings.add(searchOpt);
    //settings.add(searchGen);
    //settings.addSeparator();
    //settings.add(searchAll);
    // ---------------------------------------------------------------
    settings.addSeparator();
        settings.add(caseSensitive);
    settings.add(regExpSearch);
    //settings.addSeparator();


    searchField.addActionListener(this);
    searchField.addCaretListener(this);
        search.addActionListener(this);
    searchField.addFocusListener(new FocusAdapter() {
          public void focusGained(FocusEvent e) {
            if (increment.isSelected())
              searchField.setText("");
          }
        public void focusLost(FocusEvent e) {
            incSearch = false;
            incSearchPos = -1; // Reset incremental
                       // search. This makes the
                       // incremental search reset
                       // once the user moves focus to
                       // somewhere else.
                    if (increment.isSelected()) {
                      //searchField.setText("");
                      //System.out.println("focuslistener");
                    }
        }
        });
    escape.addActionListener(this);
    escape.setEnabled(false); // enabled after searching

    openset.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                  if (settings.isVisible()) {
                    //System.out.println("oee");
                    //settings.setVisible(false);
                  }
                  else {
                    JButton src = (JButton) e.getSource();
                    settings.show(src, 0, openset.getHeight());
                  }
        }
        });

            Insets margin = new Insets(0, 2, 0, 2);
            //search.setMargin(margin);
            escape.setMargin(margin);
            openset.setMargin(margin);
            int butSize = help.getIcon().getIconHeight() + 5;
            Dimension butDim = new Dimension(butSize, butSize);
            help.setPreferredSize(butDim);
            help.setMinimumSize(butDim);
            help.setMargin(margin);
            help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.searchHelp, "Help"));

    // Select the last used mode of search:
    if (Globals.prefs.getBoolean("incrementS"))
        increment.setSelected(true);
    else if (Globals.prefs.getBoolean("floatSearch"))
        floatSearch.setSelected(true);
    else if (Globals.prefs.getBoolean("showSearchInDialog"))
        showResultsInDialog.setSelected(true);
    else if (Globals.prefs.getBoolean("searchAllBases"))
        searchAllBases.setSelected(true);
    else
        hideSearch.setSelected(true);

    JPanel main = new JPanel();
    main.setLayout(gbl);
    //SidePaneHeader header = new SidePaneHeader("Search", GUIGlobals.searchIconFile, this);
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.fill = GridBagConstraints.BOTH;
        con.weightx = 1;
    //con.insets = new Insets(0, 0, 2,  0);
    //gbl.setConstraints(header, con);
    //add(header);
        //con.insets = new Insets(0, 0, 0,  0);
        gbl.setConstraints(searchField,con);
        main.add(searchField) ;
        //con.gridwidth = 1;
        gbl.setConstraints(search,con);
        main.add(search) ;
        con.gridwidth = GridBagConstraints.REMAINDER;
        gbl.setConstraints(escape,con);
        main.add(escape) ;
        con.insets = new Insets(0, 2, 0,  0);
        gbl.setConstraints(increment, con);
        main.add(increment);
        gbl.setConstraints(floatSearch, con);
        main.add(floatSearch);
        gbl.setConstraints(hideSearch, con);
        main.add(hideSearch);
        gbl.setConstraints(showResultsInDialog, con);
        main.add(showResultsInDialog);
        gbl.setConstraints(searchAllBases, con);
        main.add(searchAllBases);
    con.insets = new Insets(0, 0, 0,  0);
        JPanel pan = new JPanel();
        GridBagLayout gb = new GridBagLayout();
        gbl.setConstraints(pan, con);
        pan.setLayout(gb);
        con.weightx = 1;
        con.gridwidth = 1;
        gb.setConstraints(openset, con);
        pan.add(openset);
        con.weightx = 0;
        gb.setConstraints(help, con);
        pan.add(help);
        main.add(pan);
        main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
        
        setContent(main);

    searchField.getInputMap().put(Globals.prefs.getKey("Repeat incremental search"),
                      "repeat");

    searchField.getActionMap().put("repeat", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            if (increment.isSelected())
            repeatIncremental();
        }
        });
    searchField.getInputMap().put(Globals.prefs.getKey("Clear search"), "escape");
    searchField.getActionMap().put("escape", new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
            hideAway();
            //SearchManager2.this.actionPerformed(new ActionEvent(escape, 0, ""));
        }
        });
    setSearchButtonSizes();
    updateSearchButtonText();
    }

    /** force the search button to be large enough for
     * the longer of the two texts */
    private void setSearchButtonSizes() {
        search.setText(Globals.lang("Search Specified Field(s)"));
        Dimension size1 = search.getPreferredSize();
        search.setText(Globals.lang("Search All Fields"));
        Dimension size2 = search.getPreferredSize();
        size2.width = Math.max(size1.width,size2.width);
        search.setMinimumSize(size2);
        search.setPreferredSize(size2);
    }

    /**
     * Instantiate the search dialog, unless it has already been instantiated:
     */
    protected void instantiateSearchDialog() {
        if (searchDialog == null)
            searchDialog = new SearchResultsDialog(frame, Globals.lang("Search results"));
    }

    public void updatePrefs() {
        Globals.prefs.putBoolean("searchReq", searchReq.isSelected());
        Globals.prefs.putBoolean("searchOpt", searchOpt.isSelected());
        Globals.prefs.putBoolean("searchGen", searchGen.isSelected());
        Globals.prefs.putBoolean("searchAll", searchAll.isSelected());
        Globals.prefs.putBoolean("incrementS", increment.isSelected());
        Globals.prefs.putBoolean("selectS", select.isSelected());
        Globals.prefs.putBoolean("floatSearch", floatSearch.isSelected());
        Globals.prefs.putBoolean("caseSensitiveSearch",
                 caseSensitive.isSelected());
        Globals.prefs.putBoolean("regExpSearch", regExpSearch.isSelected());
        Globals.prefs.putBoolean("showSearchInDialog", showResultsInDialog.isSelected());
        Globals.prefs.putBoolean("searchAllBases", searchAllBases.isSelected());
    }

    public void startIncrementalSearch() {
    increment.setSelected(true);
    searchField.setText("");
        //System.out.println("startIncrementalSearch");
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
        //searchField.setText("");
            searchField.selectAll();
        searchField.requestFocus();
    } else {
        if (increment.isSelected())
            floatSearch.setSelected(true);
        else if (floatSearch.isSelected())
            hideSearch.setSelected(true);
        else if (hideSearch.isSelected())
            showResultsInDialog.setSelected(true);
        else if (showResultsInDialog.isSelected())
            searchAllBases.setSelected(true);
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
        if (panel != null) {
            Thread t = new Thread() {
                public void run() {
                    clearSearch();
                }
            };
            // do this after the button action is over
            SwingUtilities.invokeLater(t);
        }
    }
    else if (((e.getSource() == searchField) || (e.getSource() == search))
         && !increment.isSelected()
         && (panel != null)) {
        updatePrefs(); // Make sure the user's choices are recorded.
            if (searchField.getText().equals("")) {
              // An empty search field should cause the search to be cleared.
              panel.stopShowingSearchResults();
              return;
            }
        // Setup search parameters common to both normal and float.
        Hashtable<String, String> searchOptions = new Hashtable<String, String>();
        searchOptions.put("option",searchField.getText()) ;
        SearchRuleSet searchRules = new SearchRuleSet() ;
        SearchRule rule1;

        rule1 = new BasicSearch(Globals.prefs.getBoolean("caseSensitiveSearch"),
                Globals.prefs.getBoolean("regExpSearch"));

        try {
            // this searches specified fields if specified,
            // and all fields otherwise
            rule1 = new SearchExpression(Globals.prefs,searchOptions);
        } catch (Exception ex) {
            // we'll do a search in all fields
        }

        searchRules.addRule(rule1) ;
        SearchWorker worker = new SearchWorker(searchRules, searchOptions);
        worker.getWorker().run();
        worker.getCallBack().update();
        escape.setEnabled(true);
    }
    }

    class SearchWorker extends AbstractWorker {
        private SearchRuleSet rules;
        Hashtable<String, String> searchTerm;
        int hits = 0;
        public SearchWorker(SearchRuleSet rules, Hashtable<String, String> searchTerm) {
            this.rules = rules;
            this.searchTerm = searchTerm;
        }

        public void run() {
            if (!searchAllBases.isSelected()) {
                // Search only the current database:
                for (BibtexEntry entry : panel.getDatabase().getEntries()){
                    boolean hit = rules.applyRule(searchTerm, entry) > 0;
                    entry.setSearchHit(hit);
                    if (hit) hits++;
                }
            }
            else {
                // Search all databases:
                for (int i=0; i<frame.getTabbedPane().getTabCount(); i++) {
                    BasePanel p = frame.baseAt(i);
                    for (BibtexEntry entry : p.getDatabase().getEntries()){
                        boolean hit = rules.applyRule(searchTerm, entry) > 0;
                        entry.setSearchHit(hit);
                        if (hit) hits++;
                    }
                }
            }
        }

        public void update() {
            panel.output(Globals.lang("Searched database. Number of hits")
                    + ": " + hits);

            // Show the result in the chosen way:
            if (searchAllBases.isSelected()) {
                // Search all databases. This means we need to use the search results dialog.
                // Turn off other search mode, if activated:
                if (startedFloatSearch) {
                    panel.mainTable.stopShowingFloatSearch();
                    startedFloatSearch = false;
                }
                if (startedFilterSearch) {
                    panel.stopShowingSearchResults();
                    startedFilterSearch = false;
                }
                // Make sure the search dialog is instantiated and cleared:
                instantiateSearchDialog();
                searchDialog.clear();
                for (int i=0; i<frame.getTabbedPane().getTabCount(); i++) {
                    BasePanel p = frame.baseAt(i);
                    for (BibtexEntry entry : p.getDatabase().getEntries()){
                        if (entry.isSearchHit())
                            searchDialog.addEntry(entry, p);
                    }
                }
                searchDialog.selectFirstEntry();
                searchDialog.setVisible(true);
            }

            else if (showResultsInDialog.isSelected()) {
                // Turn off other search mode, if activated:
                if (startedFloatSearch) {
                    panel.mainTable.stopShowingFloatSearch();
                    startedFloatSearch = false;
                }
                if (startedFilterSearch) {
                    panel.stopShowingSearchResults();
                    startedFilterSearch = false;
                }
                // Make sure the search dialog is instantiated and cleared:
                instantiateSearchDialog();
                searchDialog.clear();
                for (BibtexEntry entry : panel.getDatabase().getEntries()) {
                    if (entry.isSearchHit())
                        searchDialog.addEntry(entry, panel);
                }
                searchDialog.selectFirstEntry();
                searchDialog.setVisible(true);
            }
            else if (hideSearch.isSelected()) {
                // Filtering search - removes non-hits from the table:
                if (startedFloatSearch) {
                    panel.mainTable.stopShowingFloatSearch();
                    startedFloatSearch = false;
                }
                startedFilterSearch = true;
                panel.setSearchMatcher(SearchMatcher.INSTANCE);

            } else {
                // Float search - floats hits to the top of the table:
                if (startedFilterSearch) {
                    panel.stopShowingSearchResults();
                    startedFilterSearch = false;
                }
                startedFloatSearch = true;
                panel.mainTable.showFloatSearch(SearchMatcher.INSTANCE);

            }

            // Afterwards, select all text in the search field.
            searchField.select(0, searchField.getText().length());

        }
    }

    public void clearSearch() {
        if (panel.isShowingFloatSearch()) {
            startedFloatSearch = false;
            panel.mainTable.stopShowingFloatSearch();
        } else if (panel.isShowingFilterSearch()) {
            startedFilterSearch = false;
            panel.stopShowingSearchResults();
        }
        // disable "Cancel" button to signal this to the user
        escape.setEnabled(false);
    }
    public void itemStateChanged(ItemEvent e) {
    if (e.getSource() == increment) {
        if (startedFilterSearch || startedFloatSearch) {
            clearSearch();
        }
        updateSearchButtonText();
        if (increment.isSelected())
        searchField.addKeyListener(this);
        else
        searchField.removeKeyListener(this);
    } else /*if (e.getSource() == normal)*/ {
        updateSearchButtonText();

        // If this search type is disabled, remove reordering from
        // all databases.
        /*if ((panel != null) && increment.isSelected()) {
            clearSearch();
        } */
    }
    }

    private void repeatIncremental() {
    incSearchPos++;
    if (panel != null)
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
    if (panel != null)
        goIncremental();
    }

    private void goIncremental() {
    incSearch = true;
    escape.setEnabled(true);
    SwingUtilities.invokeLater(new Thread() {
        public void run() {
            String text = searchField.getText();


            if (incSearchPos >= panel.getDatabase().getEntryCount()) {
            panel.output("'"+text+"' : "+Globals.lang

                     ("Incremental search failed. Repeat to search from top.")+".");
            incSearchPos = -1;
            return;
            }

            if (searchField.getText().equals("")) return;
            if (incSearchPos < 0)
            incSearchPos = 0;
            BibtexEntry be = panel.mainTable.getEntryAt(incSearchPos);
            while (!incSearcher.search(text, be)) {
                incSearchPos++;
                if (incSearchPos < panel.getDatabase().getEntryCount())
                    be = panel.mainTable.getEntryAt(incSearchPos);
            else {
                panel.output("'"+text+"' : "+Globals.lang
                     ("Incremental search failed. Repeat to search from top."));
                incSearchPos = -1;
                return;
            }
            }
            if (incSearchPos >= 0) {

            panel.selectSingleEntry(incSearchPos);
            panel.output("'"+text+"' "+Globals.lang

                     ("found")+".");

            }
        }
        });
    }

    public void componentClosing() {
    frame.searchToggle.setSelected(false);
        if (panel != null) {
            if (startedFilterSearch || startedFloatSearch)
                clearSearch();
        }
    }


    public void keyPressed(KeyEvent e) {}
    public void keyReleased(KeyEvent e) {}

    public void caretUpdate(CaretEvent e) {
        if (e.getSource() == searchField) {
            updateSearchButtonText();
        }
    }

    /** Updates the text on the search button to reflect
      * the type of search that will happen on click. */
    private void updateSearchButtonText() {
        search.setText(!increment.isSelected()
                && SearchExpressionParser.checkSyntax(
                searchField.getText(),
                caseSensitive.isSelected(),
                regExpSearch.isSelected()) != null
                ? Globals.lang("Search Specified Field(s)")
                : Globals.lang("Search All Fields"));
    }

    /**
     * This method is required by the ErrorMessageDisplay interface, and lets this class
     * serve as a callback for regular expression exceptions happening in DatabaseSearch.
     * @param errorMessage
     */
    public void reportError(String errorMessage) {
        JOptionPane.showMessageDialog(panel, errorMessage, Globals.lang("Search error"),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * This method is required by the ErrorMessageDisplay interface, and lets this class
     * serve as a callback for regular expression exceptions happening in DatabaseSearch.
     * @param errorMessage
     */
    public void reportError(String errorMessage, Exception exception) {
        reportError(errorMessage);
    }


    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
        if (panel != null)
            escape.setEnabled(panel.isShowingFloatSearch()
                    || panel.isShowingFilterSearch());
        else
            escape.setEnabled(false);
    }
}
