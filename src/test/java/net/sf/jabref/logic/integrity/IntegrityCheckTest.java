package net.sf.jabref.logic.integrity;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.MetaData;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.IdGenerator;
import net.sf.jabref.model.entry.BibEntry;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class IntegrityCheckTest {

    @Test
    public void testUrlChecks() {
        assertCorrect("http://www.google.com", IntegrityCheck.URL_CHECKER);
        assertCorrect("https://www.google.com", IntegrityCheck.URL_CHECKER);
        assertCorrect("file://c:/asdf/asdf", IntegrityCheck.URL_CHECKER);
        assertWrong("www.google.com", IntegrityCheck.URL_CHECKER);
        assertWrong("google.com", IntegrityCheck.URL_CHECKER);
        assertWrong("c:/asdf/asdf", IntegrityCheck.URL_CHECKER);
    }

    @Test
    public void testYearChecks() {
        assertCorrect("2014", IntegrityCheck.YEAR_CHECKER);
        assertCorrect("1986", IntegrityCheck.YEAR_CHECKER);
        assertWrong("abc", IntegrityCheck.YEAR_CHECKER);
        assertWrong("86", IntegrityCheck.YEAR_CHECKER);
        assertWrong("204", IntegrityCheck.YEAR_CHECKER);
    }

    @Test
    public void testBraketChecks() {
        assertCorrect("x", IntegrityCheck.BRACKET_CHECKER);
        assertCorrect("{x}", IntegrityCheck.BRACKET_CHECKER);
        assertCorrect("{x}x{}x{{}}", IntegrityCheck.BRACKET_CHECKER);
        assertWrong("{x}x{}}x{{}}", IntegrityCheck.BRACKET_CHECKER);
        assertWrong("}", IntegrityCheck.BRACKET_CHECKER);
        assertWrong("{", IntegrityCheck.BRACKET_CHECKER);
    }

    @Test
    public void regexTest() {
        assertEquals("N,NN", Pattern.compile("[^, ]+").matcher("Knuth, Donald E. ".trim()).replaceAll("N").replaceAll("\\s+", ""));
    }

    @Test
    public void testAuthorNameChecks() {
        assertCorrect("", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertCorrect("Knuth", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertCorrect("   Knuth, Donald E. ", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertCorrect("Knuth, Donald E. and Kurt Cobain and A. Einstein", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertCorrect("Donald E. Knuth and Kurt Cobain and A. Einstein", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertWrong(", and Kurt Cobain and A. Einstein", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertWrong("Donald E. Knuth and Kurt Cobain and ,", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertWrong("and Kurt Cobain and A. Einstein", IntegrityCheck.AUTHOR_NAME_CHECKER);
        assertWrong("Donald E. Knuth and Kurt Cobain and", IntegrityCheck.AUTHOR_NAME_CHECKER);
    }

    @Test
    public void testTitleChecks() {
        assertCorrect("This is a title", IntegrityCheck.TITLE_CHECKER);
        assertWrong("This is a Title", IntegrityCheck.TITLE_CHECKER);
        assertCorrect("This is a {T}itle", IntegrityCheck.TITLE_CHECKER);
        assertCorrect("{This is a Title}", IntegrityCheck.TITLE_CHECKER);
        assertCorrect("This is a {Title}", IntegrityCheck.TITLE_CHECKER);
        assertCorrect("{C}urrent {C}hronicle", IntegrityCheck.TITLE_CHECKER);
        assertCorrect("{A Model-Driven Approach for Monitoring {ebBP} BusinessTransactions}", IntegrityCheck.TITLE_CHECKER);
    }

    @Test
    public void testAbbreviationChecks() {
        assertCorrect("Proceedings of the", IntegrityCheck.ABBREVIATION_CHECKER);
        assertWrong("Proc. of the", IntegrityCheck.ABBREVIATION_CHECKER);
    }

    @Test
    public void testFileChecks() {
        MetaData metaData = Mockito.mock(MetaData.class);
        Mockito.when(metaData.getFileDirectory("file")).thenReturn(Collections.singletonList("."));
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(), metaData, new Defaults());

        assertCorrect(":build.gradle:gradle", IntegrityCheck.FILE_CHECKER, context);
        assertWrong(":asflakjfwofja:PDF", IntegrityCheck.FILE_CHECKER, context);
    }

    @Test
    public void testTypeChecks() {
        LinkedList<IntegrityMessage> messages = new LinkedList<>();
        IntegrityCheck.TYPE_CHECKER.check("11--15","pages",new BibEntry("asdfasdf", "inproceedings"), messages, new BibDatabaseContext());
        assertEquals(Collections.emptyList(), messages);

        messages = new LinkedList<>();
        IntegrityCheck.TYPE_CHECKER.check("11--15","pages",new BibEntry("asdfasdf", "proceedings"), messages, new BibDatabaseContext());
        assertFalse(messages.toString(), messages.isEmpty());
    }

    @Test
    public void testPageNumbersChecks() {
        assertCorrect("1--2", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("12", IntegrityCheck.PAGES_CHECKER);
        assertWrong("1-2", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("1,2,3", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("43+", IntegrityCheck.PAGES_CHECKER);
        assertWrong("1 2", IntegrityCheck.PAGES_CHECKER);
        assertWrong("{1}-{2}", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("7,41,73--97", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("7,41--42,73", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("7--11,41--43,73", IntegrityCheck.PAGES_CHECKER);
        assertCorrect("7+,41--43,73", IntegrityCheck.PAGES_CHECKER);
    }

    private void assertWrong(String value, IntegrityCheck.Checker checker, BibDatabaseContext context) {
        List<IntegrityMessage> messages = new LinkedList<>();
        BibEntry entry = new BibEntry(IdGenerator.next());
        entry.setField(BibEntry.KEY_FIELD, "key");
        checker.check(value, "field", entry, messages, context);
        assertFalse(messages.toString(), messages.isEmpty());
    }

    private void assertWrong(String value, IntegrityCheck.Checker checker) {
        assertWrong(value, checker, new BibDatabaseContext());
    }

    private void assertCorrect(String value, IntegrityCheck.Checker checker) {
        assertCorrect(value, checker, new BibDatabaseContext());
    }

    private void assertCorrect(String value, IntegrityCheck.Checker checker, BibDatabaseContext context) {
        List<IntegrityMessage> messages = new LinkedList<>();
        BibEntry entry = new BibEntry(IdGenerator.next());
        entry.setField(BibEntry.KEY_FIELD, "key");
        checker.check(value, "field", entry, messages, context);
        assertEquals(Collections.emptyList(), messages);
    }

}
