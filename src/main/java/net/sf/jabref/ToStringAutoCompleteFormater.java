package net.sf.jabref;

public class ToStringAutoCompleteFormater<E> implements AutoCompleteFormater<E> {

	@Override
	public String formatItemToString(E item) {
		return item.toString();
	}
	
}