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
package net.sf.jabref;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.sf.jabref.autocompleter.AbstractAutoCompleter;
import net.sf.jabref.autocompleter.FuzzyTextSearchStrategy;
import net.sf.jabref.gui.AutoCompleteListener;
import net.sf.jabref.gui.SearchResultsDialog;
import net.sf.jabref.help.HelpAction;
import net.sf.jabref.search.SearchRule;
import net.sf.jabref.search.SearchRules;
import net.sf.jabref.search.matchers.SearchMatcher;
import net.sf.jabref.search.rules.GrammarBasedSearchRule;
import net.sf.jabref.search.rules.sets.SearchRuleSet;

import org.gpl.JSplitButton.JSplitButton;
import org.gpl.JSplitButton.action.SplitButtonActionListener;

import ca.odell.glazedlists.impl.filter.TextSearchStrategy;

public class SearchBar extends JPanel implements ActionListener, KeyListener, ItemListener, CaretListener {

	private JabRefFrame frame;

	private JSearchTextField searchField;
	private JSplitButton searchButton;
	private JPopupMenu popupMenu;

	private JMenuItem escape;
	private JRadioButtonMenuItem increment, floatSearch, hideSearch, liveFilterSearch, showResultsInDialog, searchAllBases;

	private JMenu settings;
	private JCheckBoxMenuItem select, caseSensitive, regExpSearch, highLightWords, searchAutoComplete;

	AutoCompleteSupport<String> autoCompleteSupport;

	IncrementalSearcher incSearcher;
	SearchResultsDialog searchDialog = null;

	AutoCompleteListener autoCompleteListener = null;

	/**
	 * subscribed Objects
	 */
	private Vector<SearchTextListener> listeners = new Vector<SearchTextListener>();

	// private JButton escape = new JButton(Globals.lang("Clear"));
	/** This button's text will be set later. */
	// private JCheckBoxMenuItem searchReq, searchOpt, searchGen,searchAll;

	private boolean incSearch = false, startedFloatSearch = false, startedFilterSearch = false;

	private int incSearchPos = -1; // To keep track of where we are in

	// an incremental search. -1 means
	// that the search is inactive.

	public SearchBar(JabRefFrame frame) {
		super();

		this.frame = frame;

		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 0;

		initSearchField();
		c.gridx = 0;
		c.gridy = 0;
		this.add(searchField, c);
		initSearchButton();
		c.weightx = 0;
		c.anchor = GridBagConstraints.EAST;
		c.gridx = GridBagConstraints.RELATIVE;
		this.add(searchButton, c);

		autoCompleteSupport = new AutoCompleteSupport<String>(searchField);
		autoCompleteSupport.install();

		// setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
		// setLayout(new FlowLayout(FlowLayout.CENTER, 0, 1)); //his,
		// BoxLayout.X_AXIS));
		// setLayout(new BoxLayout(this, BoxLayout.X_AXIS));

		// search.setPreferredSize(new Dimension(60,
		// searchField.getPreferredSize().height));
		// search.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

		incSearcher = new IncrementalSearcher(Globals.prefs);

		// setBorder(BorderFactory.createMatteBorder(1,1,1,1,Color.magenta));

		// Add an item listener that makes sure we only listen for key events
		// when incremental search is turned on.
		

		// Add the global focus listener, so a menu item can see if this field
		// was focused when
		// an action was called.
		searchField.addFocusListener(Globals.focusListener);

		// searchField.setIcon(GUIGlobals.getImage("search"));
		searchField.setTextWhenNotFocused(Globals.lang("Search..."));
		searchField.addActionListener(this);
		// searchField.addCaretListener(this);

		searchField.addFocusListener(new FocusAdapter() {
			public void focusGained(FocusEvent e) {
				// if (increment.isSelected())
				// searchField.setText("");
			}

			public void focusLost(FocusEvent e) {
				incSearch = false;
				incSearchPos = -1; // Reset incremental
				// search. This makes the
				// incremental search reset
				// once the user moves focus to
				// somewhere else.
				if (increment.isSelected()) {
					// searchField.setText("");
					// System.out.println("focuslistener");
				}
			}
		});
		// escape.addActionListener(this);
		// escape.setEnabled(false); // enabled after searching

		searchAutoComplete.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				Globals.prefs.putBoolean("searchAutoComplete", searchAutoComplete.isSelected());
				if (SearchBar.this.frame.basePanel() != null) {
					SearchBar.this.frame.basePanel().updateSearchManager();
				}

			}
		});

		

		// this.setLayout(gbl);
		// con.gridwidth = GridBagConstraints.REMAINDER;
		// con.fill = GridBagConstraints.NONE;
		// con.weightx = 1;

		// searchField.setSize(new Dimension(60, 10));

		// gbl.setConstraints(searchField,con);

		// searchField.setSize(new Dimension(60, 10));
		// con.gridwidth = 1;

		// gbl.setConstraints(escape,con);
		// this.add(escape) ;

		searchField.getInputMap().put(Globals.prefs.getKey("Repeat incremental search"), "repeat");

		searchField.getActionMap().put("repeat", new AbstractAction() {
			public void actionPerformed(ActionEvent e) {
				if (increment.isSelected())
					repeatIncremental();
			}
		});
		searchField.getInputMap().put(Globals.prefs.getKey("Clear search"), "escape");
		/*
		 * searchField.getActionMap().put("escape", new AbstractAction() {
		 * public void actionPerformed(ActionEvent e) { hideAway();
		 * //SearchManager2.this.actionPerformed(new ActionEvent(escape, 0,
		 * "")); } });
		 */
		// setSearchButtonSizes();
		updateSearchButtonText();
	}

	/**
	 * DONE
	 * Initializes the search button and its popup menu 
	 */
	private void initSearchButton() {
		// Create search button
		searchButton = new JSplitButton(GUIGlobals.getImage("search"));
		searchButton.setMinimumSize(new Dimension(50, 25));
		searchButton.setBackground(searchField.getBackground());
		searchButton.setContentAreaFilled(false);
		searchButton.setOpaque(true);
		searchButton.addSplitButtonActionListener(new SplitButtonActionListener() {
			public void buttonClicked(ActionEvent e) {
				actionPerformed(e);
			}

			public void splitButtonClicked(ActionEvent e) {
			}
		});

		// Populate popup menu and add it to search button
		popupMenu = new JPopupMenu("");

		escape = new JMenuItem(Globals.lang("Clear results"));
		escape.addActionListener(this);
		popupMenu.add(escape);
		popupMenu.addSeparator();

		initSearchModeMenu();		
		for (SearchMode mode : SearchMode.values()) {
			popupMenu.add(getSearchModeMenuItem(mode));
		}
		popupMenu.addSeparator();

		initSearchSettingsMenu();
		popupMenu.add(settings);
		
		JMenuItem help = new JMenuItem(Globals.lang("Help"), GUIGlobals.getImage("help"));
		help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.searchHelp, Globals.lang("Help")));
		popupMenu.add(help);

		searchButton.setPopupMenu(popupMenu);
	}

	/**
	 * DONE
	 * Initializes the popup menu items controlling search settings
	 */
	private void initSearchSettingsMenu() {
		// Create menu items
		settings = new JMenu(Globals.lang("Settings"));
		select = new JCheckBoxMenuItem(Globals.lang("Select matches"), false);
		caseSensitive = new JCheckBoxMenuItem(Globals.lang("Case sensitive"), Globals.prefs.getBoolean("caseSensitiveSearch"));
		regExpSearch = new JCheckBoxMenuItem(Globals.lang("Use regular expressions"), Globals.prefs.getBoolean("regExpSearch"));
		highLightWords = new JCheckBoxMenuItem(Globals.lang("Highlight Words"), Globals.prefs.getBoolean("highLightWords"));
		searchAutoComplete = new JCheckBoxMenuItem(Globals.lang("Autocomplete names"), Globals.prefs.getBoolean("searchAutoComplete"));

		// Add them to the menu
		settings.add(select);
		settings.addSeparator();
		settings.add(caseSensitive);
		settings.add(regExpSearch);
		settings.addSeparator();
		settings.add(highLightWords);
		settings.addSeparator();
		settings.add(searchAutoComplete);
	}

	/**
	 * DONE
	 * Initializes the popup menu items controlling the search mode
	 */
	private void initSearchModeMenu() {
		ButtonGroup searchMethod = new ButtonGroup();
		for (SearchMode mode : SearchMode.values()) {
			// Create menu items
			switch (mode) {
				case Filter:
					hideSearch = new JRadioButtonMenuItem(mode.getDisplayName(), true);
					break;
				case Float:
					floatSearch = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean("floatSearch"));
					break;
				case Global:
					searchAllBases = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean("searchAllBases"));
					break;
				case Incremental:
					increment = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean("incrementS"));
					break;
				case LiveFilter:
					liveFilterSearch = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean("searchModeLiveFilter"));
					break;
				case ResultsInDialog:
					showResultsInDialog = new JRadioButtonMenuItem(mode.getDisplayName(), Globals.prefs.getBoolean("showSearchInDialog"));
					break;
			}
			
			// Set tooltips on menu items
			getSearchModeMenuItem(mode).setToolTipText(mode.getToolTipText());

			// Add menu item to group
			searchMethod.add(getSearchModeMenuItem(mode));
			
			// Listen to selection changed events
			getSearchModeMenuItem(mode).addItemListener(this);
		}
	}

	/**
	 * DONE
	 * Initializes the search text field
	 */
	private void initSearchField() {
		// Create search text field
		searchField = new JSearchTextField();
		searchField.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
	}

	/**
	 * DONE
	 * Enum collecting all the different possible search methods
	 */
	private enum SearchMode {
		Incremental(Globals.lang("Incremental"), Globals.lang("Incremental search")),
		Float(Globals.lang("Float"), Globals.lang("Gray out non-matching entries")),
		Filter(Globals.lang("Filter"), Globals.lang("Hide non-matching entries")),
		LiveFilter(Globals.lang("Live filter"), Globals.lang("Automatically hide non-matching entries")),
		ResultsInDialog(Globals.lang("Show results in dialog"), Globals.lang("Show search results in a window")),
		Global(Globals.lang("Global search"), Globals.lang("Search in all open databases"));

		private String displayName;
		private String toolTipText;

		SearchMode(String displayName, String toolTipText) {
			this.displayName = displayName;
			this.toolTipText = toolTipText;
		}

		public String getDisplayName() {
			return displayName;
		}

		public String getToolTipText() {
			return toolTipText;
		}
	}

	/**
	 * DONE
	 * Returns the item in the popup menu of the search button corresponding to the given search mode
	 */
	private JRadioButtonMenuItem getSearchModeMenuItem(SearchMode mode) {
		switch (mode) {
			case Incremental:
				return increment;
			case Filter:
				return hideSearch;
			case Float:
				return floatSearch;
			case Global:
				return searchAllBases;
			case LiveFilter:
				return liveFilterSearch;
			case ResultsInDialog:
				return showResultsInDialog;
		}
		return null;
	}

	/*
	 * public void setAutoCompleteListener(AutoCompleteListener listener) {
	 * this.autoCompleteListener = listener; updateKeyListeners(); }
	 */

	/**
	 * Add the correct key listeners to the search text field, depending on
	 * whether and autocomplete listener has been set and whether incremental
	 * search is selected.
	 */
	protected void updateKeyListeners() {
		/*
		 * KeyListener[] listeners = searchField.getKeyListeners(); for
		 * (KeyListener listener : listeners) {
		 * searchField.removeKeyListener(listener); } if
		 * (increment.isSelected()) { searchField.addKeyListener(this); }
		 */
		/*
		 * else { if (searchAutoComplete.isSelected() && autoCompleteListener !=
		 * null) searchField.addKeyListener(autoCompleteListener); }
		 */
	}

	/**
	 * Subscribe to the SearchListener and receive events, if the user searches
	 * for some thing. You will receive a list of words
	 * 
	 * @param l
	 */
	public void addSearchListener(SearchTextListener l) {
		if (listeners.contains(l))
			return;
		else
			listeners.add(l);
		// fire event for the new subscriber
		l.searchText(getSearchwords(searchField.getText()));
	}

	/**
	 * Remove object from the SearchListener
	 * 
	 * @param l
	 */
	public void removeSearchListener(SearchTextListener l) {
		listeners.remove(l);
	}

	/**
	 * parse the search string for valid words and return a list of words Like
	 * "The great Vikinger" will be ["The","great","Vikinger"]
	 * 
	 * @param t
	 * @return
	 */
	private ArrayList<String> getSearchwords(String t) {
		// for now ... just seperate words by whitespace and comma
		// First remove trailing white spaces and/or commas, then split
		String[] strings = t.replaceAll("^[,\\s]+", "").split("[,\\s]+");
		ArrayList<String> words = new ArrayList<String>(strings.length);
		Collections.addAll(words, strings);
		return words;
	}

	/**
	 * Fires an event if a search was started / canceled
	 * 
	 * @param t
	 */
	private void fireSearchlistenerEvent(String t) {
		// parse the Search string to words
		ArrayList<String> words;
		if ((t == null) || (t.isEmpty())) {
			words = null;
		} else {
			words = getSearchwords(t);
		}

		// fire an event for every listener
		for (SearchTextListener s : listeners)
			s.searchText(words);
	}

	/**
	 * Instantiate the search dialog, unless it has already been instantiated:
	 */
	protected void instantiateSearchDialog() {
		if (searchDialog == null)
			searchDialog = new SearchResultsDialog(frame, Globals.lang("Search results"));
	}

	public void updatePrefs() {
		Globals.prefs.putBoolean("incrementS", increment.isSelected());
		Globals.prefs.putBoolean("searchModeLiveFilter", liveFilterSearch.isSelected());
		Globals.prefs.putBoolean("selectS", select.isSelected());
		Globals.prefs.putBoolean("floatSearch", floatSearch.isSelected());
		Globals.prefs.putBoolean("caseSensitiveSearch", caseSensitive.isSelected());
		Globals.prefs.putBoolean("regExpSearch", regExpSearch.isSelected());
		Globals.prefs.putBoolean("highLightWords", highLightWords.isSelected());
		Globals.prefs.putBoolean("showSearchInDialog", showResultsInDialog.isSelected());
		Globals.prefs.putBoolean("searchAllBases", searchAllBases.isSelected());
	}

	public void startIncrementalSearch() {
		increment.setSelected(true);
		searchField.setText("");
		// System.out.println("startIncrementalSearch");
		searchField.requestFocus();
	}

	/**
	 * Clears and focuses the search field if it is not focused. Otherwise,
	 * cycles to the next search type.
	 */
	public void startSearch() {
		if (increment.isSelected() && incSearch) {
			repeatIncremental();
			return;
		}
		if (!searchField.hasFocus()) {
			// searchField.setText("");
			// searchField.selectAll(); TODO I disabled this
			searchField.requestFocus();
		} else {
			if (increment.isSelected())
				floatSearch.setSelected(true);
			else if (floatSearch.isSelected())
				hideSearch.setSelected(true);
			else if (hideSearch.isSelected())
				liveFilterSearch.setSelected(true);
			else if (liveFilterSearch.isSelected())
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

	private void clearSearchLater() {
		if (frame.basePanel() != null) {
			Thread t = new Thread() {
				public void run() {
					clearSearch();
				}
			};
			// do this after the button action is over
			SwingUtilities.invokeLater(t);
		}
	}

	public void actionPerformed(ActionEvent e) {

		if (e.getSource() == escape) {
			incSearch = false;
			clearSearchLater();
		} else if (((e.getSource() == searchField) || (e.getSource() == searchButton)) && !increment.isSelected() && (frame.basePanel() != null)) {

			updatePrefs(); // Make sure the user's choices are recorded.
			if (searchField.getText().equals("")) {
				// An empty search field should cause the search to be cleared.
				clearSearchLater();
				return;
			}

			/*
			 * if(support == null || ! support.isInstalled()) { // TODO: Add
			 * proper autocomplete String[] autoCompletions = {"Test1", "Test2",
			 * "Test3", "T", "Te", "A"}; support =
			 * AutoCompleteSupport.install(searchField,
			 * GlazedLists.eventListOf(autoCompletions));
			 * support.setFilterMode(TextMatcherEditor.CONTAINS);
			 * support.setTextMatchingStrategy(FUZZY_STRATEGY); }
			 */

			// AutoCompleteSupport support = AutoCompleteSupport.install(
			// searchField, GlazedLists.eventListOf(autoCompletions));

			fireSearchlistenerEvent(searchField.getText().toString());
			// TODO I disabled this

			// Setup search parameters common to both normal and float.
			SearchRule searchRule = SearchRules.getSearchRuleByQuery(searchField.getText(),
                    Globals.prefs.getBoolean(JabRefPreferences.CASE_SENSITIVE_SEARCH),
                    Globals.prefs.getBoolean(JabRefPreferences.REG_EXP_SEARCH));

            if (!searchRule.validateSearchStrings(searchField.getText())) {
            	frame.basePanel().output(Globals.lang("Search failed: illegal search expression"));
            	frame.basePanel().stopShowingSearchResults();
                return;
            }
			SearchWorker worker = new SearchWorker(searchRule, searchField.getText().toString());
			worker.getWorker().run();
			worker.getCallBack().update();
			// escape.setEnabled(true);

			if (worker.getHits() > 0)
				frame.basePanel().mainTable.setSelected(0);
		}
	}

	class SearchWorker extends AbstractWorker {
		
		private final SearchRule rule;
        private final String searchTerm;
		int hits = 0;

		public SearchWorker(SearchRule rule, String searchTerm) {
            this.rule = rule;
            this.searchTerm = searchTerm;
        }
		
		public int getHits() {
			return hits;
		}

		@Override
		public void run() {
			if (!searchAllBases.isSelected()) {
				// Search only the current database:
				for (BibtexEntry entry : frame.basePanel().getDatabase().getEntries()) {

					boolean hit = rule.applyRule(searchTerm, entry);
					entry.setSearchHit(hit);
					if (hit) {
						hits++;
					}
				}
			} else {
				// Search all databases:
				for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
					BasePanel p = frame.baseAt(i);
					for (BibtexEntry entry : p.getDatabase().getEntries()) {

						boolean hit = rule.applyRule(searchTerm, entry);
						entry.setSearchHit(hit);
						if (hit)
							hits++;
					}
				}
			}
		}

		public void update() {
			frame.basePanel().output(Globals.lang("Searched database. Number of hits") + ": " + hits);

			// Show the result in the chosen way:
			if (searchAllBases.isSelected()) {
				// Search all databases. This means we need to use the search
				// results dialog.
				// Turn off other search mode, if activated:
				if (startedFloatSearch) {
					frame.basePanel().mainTable.stopShowingFloatSearch();
					startedFloatSearch = false;
				}
				if (startedFilterSearch) {
					frame.basePanel().stopShowingSearchResults();
					startedFilterSearch = false;
				}
				// Make sure the search dialog is instantiated and cleared:
				instantiateSearchDialog();
				searchDialog.clear();
				for (int i = 0; i < frame.getTabbedPane().getTabCount(); i++) {
					BasePanel p = frame.baseAt(i);
					for (BibtexEntry entry : p.getDatabase().getEntries()) {
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
					frame.basePanel().mainTable.stopShowingFloatSearch();
					startedFloatSearch = false;
				}
				if (startedFilterSearch) {
					frame.basePanel().stopShowingSearchResults();
					startedFilterSearch = false;
				}
				// Make sure the search dialog is instantiated and cleared:
				instantiateSearchDialog();
				searchDialog.clear();
				for (BibtexEntry entry : frame.basePanel().getDatabase().getEntries()) {
					if (entry.isSearchHit())
						searchDialog.addEntry(entry, frame.basePanel());
				}
				searchDialog.selectFirstEntry();
				searchDialog.setVisible(true);
			} else if (hideSearch.isSelected() || liveFilterSearch.isSelected()) {
				// Filtering search - removes non-hits from the table:
				if (startedFloatSearch) {
					frame.basePanel().mainTable.stopShowingFloatSearch();
					startedFloatSearch = false;
				}
				startedFilterSearch = true;
				frame.basePanel().setSearchMatcher(new SearchMatcher());

			} else {
				// Float search - floats hits to the top of the table:
				if (startedFilterSearch) {
					frame.basePanel().stopShowingSearchResults();
					startedFilterSearch = false;
				}
				startedFloatSearch = true;
				frame.basePanel().mainTable.showFloatSearch(new SearchMatcher());

			}

			// Afterwards, select all text in the search field.
			// searchField.select(0,
			// searchField.getSelectedItem().toString().length());
			// TODO I disabled this
		}
	}

	public void clearSearch() {

		if (frame.basePanel().isShowingFloatSearch()) {
			startedFloatSearch = false;
			frame.basePanel().mainTable.stopShowingFloatSearch();
		} else if (frame.basePanel().isShowingFilterSearch()) {
			startedFilterSearch = false;
			frame.basePanel().stopShowingSearchResults();
		}

		// clear search means that nothing is searched for
		// even if a word is written in the text field,
		// nothing should be highlighted
		fireSearchlistenerEvent(null);

		// disable "Cancel" button to signal this to the user
		// escape.setEnabled(false);
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() == increment) {
			if (startedFilterSearch || startedFloatSearch) {
				clearSearch();
			}
			updateSearchButtonText();

			// Make sure the correct key listener is activated:
			updateKeyListeners();

		} else /* if (e.getSource() == normal) */{
			updateSearchButtonText();

			// If this search type is disabled, remove reordering from
			// all databases.
			/*
			 * if ((panel != null) && increment.isSelected()) { clearSearch(); }
			 */
		}
	}

	private void repeatIncremental() {
		incSearchPos++;
		if (frame.basePanel() != null)
			goIncremental();
	}

	/**
	 * Used for incremental search. Only activated when incremental is selected.
	 *
	 * The variable incSearchPos keeps track of which entry was last checked.
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
		// escape.setEnabled(true);
		SwingUtilities.invokeLater(new Thread() {
			public void run() {
				String text = searchField.getText();

				if (incSearchPos >= frame.basePanel().getDatabase().getEntryCount()) {
					frame.basePanel().output("'" + text + "' : " + Globals.lang("Incremental search failed. Repeat to search from top.") + ".");
					incSearchPos = -1;
					return;
				}

				if (searchField.getText().equals(""))
					return;
				if (incSearchPos < 0)
					incSearchPos = 0;
				BibtexEntry be = frame.basePanel().mainTable.getEntryAt(incSearchPos);
				while (!incSearcher.search(text, be)) {
					incSearchPos++;
					if (incSearchPos < frame.basePanel().getDatabase().getEntryCount())
						be = frame.basePanel().mainTable.getEntryAt(incSearchPos);
					else {
						frame.basePanel().output("'" + text + "' : " + Globals.lang("Incremental search failed. Repeat to search from top."));
						incSearchPos = -1;
						return;
					}
				}
				if (incSearchPos >= 0) {

					frame.basePanel().selectSingleEntry(incSearchPos);
					frame.basePanel().output("'" + text + "' " + Globals.lang("found") + ".");

				}
			}
		});
	}

	/*
	 * public void componentClosing() { frame.searchToggle.setSelected(false);
	 * if (frame.basePanel() != null) { if (startedFilterSearch ||
	 * startedFloatSearch) clearSearch(); } }
	 */

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

	public void caretUpdate(CaretEvent e) {
		if (e.getSource() == searchField) {
			updateSearchButtonText();
		}
	}

	/**
	 * Updates the text on the search button to reflect the type of search that
	 * will happen on click.
	 */
	private void updateSearchButtonText() {
		// TODO: add search mode description
		
		searchButton.setToolTipText(isSpecificSearch() ? Globals
				.lang("Search specified field(s)") : Globals.lang("Search all fields"));
	}

    private boolean isSpecificSearch() {
        return !increment.isSelected() && GrammarBasedSearchRule.isValid(caseSensitive.isSelected(), regExpSearch.isSelected(), searchField.getText());
    }
	
	/**
	 * This method is required by the ErrorMessageDisplay interface, and lets
	 * this class serve as a callback for regular expression exceptions
	 * happening in DatabaseSearch.
	 * 
	 * @param errorMessage
	 */
	public void reportError(String errorMessage) {
		JOptionPane.showMessageDialog(frame.basePanel(), errorMessage, Globals.lang("Search error"), JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * This method is required by the ErrorMessageDisplay interface, and lets
	 * this class serve as a callback for regular expression exceptions
	 * happening in DatabaseSearch.
	 * 
	 * @param errorMessage
	 */
	public void reportError(String errorMessage, Exception exception) {
		reportError(errorMessage);
	}

	/*
	 * public void setActiveBasePanel(BasePanel panel) {
	 * super.setActiveBasePanel(panel); if (panel != null)
	 * escape.setEnabled(panel.isShowingFloatSearch() ||
	 * panel.isShowingFilterSearch()); else escape.setEnabled(false); }
	 */

	// Implementation based on
	// https://gmigdos.wordpress.com/2010/03/30/java-a-custom-jtextfield-for-searching/
	public class JSearchTextField extends JTextField implements FocusListener {

		// JPopupMenu popup = new JPopupMenu();
		// JList<String> list = new JList<String>();
		private String textWhenNotFocused;

		public JSearchTextField() {
			super();
			this.setEditable(true);
			this.setText("");
			this.textWhenNotFocused = "Search...";
			this.addFocusListener(this);

			/*
			 * this.getDocument().addDocumentListener(new DocumentListener() {
			 * 
			 * @Override public void insertUpdate(DocumentEvent e) {
			 * updateAfterDocumentChange(); }
			 * 
			 * @Override public void removeUpdate(DocumentEvent e) {
			 * updateAfterDocumentChange(); }
			 * 
			 * @Override public void changedUpdate(DocumentEvent e) {
			 * //updateAfterDocumentChange(); } });
			 */

			// popup.setFocusable(false);
			// popup.add(list);
		}

		public void setAutoCompleter(AbstractAutoCompleter searchCompleter) {
			autoCompleteSupport.setAutoCompleter(searchCompleter);
		}

		// public JSearchTextField() {
		// this("", 12);
		// }
		/*
		 * protected void updateAfterDocumentChange() { String[] autoCompletions
		 * = searchCompleter.complete(this.getText()); if(autoCompletions !=
		 * null) { list.setListData(autoCompletions); //popup.setLayout(new
		 * BorderLayout()); /*for(String str : autoCompletions) {
		 * listData);.add(str); }
		 */
		// popup.add(new JPanel()); // your component
		// popup.setPopupSize(100, 100);
		/*
		 * if (!popup.isVisible()) popup.show(this, 0, this.getHeight());
		 * 
		 * //popup.repaint(); }
		 * 
		 * }
		 */

		public String getTextWhenNotFocused() {
			return this.textWhenNotFocused;
		}

		public void setTextWhenNotFocused(String newText) {
			this.textWhenNotFocused = newText;
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);

			if (!this.hasFocus() && (this.getText().equals(""))) {
				int width = this.getWidth();
				int height = this.getHeight();
				Font prev = g.getFont();
				Font italic = prev.deriveFont(Font.ITALIC);
				Color prevColor = g.getColor();
				// g.setFont(italic);
				g.setColor(UIManager.getColor("textInactiveText"));
				int h = g.getFontMetrics().getHeight();
				int textBottom = (height - h) / 2 + h - 4;
				int x = this.getInsets().left;
				Graphics2D g2d = (Graphics2D) g;
				RenderingHints hints = g2d.getRenderingHints();
				g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2d.drawString(textWhenNotFocused, x, textBottom);
				g2d.setRenderingHints(hints);
				g.setFont(prev);
				g.setColor(prevColor);
			}
		}

		public void focusGained(FocusEvent e) {
			this.repaint();
		}

		public void focusLost(FocusEvent e) {
			this.repaint();
		}
		/*
		 * public String getText() { System.out.println("get text"); if
		 * (this.getSelectedItem() != null) { return
		 * this.getSelectedItem().toString(); } else return "";
		 * 
		 * 
		 * } public void setText(String text) { this.setSelectedItem(text); }
		 */

	}

	public void setAutoCompleter(AbstractAutoCompleter searchCompleter) {
		this.searchField.setAutoCompleter(searchCompleter);
		updateKeyListeners(); // Should I really call this here?
	}

	public static final Object FUZZY_STRATEGY = new FuzzyStrategyFactory();

	private static class FuzzyStrategyFactory implements TextSearchStrategy.Factory {
		public TextSearchStrategy create(int mode, String filter) {
			// if(mode != TextMatcherEditor.CONTAINS)
			// throw new IllegalArgumentException("unrecognized mode: " + mode);

			return new FuzzyTextSearchStrategy();
		}
	}
}
