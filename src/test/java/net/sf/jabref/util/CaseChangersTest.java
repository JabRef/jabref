package net.sf.jabref.util;

import junit.framework.TestCase;

public class CaseChangersTest extends TestCase {

	public void testNumberOfModes() {
		assertEquals("lower", CaseChangers.LOWER.getName());
		assertEquals("UPPER", CaseChangers.UPPER.getName());
		assertEquals("Upper first", CaseChangers.UPPER_FIRST.getName());
		assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.getName());
        assertEquals("Title", CaseChangers.TITLE.getName());
	}

    public void testChangeCaseLower() {
        assertEquals("", CaseChangers.LOWER.changeCase(""));
        assertEquals("lower", CaseChangers.LOWER.changeCase("LOWER"));
    }

    public void testChangeCaseUpper() {
        assertEquals("", CaseChangers.UPPER.changeCase(""));
        assertEquals("LOWER", CaseChangers.UPPER.changeCase("LOWER"));
        assertEquals("UPPER", CaseChangers.UPPER.changeCase("upper"));
        assertEquals("UPPER", CaseChangers.UPPER.changeCase("UPPER"));
    }

    public void testChangeCaseUpperFirst() {
        assertEquals("", CaseChangers.UPPER_FIRST.changeCase(""));
        assertEquals("Upper first", CaseChangers.UPPER_FIRST.changeCase("upper First"));
    }

	public void testChangeCaseUpperEachFirst() {
		assertEquals("", CaseChangers.UPPER_EACH_FIRST.changeCase(""));
		assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.changeCase("upper each First"));
	}

    public void testChangeCaseTitle() {
        assertEquals("", CaseChangers.TITLE.changeCase(""));
        assertEquals("Upper Each First", CaseChangers.TITLE.changeCase("upper each first"));
        assertEquals("An Upper Each First And", CaseChangers.TITLE.changeCase("an upper each first and"));
        assertEquals("An Upper Each of the and First And", CaseChangers.TITLE.changeCase("an upper each of the and first and"));
    }
}
