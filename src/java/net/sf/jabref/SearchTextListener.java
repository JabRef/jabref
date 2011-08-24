package net.sf.jabref;

import java.util.ArrayList;

/**
 * Every Listener that wants to receive events from a search needs to
 * implement this interface
 * 
 * @author Ben
 * 
 */
interface SearchTextListener {
	/**
	 * Array of words that were searched for
	 * 
	 * @param words
	 */
	public void searchText(ArrayList<String> words);
}