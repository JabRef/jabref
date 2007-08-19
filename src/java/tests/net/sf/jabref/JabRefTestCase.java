package tests.net.sf.jabref;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;

public class JabRefTestCase extends TestCase {
	
	public void assertEquals(BibtexEntry e, BibtexEntry x){
		assertEquals(e.getCiteKey(), x.getCiteKey());
		assertEquals(e.getType(), x.getType());

		assertEquals(e.getAllFields().size(), x.getAllFields().size());

		for (String name : e.getAllFields()){
			assertEquals(e.getField(name), x.getField(name));
		}
	}
	
	public void testVoid(){
		// to remove warning
	}
	
}
