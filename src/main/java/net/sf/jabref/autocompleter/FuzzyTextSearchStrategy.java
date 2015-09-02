package net.sf.jabref.autocompleter;

import ca.odell.glazedlists.impl.filter.TextSearchStrategy;

public class FuzzyTextSearchStrategy implements TextSearchStrategy {
	private String subtextLower;
	private String subtextLastWord;
	
	@Override
	// Searches for the previously registered 'subtext' in 'text'
	public int indexOf(String text) {
		return text.toLowerCase().indexOf(subtextLastWord);
	}

	@Override
	public void setCharacterMap(char[] arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSubtext(String subtext) {
		subtextLower = subtext.toLowerCase();
		
		int lastComma = subtextLower.lastIndexOf(',');
		if(lastComma != -1)
			subtextLastWord = subtextLower.substring(lastComma + 1);
		else
			subtextLastWord = subtextLower;
		
		subtextLastWord = subtextLastWord.trim();
	}

}
