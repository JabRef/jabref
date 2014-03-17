package tests.net.sf.jabref.util;

import junit.framework.TestCase;
import net.sf.jabref.util.CaseChanger;

public class CaseChangerTest extends TestCase {

	public void testNumberOfModes() {
		
		// If this fails we know there are new modes
		assertEquals(4, CaseChanger.getNumModes());
		assertEquals(4, CaseChanger.getModeNames().length);
		
		String[] modeNames = CaseChanger.getModeNames();
		for (int i = 0; i < CaseChanger.getNumModes(); i++){
			assertEquals(CaseChanger.getModeName(i), modeNames[i]);
		}
		assertEquals("lower", CaseChanger.getModeName(0));
		assertEquals("UPPER", CaseChanger.getModeName(1));
		assertEquals("Upper first", CaseChanger.getModeName(2));
		assertEquals("Upper Each First", CaseChanger.getModeName(3));
	}

	public void testChangeCaseStringArrayInt() {
		String[] s = new String[0];
		assertEquals(0, CaseChanger.changeCase(s, 0).length);
		
		s = new String[]{"UPPER", "UPdownUPdown", "Mary has a little Lamb"};
		
		s = CaseChanger.changeCase(s,0);
		assertEquals(3, s.length);
		assertEquals("upper", s[0]);
		assertEquals("updownupdown", s[1]);
		assertEquals("mary has a little lamb", s[2]);
	}

	public void testChangeCaseStringInt() {

		assertEquals("", CaseChanger.changeCase("", 0));
		assertEquals("", CaseChanger.changeCase("", 1));
		assertEquals("", CaseChanger.changeCase("", 2));
		assertEquals("", CaseChanger.changeCase("", 3));
		
		assertEquals("lower", CaseChanger.changeCase("LOWER", 0));
		assertEquals("LOWER", CaseChanger.changeCase("LOWER", 1));
		assertEquals("UPPER", CaseChanger.changeCase("upper", 1));
		assertEquals("UPPER", CaseChanger.changeCase("UPPER", 1));
		
		assertEquals("Upper first", CaseChanger.changeCase("upper First", 2));
		
		assertEquals("Upper Each First", CaseChanger.changeCase("upper each First", 3));
	}
	
	public void testPreserveBrackets(){
		
		
	}
}
