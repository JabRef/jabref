package net.sf.jabref.model.entry;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christopher Oezbek <oezi@oezi.de>
 * @version 0.1 - Still fails for stuff in AuthorList that is ambiguous
 * @see AuthorList Class tested.
 */
public class AuthorListTest {

    @SuppressWarnings("unused")
	@Test
    public void testFixAuthorNatbib() {
        Assert.assertEquals("", AuthorList.fixAuthorNatbib(""));
        Assert.assertEquals("Smith", AuthorList.fixAuthorNatbib("John Smith"));
        Assert.assertEquals("Smith and Black Brown", AuthorList
                .fixAuthorNatbib("John Smith and Black Brown, Peter"));
        Assert.assertEquals("von Neumann et al.", AuthorList
                .fixAuthorNatbib("John von Neumann and John Smith and Black Brown, Peter"));

        // Is not cached!
        Assert.assertTrue(AuthorList
                .fixAuthorNatbib("John von Neumann and John Smith and Black Brown, Peter").equals(AuthorList
                        .fixAuthorNatbib("John von Neumann" + (0 == 1 ? "" : " and ")
                                + "John Smith and Black Brown, Peter")));
    }

    @Test
    public void testGetAuthorList() {
        // Test caching in authorCache.
        AuthorList al = AuthorList.parse("John Smith");
        Assert.assertTrue(al == AuthorList.parse("John Smith"));
        Assert.assertFalse(al == AuthorList.parse("Smith"));
    }

    @SuppressWarnings("unused")
	@Test
    public void testFixAuthorFirstNameFirstCommas() {

        // No Commas
        Assert.assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", true, false));
        Assert.assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", false, false));

        Assert.assertEquals("John Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith",
                false, false));
        Assert.assertEquals("J. Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith", true,
                false));

        // Check caching
        Assert.assertTrue(AuthorList.fixAuthorFirstNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, false).equals(AuthorList
                .fixAuthorFirstNameFirstCommas("John von Neumann" + (0 == 1 ? "" : " and ")
                        + "John Smith and Black Brown, Peter", true, false)));

        Assert.assertEquals("John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas("John Smith and Black Brown, Peter", false, false));
        Assert.assertEquals("J. Smith and P. Black Brown", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Smith and Black Brown, Peter", true, false));

        // Method description is different than code -> additional comma
        // there
        Assert.assertEquals("John von Neumann, John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false, false));
        Assert.assertEquals("J. von Neumann, J. Smith and P. Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true, false));

        Assert.assertEquals("J. P. von Neumann", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Peter von Neumann", true, false));
        // Oxford Commas
        Assert.assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", true, true));
        Assert.assertEquals("", AuthorList.fixAuthorFirstNameFirstCommas("", false, true));

        Assert.assertEquals("John Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith",
                false, true));
        Assert.assertEquals("J. Smith", AuthorList.fixAuthorFirstNameFirstCommas("John Smith", true,
                true));

        // Check caching
        Assert.assertTrue(AuthorList.fixAuthorFirstNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, true).equals(AuthorList
                .fixAuthorFirstNameFirstCommas("John von Neumann" + (0 == 1 ? "" : " and ")
                        + "John Smith and Black Brown, Peter", true, true)));

        Assert.assertEquals("John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas("John Smith and Black Brown, Peter", false, true));
        Assert.assertEquals("J. Smith and P. Black Brown", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Smith and Black Brown, Peter", true, true));

        // Method description is different than code -> additional comma
        // there
        Assert.assertEquals("John von Neumann, John Smith, and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false, true));
        Assert.assertEquals("J. von Neumann, J. Smith, and P. Black Brown", AuthorList
                .fixAuthorFirstNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true, true));

        Assert.assertEquals("J. P. von Neumann", AuthorList.fixAuthorFirstNameFirstCommas(
                "John Peter von Neumann", true, true));

    }

    @SuppressWarnings("unused")
	@Test
    public void testFixAuthorFirstNameFirst() {
        Assert.assertEquals("John Smith", AuthorList.fixAuthorFirstNameFirst("John Smith"));

        Assert.assertEquals("John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirst("John Smith and Black Brown, Peter"));

        Assert.assertEquals("John von Neumann and John Smith and Peter Black Brown", AuthorList
                .fixAuthorFirstNameFirst("John von Neumann and John Smith and Black Brown, Peter"));

        Assert.assertEquals("First von Last, Jr. III", AuthorList
                .fixAuthorFirstNameFirst("von Last, Jr. III, First"));

        // Check caching
        Assert.assertTrue(AuthorList
                .fixAuthorFirstNameFirst("John von Neumann and John Smith and Black Brown, Peter").equals(AuthorList
                        .fixAuthorFirstNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
                                + "John Smith and Black Brown, Peter")));

    }

    @SuppressWarnings("unused")
	@Test
    public void testFixAuthorLastNameFirstCommasNoComma() {
        // No commas before and
        Assert.assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", true, false));
        Assert.assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", false, false));

        Assert.assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirstCommas("John Smith", false, false));
        Assert.assertEquals("Smith, J.", AuthorList.fixAuthorLastNameFirstCommas("John Smith", true, false));

        String a = AuthorList.fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter",
                true, false);
        String b = AuthorList.fixAuthorLastNameFirstCommas(
                "John von Neumann" + (0 == 1 ? "" : " and ") + "John Smith and Black Brown, Peter", true, false);

        // Check caching
        Assert.assertEquals(a, b);
        Assert.assertTrue(a.equals(b));

        Assert.assertEquals("Smith, John and Black Brown, Peter",
                AuthorList.fixAuthorLastNameFirstCommas("John Smith and Black Brown, Peter", false, false));
        Assert.assertEquals("Smith, J. and Black Brown, P.",
                AuthorList.fixAuthorLastNameFirstCommas("John Smith and Black Brown, Peter", true, false));

        Assert.assertEquals("von Neumann, John, Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", false, false));
        Assert.assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", AuthorList
                .fixAuthorLastNameFirstCommas("John von Neumann and John Smith and Black Brown, Peter", true, false));

        Assert.assertEquals("von Neumann, J. P.",
                AuthorList.fixAuthorLastNameFirstCommas("John Peter von Neumann", true, false));
    }

    @SuppressWarnings("unused")
    @Test
    public void testFixAuthorLastNameFirstCommasOxfordComma() {
        // Oxford Commas
        Assert.assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", true, true));
        Assert.assertEquals("", AuthorList.fixAuthorLastNameFirstCommas("", false, true));

        Assert.assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirstCommas("John Smith",
                false, true));
        Assert.assertEquals("Smith, J.", AuthorList.fixAuthorLastNameFirstCommas("John Smith", true,
                true));

        String a = AuthorList.fixAuthorLastNameFirstCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true, true);
        String b = AuthorList.fixAuthorLastNameFirstCommas("John von Neumann"
                + (0 == 1 ? "" : " and ") + "John Smith and Black Brown, Peter", true, true);

        // Check caching
        Assert.assertEquals(a, b);
        Assert.assertTrue(a.equals(b));

        Assert.assertEquals("Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirstCommas("John Smith and Black Brown, Peter", false, true));
        Assert.assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthorLastNameFirstCommas(
                "John Smith and Black Brown, Peter", true, true));

        Assert.assertEquals("von Neumann, John, Smith, John, and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false, true));
        Assert.assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", AuthorList
                .fixAuthorLastNameFirstCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true, true));

        Assert.assertEquals("von Neumann, J. P.", AuthorList.fixAuthorLastNameFirstCommas(
                "John Peter von Neumann", true, true));
    }

    @SuppressWarnings("unused")
	@Test
    public void testFixAuthorLastNameFirst() {

        // Test helper method

        Assert.assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirst("John Smith"));

        Assert.assertEquals("Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirst("John Smith and Black Brown, Peter"));

        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter"));

        Assert.assertEquals("von Last, Jr, First", AuthorList
                .fixAuthorLastNameFirst("von Last, Jr ,First"));

        Assert.assertTrue(AuthorList
                .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter").equals(AuthorList
                        .fixAuthorLastNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
                                + "John Smith and Black Brown, Peter")));

        // Test Abbreviation == false
        Assert.assertEquals("Smith, John", AuthorList.fixAuthorLastNameFirst("John Smith", false));

        Assert.assertEquals("Smith, John and Black Brown, Peter", AuthorList.fixAuthorLastNameFirst(
                "John Smith and Black Brown, Peter", false));

        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
                .fixAuthorLastNameFirst("John von Neumann and John Smith and Black Brown, Peter",
                        false));

        Assert.assertEquals("von Last, Jr, First", AuthorList.fixAuthorLastNameFirst(
                "von Last, Jr ,First", false));

        Assert.assertTrue(AuthorList.fixAuthorLastNameFirst(
                "John von Neumann and John Smith and Black Brown, Peter", false).equals(AuthorList
                .fixAuthorLastNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
                        + "John Smith and Black Brown, Peter", false)));

        // Test Abbreviate == true
        Assert.assertEquals("Smith, J.", AuthorList.fixAuthorLastNameFirst("John Smith", true));

        Assert.assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthorLastNameFirst(
                "John Smith and Black Brown, Peter", true));

        Assert.assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
                AuthorList.fixAuthorLastNameFirst(
                        "John von Neumann and John Smith and Black Brown, Peter", true));

        Assert.assertEquals("von Last, Jr, F.", AuthorList.fixAuthorLastNameFirst("von Last, Jr ,First",
                true));

        Assert.assertTrue(AuthorList.fixAuthorLastNameFirst(
                "John von Neumann and John Smith and Black Brown, Peter", true).equals(AuthorList
                .fixAuthorLastNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
                        + "John Smith and Black Brown, Peter", true)));

    }

    @SuppressWarnings("unused")
	@Test
    public void testFixAuthorLastNameOnlyCommas() {

        // No comma before and
        Assert.assertEquals("", AuthorList.fixAuthorLastNameOnlyCommas("", false));
        Assert.assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("John Smith", false));
        Assert.assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("Smith, Jr, John", false));

        Assert.assertTrue(AuthorList.fixAuthorLastNameOnlyCommas(
                "John von Neumann and John Smith and Black Brown, Peter", false).equals(AuthorList
                .fixAuthorLastNameOnlyCommas("John von Neumann" + (0 == 1 ? "" : " and ")
                        + "John Smith and Black Brown, Peter", false)));

        Assert.assertEquals("von Neumann, Smith and Black Brown", AuthorList
                .fixAuthorLastNameOnlyCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", false));
        // Oxford Comma
        Assert.assertEquals("", AuthorList.fixAuthorLastNameOnlyCommas("", true));
        Assert.assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("John Smith", true));
        Assert.assertEquals("Smith", AuthorList.fixAuthorLastNameOnlyCommas("Smith, Jr, John", true));

        Assert.assertTrue(AuthorList.fixAuthorLastNameOnlyCommas(
                "John von Neumann and John Smith and Black Brown, Peter", true).equals(AuthorList
                .fixAuthorLastNameOnlyCommas("John von Neumann" + (0 == 1 ? "" : " and ")
                        + "John Smith and Black Brown, Peter", true)));

        Assert.assertEquals("von Neumann, Smith, and Black Brown", AuthorList
                .fixAuthorLastNameOnlyCommas(
                        "John von Neumann and John Smith and Black Brown, Peter", true));
    }

    @Test
    public void testFixAuthorForAlphabetization() {
        Assert.assertEquals("Smith, J.", AuthorList.fixAuthorForAlphabetization("John Smith"));
        Assert.assertEquals("Neumann, J.", AuthorList.fixAuthorForAlphabetization("John von Neumann"));
        Assert.assertEquals("Neumann, J.", AuthorList.fixAuthorForAlphabetization("J. von Neumann"));
        Assert.assertEquals(
                "Neumann, J. and Smith, J. and Black Brown, Jr., P.",
                AuthorList
                        .fixAuthorForAlphabetization("John von Neumann and John Smith and de Black Brown, Jr., Peter"));
    }

    public static int size(String bibtex) {
        return AuthorList.parse(bibtex).getNumberOfAuthors();
    }

    @Test
    public void testSize() {

        Assert.assertEquals(0, AuthorListTest.size(""));
        Assert.assertEquals(1, AuthorListTest.size("Bar"));
        Assert.assertEquals(1, AuthorListTest.size("Foo Bar"));
        Assert.assertEquals(1, AuthorListTest.size("Foo von Bar"));
        Assert.assertEquals(1, AuthorListTest.size("von Bar, Foo"));
        Assert.assertEquals(1, AuthorListTest.size("Bar, Foo"));
        Assert.assertEquals(1, AuthorListTest.size("Bar, Jr., Foo"));
        Assert.assertEquals(1, AuthorListTest.size("Bar, Foo"));
        Assert.assertEquals(2, AuthorListTest.size("John Neumann and Foo Bar"));
        Assert.assertEquals(2, AuthorListTest.size("John von Neumann and Bar, Jr, Foo"));

        Assert.assertEquals(3, AuthorListTest.size("John von Neumann and John Smith and Black Brown, Peter"));

        StringBuilder s = new StringBuilder("John von Neumann");
        for (int i = 0; i < 25; i++) {
            Assert.assertEquals(i + 1, AuthorListTest.size(s.toString()));
            s.append(" and Albert Einstein");
        }
    }

    @Test
    public void testIsEmpty() {

        Assert.assertTrue(AuthorList.parse("").isEmpty());
        Assert.assertFalse(AuthorList.parse("Bar").isEmpty());
    }

    @Test(expected = Exception.class)
    public void testGetEmptyAuthor() {
        AuthorList.parse("").getAuthor(0);
        Assert.fail();
    }

    @Test
    public void testGetAuthor() {

        Author author = AuthorList.parse("John Smith and von Neumann, Jr, John").getAuthor(0);
        Assert.assertEquals("John", author.getFirst());
        Assert.assertEquals("J.", author.getFirstAbbr());
        Assert.assertEquals("John Smith", author.getFirstLast(false));
        Assert.assertEquals("J. Smith", author.getFirstLast(true));
        Assert.assertEquals(null, author.getJr());
        Assert.assertEquals("Smith", author.getLast());
        Assert.assertEquals("Smith, John", author.getLastFirst(false));
        Assert.assertEquals("Smith, J.", author.getLastFirst(true));
        Assert.assertEquals("Smith", author.getLastOnly());
        Assert.assertEquals("Smith, J.", author.getNameForAlphabetization());
        Assert.assertEquals(null, author.getVon());

        author = AuthorList.parse("Peter Black Brown").getAuthor(0);
        Assert.assertEquals("Peter Black", author.getFirst());
        Assert.assertEquals("P. B.", author.getFirstAbbr());
        Assert.assertEquals("Peter Black Brown", author.getFirstLast(false));
        Assert.assertEquals("P. B. Brown", author.getFirstLast(true));
        Assert.assertEquals(null, author.getJr());
        Assert.assertEquals(null, author.getVon());

        author = AuthorList.parse("John Smith and von Neumann, Jr, John").getAuthor(1);
        Assert.assertEquals("John", author.getFirst());
        Assert.assertEquals("J.", author.getFirstAbbr());
        Assert.assertEquals("John von Neumann, Jr", author.getFirstLast(false));
        Assert.assertEquals("J. von Neumann, Jr", author.getFirstLast(true));
        Assert.assertEquals("Jr", author.getJr());
        Assert.assertEquals("Neumann", author.getLast());
        Assert.assertEquals("von Neumann, Jr, John", author.getLastFirst(false));
        Assert.assertEquals("von Neumann, Jr, J.", author.getLastFirst(true));
        Assert.assertEquals("von Neumann", author.getLastOnly());
        Assert.assertEquals("Neumann, Jr, J.", author.getNameForAlphabetization());
        Assert.assertEquals("von", author.getVon());
    }

    @Test
    public void testGetAuthorsNatbib() {
        Assert.assertEquals("", AuthorList.parse("").getAsNatbib());
        Assert.assertEquals("Smith", AuthorList.parse("John Smith").getAsNatbib());
        Assert.assertEquals("Smith and Black Brown", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsNatbib());
        Assert.assertEquals("von Neumann et al.", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsNatbib());

        /*
         * [ 1465610 ] (Double-)Names containing hyphen (-) not handled correctly
         */
        Assert.assertEquals("Last-Name et al.", AuthorList.parse(
                "First Second Last-Name" + " and John Smith and Black Brown, Peter").getAsNatbib());

        // Test caching
        AuthorList al = AuthorList
                .parse("John von Neumann and John Smith and Black Brown, Peter");
        Assert.assertTrue(al.getAsNatbib().equals(al.getAsNatbib()));
    }

    @Test
    public void testGetAuthorsLastOnly() {

        // No comma before and
        Assert.assertEquals("", AuthorList.parse("").getAsLastNames(false));
        Assert.assertEquals("Smith", AuthorList.parse("John Smith").getAsLastNames(false));
        Assert.assertEquals("Smith", AuthorList.parse("Smith, Jr, John").getAsLastNames(
                false));

        Assert.assertEquals("von Neumann, Smith and Black Brown", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsLastNames(false));
        // Oxford comma
        Assert.assertEquals("", AuthorList.parse("").getAsLastNames(true));
        Assert.assertEquals("Smith", AuthorList.parse("John Smith").getAsLastNames(true));
        Assert.assertEquals("Smith", AuthorList.parse("Smith, Jr, John").getAsLastNames(
                true));

        Assert.assertEquals("von Neumann, Smith, and Black Brown", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsLastNames(true));

        Assert.assertEquals("von Neumann and Smith",
                AuthorList.parse("John von Neumann and John Smith").getAsLastNames(false));
    }

    @Test
    public void testGetAuthorsLastFirstNoComma() {
        // No commas before and
        AuthorList al;

        al = AuthorList.parse("");
        Assert.assertEquals("", al.getAsLastFirstNames(true, false));
        Assert.assertEquals("", al.getAsLastFirstNames(false, false));

        al = AuthorList.parse("John Smith");
        Assert.assertEquals("Smith, John", al.getAsLastFirstNames(false, false));
        Assert.assertEquals("Smith, J.", al.getAsLastFirstNames(true, false));

        al = AuthorList.parse("John Smith and Black Brown, Peter");
        Assert.assertEquals("Smith, John and Black Brown, Peter", al.getAsLastFirstNames(false, false));
        Assert.assertEquals("Smith, J. and Black Brown, P.", al.getAsLastFirstNames(true, false));

        al = AuthorList.parse("John von Neumann and John Smith and Black Brown, Peter");
        // Method description is different than code -> additional comma
        // there
        Assert.assertEquals("von Neumann, John, Smith, John and Black Brown, Peter",
                al.getAsLastFirstNames(false, false));
        Assert.assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", al.getAsLastFirstNames(true, false));

        al = AuthorList.parse("John Peter von Neumann");
        Assert.assertEquals("von Neumann, J. P.", al.getAsLastFirstNames(true, false));
    }

    @Test
    public void testGetAuthorsLastFirstOxfordComma() {
        // Oxford comma
        AuthorList al;

        al = AuthorList.parse("");
        Assert.assertEquals("", al.getAsLastFirstNames(true, true));
        Assert.assertEquals("", al.getAsLastFirstNames(false, true));

        al = AuthorList.parse("John Smith");
        Assert.assertEquals("Smith, John", al.getAsLastFirstNames(false, true));
        Assert.assertEquals("Smith, J.", al.getAsLastFirstNames(true, true));

        al = AuthorList.parse("John Smith and Black Brown, Peter");
        Assert.assertEquals("Smith, John and Black Brown, Peter", al.getAsLastFirstNames(false, true));
        Assert.assertEquals("Smith, J. and Black Brown, P.", al.getAsLastFirstNames(true, true));

        al = AuthorList.parse("John von Neumann and John Smith and Black Brown, Peter");
        Assert.assertEquals("von Neumann, John, Smith, John, and Black Brown, Peter", al
                .getAsLastFirstNames(false, true));
        Assert.assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", al.getAsLastFirstNames(
                true, true));

        al = AuthorList.parse("John Peter von Neumann");
        Assert.assertEquals("von Neumann, J. P.", al.getAsLastFirstNames(true, true));
    }

    @Test
    public void testGetAuthorsLastFirstAnds() {
        Assert.assertEquals("Smith, John", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(
                false));
        Assert.assertEquals("Smith, John and Black Brown, Peter", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsLastFirstNamesWithAnd(false));
        Assert.assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
                .parse("John von Neumann and John Smith and Black Brown, Peter")
                .getAsLastFirstNamesWithAnd(false));
        Assert.assertEquals("von Last, Jr, First", AuthorList.parse("von Last, Jr ,First")
                .getAsLastFirstNamesWithAnd(false));

        Assert.assertEquals("Smith, J.", AuthorList.parse("John Smith").getAsLastFirstNamesWithAnd(
                true));
        Assert.assertEquals("Smith, J. and Black Brown, P.", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsLastFirstNamesWithAnd(true));
        Assert.assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.", AuthorList.parse(
                "John von Neumann and John Smith and Black Brown, Peter").getAsLastFirstNamesWithAnd(true));
        Assert.assertEquals("von Last, Jr, F.", AuthorList.parse("von Last, Jr ,First")
                .getAsLastFirstNamesWithAnd(true));

    }

    @Test
    public void testGetAuthorsFirstFirst() {

        AuthorList al;

        al = AuthorList.parse("");
        Assert.assertEquals("", al.getAsFirstLastNames(true, false));
        Assert.assertEquals("", al.getAsFirstLastNames(false, false));
        Assert.assertEquals("", al.getAsFirstLastNames(true, true));
        Assert.assertEquals("", al.getAsFirstLastNames(false, true));

        al = AuthorList.parse("John Smith");
        Assert.assertEquals("John Smith", al.getAsFirstLastNames(false, false));
        Assert.assertEquals("J. Smith", al.getAsFirstLastNames(true, false));
        Assert.assertEquals("John Smith", al.getAsFirstLastNames(false, true));
        Assert.assertEquals("J. Smith", al.getAsFirstLastNames(true, true));

        al = AuthorList.parse("John Smith and Black Brown, Peter");
        Assert.assertEquals("John Smith and Peter Black Brown", al.getAsFirstLastNames(false, false));
        Assert.assertEquals("J. Smith and P. Black Brown", al.getAsFirstLastNames(true, false));
        Assert.assertEquals("John Smith and Peter Black Brown", al.getAsFirstLastNames(false, true));
        Assert.assertEquals("J. Smith and P. Black Brown", al.getAsFirstLastNames(true, true));

        al = AuthorList.parse("John von Neumann and John Smith and Black Brown, Peter");
        Assert.assertEquals("John von Neumann, John Smith and Peter Black Brown", al.getAsFirstLastNames(
                false, false));
        Assert.assertEquals("J. von Neumann, J. Smith and P. Black Brown", al.getAsFirstLastNames(true,
                false));
        Assert.assertEquals("John von Neumann, John Smith, and Peter Black Brown", al
                .getAsFirstLastNames(false, true));
        Assert.assertEquals("J. von Neumann, J. Smith, and P. Black Brown", al.getAsFirstLastNames(true,
                true));

        al = AuthorList.parse("John Peter von Neumann");
        Assert.assertEquals("John Peter von Neumann", al.getAsFirstLastNames(false, false));
        Assert.assertEquals("John Peter von Neumann", al.getAsFirstLastNames(false, true));
        Assert.assertEquals("J. P. von Neumann", al.getAsFirstLastNames(true, false));
        Assert.assertEquals("J. P. von Neumann", al.getAsFirstLastNames(true, true));
    }

    @Test
    public void testGetAuthorsFirstFirstAnds() {
        Assert.assertEquals("John Smith", AuthorList.parse("John Smith")
                .getAsFirstLastNamesWithAnd());
        Assert.assertEquals("John Smith and Peter Black Brown", AuthorList.parse(
                "John Smith and Black Brown, Peter").getAsFirstLastNamesWithAnd());
        Assert.assertEquals("John von Neumann and John Smith and Peter Black Brown", AuthorList
                .parse("John von Neumann and John Smith and Black Brown, Peter")
                .getAsFirstLastNamesWithAnd());
        Assert.assertEquals("First von Last, Jr. III", AuthorList
                .parse("von Last, Jr. III, First").getAsFirstLastNamesWithAnd());
    }

    @Test
    public void testGetAuthorsForAlphabetization() {
        Assert.assertEquals("Smith, J.", AuthorList.parse("John Smith")
                .getForAlphabetization());
        Assert.assertEquals("Neumann, J.", AuthorList.parse("John von Neumann")
                .getForAlphabetization());
        Assert.assertEquals("Neumann, J.", AuthorList.parse("J. von Neumann")
                .getForAlphabetization());
        Assert.assertEquals("Neumann, J. and Smith, J. and Black Brown, Jr., P.", AuthorList
                .parse("John von Neumann and John Smith and de Black Brown, Jr., Peter")
                .getForAlphabetization());
    }

    @Test
    public void testRemoveStartAndEndBraces() {
        Assert.assertEquals("{A}bbb{c}", AuthorList.parse("{A}bbb{c}").getAsLastNames(false));
        Assert.assertEquals("Vall{\\'e}e Poussin", AuthorList.parse("{Vall{\\'e}e Poussin}").getAsLastNames(false));
        Assert.assertEquals("Poussin", AuthorList.parse("{Vall{\\'e}e} {Poussin}").getAsLastNames(false));
        Assert.assertEquals("Poussin", AuthorList.parse("Vall{\\'e}e Poussin").getAsLastNames(false));
        Assert.assertEquals("Lastname", AuthorList.parse("Firstname {Lastname}").getAsLastNames(false));
        Assert.assertEquals("Firstname Lastname", AuthorList.parse("{Firstname Lastname}").getAsLastNames(false));
    }

    @Test
    public void createCorrectInitials() {
        Assert.assertEquals("J. G.", AuthorList.parse("Hornberg, Johann Gottfried").getAuthor(0).getFirstAbbr());
    }
}
