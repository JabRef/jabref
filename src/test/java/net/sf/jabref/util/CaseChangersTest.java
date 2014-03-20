package net.sf.jabref.util;

import junit.framework.TestCase;

public class CaseChangersTest extends TestCase {

	public void testNumberOfModes() {
		assertEquals("lower", CaseChangers.LOWER.getName());
		assertEquals("UPPER", CaseChangers.UPPER.getName());
		assertEquals("Upper first", CaseChangers.UPPER_FIRST.getName());
		assertEquals("Upper Each First", CaseChangers.UPPER_EACH_FIRST.getName());
        assertEquals("Upper Each First and of the Skipped", CaseChangers.UPPER_EACH_FIRST_SKIP_SMALL_WORDS.getName());
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

    public void testChangeCaseUpperEachFirstSkipSmallerWords() {
        assertEquals("", CaseChangers.UPPER_EACH_FIRST_SKIP_SMALL_WORDS.changeCase(""));
        assertEquals("Upper Each First and", CaseChangers.UPPER_EACH_FIRST_SKIP_SMALL_WORDS.changeCase("upper each First and"));
        assertEquals("Upper Each First and", CaseChangers.UPPER_EACH_FIRST_SKIP_SMALL_WORDS.changeCase("upper each First aNd"));
        assertEquals("Upper Each First and", CaseChangers.UPPER_EACH_FIRST_SKIP_SMALL_WORDS.changeCase("upper each First AND"));
    }
}
