package tests.net.sf.jabref;

import junit.framework.TestCase;
import net.sf.jabref.BibtexEntry;

public class JabRefTestCase extends TestCase {
	
	public void assertEquals(BibtexEntry e, BibtexEntry x){
		assertEquals(e.getCiteKey(), x.getCiteKey());
		assertEquals(e.getType(), x.getType());

		Object[] o = e.getAllFields();
		assertEquals(o.length, x.getAllFields().length);

		for (int i = 0; i < o.length; i++) {
			assertEquals(e.getField(o.toString()), x.getField(o.toString()));
		}
	}
	
	public void testVoid(){
		// to remove warning
	}
	
}
