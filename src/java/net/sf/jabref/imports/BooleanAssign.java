/*
 * Package package net.sf.jabref.imports;
 * Created on Jul 12, 2004
 * Author mspiegel
 *
 */
package net.sf.jabref.imports;

/**
 * @author mspiegel
 *
 */
public class BooleanAssign {
    
    boolean value;
    
    /**
	 * @param b
	 */
	public BooleanAssign(boolean b) {
		setValue(b);		
	}

	public void setValue(boolean value) {
        this.value = value;
    }
    
    public boolean getValue() {
        return(value);
    }
}
