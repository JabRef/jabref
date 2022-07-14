package org.jabref.logic.search;

import java.util.*;

public class History {
	private ArrayList<String> searchHistory = new ArrayList<>();

    /**
     * Add names into the history
     *
     * @param name of the history
     */
    public void add(String name) {
    	searchHistory.add(name);
    }

    /**
     * Get the list of search history.
     *
     * @Return a list of all search histories
     */
    public List<String> getAllHistories() {
        return new ArrayList<>(searchHistory);
    }

    /**
     * Get the x'th search history
     *
     * @param integer x
     * @Return the name of the x'th search history
     */
    public String getHitsoryWithNO(int x) {
        return searchHistory.get(x);
    }
    
    /**
     * Get the history with a specific prefix
     *
     * @param a string of the prefix
     * @Return the list of all search histories that has the given prefix
     */
    public List<String> getHitsoryWithPrefix(String pre) {
    	ArrayList<String> ret = new ArrayList<>();
    	Iterator<String> iterator = searchHistory.iterator();
    	while (iterator.hasNext()){
    	    String str = iterator.next();
    	    if(str.startsWith(pre)){
    	        ret.add(str);
    	    }
    	}
    	return ret;
    }
    
    /**
     * Delete all the search history.
     *
     */
    public void deleteAll() {
        searchHistory.clear();
    }

    /**
     * Delete the x'th search history
     *
     * @param integer x
     */
    public void deleteHitsoryWithNO(int x) {
        int n=0;
        Iterator<String> iterator = searchHistory.iterator();
    	while (iterator.hasNext()){
    	    if(n == x){
    	        iterator.remove();
    	    }
    	    n++;
    	}
    }
    
    /**
     * Delete the history with a specific prefix
     *
     * @param a string of the prefix
     */
    public void deleteHitsoryWithPrefix(String pre) {
    	Iterator<String> iterator = searchHistory.iterator();
    	while (iterator.hasNext()){
    	    String str = iterator.next();
    	    if(str.startsWith(pre)){
    	        iterator.remove();
    	    }
    	}
    }
}