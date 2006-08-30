package tests.net.sf.jabref;

import junit.framework.TestCase;
import net.sf.jabref.AuthorList;

/**
 * 
 * @see net.sf.jabref.AuthorList Class tested.
 * 
 * @author Christopher Oezbek <oezi@oezi.de>
 * 
 * @version 0.1 - Still fails for stuff in AuthorList that is ambiguous
 */
public class AuthorListTest extends TestCase {

	public void testFixAuthor_Natbib() {
		assertEquals("", AuthorList.fixAuthor_Natbib(""));
		assertEquals("Smith", AuthorList.fixAuthor_Natbib("John Smith"));
		assertEquals("Smith and Black Brown", AuthorList
			.fixAuthor_Natbib("John Smith and Black Brown, Peter"));
		assertEquals("von Neumann et al.", AuthorList
			.fixAuthor_Natbib("John von Neumann and John Smith and Black Brown, Peter"));

		// Is not cached!
		assertTrue(AuthorList
			.fixAuthor_Natbib("John von Neumann and John Smith and Black Brown, Peter") == AuthorList
			.fixAuthor_Natbib("John von Neumann" + (0 == 1 ? "" : " and ")
				+ "John Smith and Black Brown, Peter"));
	}

	public void testGetAuthorList() {
		// Test caching in authorCache.
		AuthorList al = AuthorList.getAuthorList("John Smith");
		assertTrue(al == AuthorList.getAuthorList("John Smith"));
		assertFalse(al == AuthorList.getAuthorList("Smith"));
	}

	public void testFixAuthor_firstNameFirstCommas() {

		{ // No Commas
			assertEquals("", AuthorList.fixAuthor_firstNameFirstCommas("", true, false));
			assertEquals("", AuthorList.fixAuthor_firstNameFirstCommas("", false, false));

			assertEquals("John Smith", AuthorList.fixAuthor_firstNameFirstCommas("John Smith",
				false, false));
			assertEquals("J. Smith", AuthorList.fixAuthor_firstNameFirstCommas("John Smith", true,
				false));

			// Check caching
			assertTrue(AuthorList.fixAuthor_firstNameFirstCommas(
				"John von Neumann and John Smith and Black Brown, Peter", true, false) == AuthorList
				.fixAuthor_firstNameFirstCommas("John von Neumann" + (0 == 1 ? "" : " and ")
					+ "John Smith and Black Brown, Peter", true, false));

			assertEquals("John Smith and Peter Black Brown", AuthorList
				.fixAuthor_firstNameFirstCommas("John Smith and Black Brown, Peter", false, false));
			assertEquals("J. Smith and P. Black Brown", AuthorList.fixAuthor_firstNameFirstCommas(
				"John Smith and Black Brown, Peter", true, false));

			// Method description is different than code -> additional comma
			// there
			assertEquals("John von Neumann, John Smith and Peter Black Brown", AuthorList
				.fixAuthor_firstNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", false, false));
			assertEquals("J. von Neumann, J. Smith and P. Black Brown", AuthorList
				.fixAuthor_firstNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", true, false));

			assertEquals("J. P. von Neumann", AuthorList.fixAuthor_firstNameFirstCommas(
				"John Peter von Neumann", true, false));
		}
		{ // Oxford Commas
			assertEquals("", AuthorList.fixAuthor_firstNameFirstCommas("", true, true));
			assertEquals("", AuthorList.fixAuthor_firstNameFirstCommas("", false, true));

			assertEquals("John Smith", AuthorList.fixAuthor_firstNameFirstCommas("John Smith",
				false, true));
			assertEquals("J. Smith", AuthorList.fixAuthor_firstNameFirstCommas("John Smith", true,
				true));

			// Check caching
			assertTrue(AuthorList.fixAuthor_firstNameFirstCommas(
				"John von Neumann and John Smith and Black Brown, Peter", true, true) == AuthorList
				.fixAuthor_firstNameFirstCommas("John von Neumann" + (0 == 1 ? "" : " and ")
					+ "John Smith and Black Brown, Peter", true, true));

			assertEquals("John Smith and Peter Black Brown", AuthorList
				.fixAuthor_firstNameFirstCommas("John Smith and Black Brown, Peter", false, true));
			assertEquals("J. Smith and P. Black Brown", AuthorList.fixAuthor_firstNameFirstCommas(
				"John Smith and Black Brown, Peter", true, true));

			// Method description is different than code -> additional comma
			// there
			assertEquals("John von Neumann, John Smith, and Peter Black Brown", AuthorList
				.fixAuthor_firstNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", false, true));
			assertEquals("J. von Neumann, J. Smith, and P. Black Brown", AuthorList
				.fixAuthor_firstNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", true, true));

			assertEquals("J. P. von Neumann", AuthorList.fixAuthor_firstNameFirstCommas(
				"John Peter von Neumann", true, true));

		}
	}

	public void testFixAuthor_firstNameFirst() {
		assertEquals("John Smith", AuthorList.fixAuthor_firstNameFirst("John Smith"));

		assertEquals("John Smith and Peter Black Brown", AuthorList
			.fixAuthor_firstNameFirst("John Smith and Black Brown, Peter"));

		assertEquals("John von Neumann and John Smith and Peter Black Brown", AuthorList
			.fixAuthor_firstNameFirst("John von Neumann and John Smith and Black Brown, Peter"));

		assertEquals("First von Last, Jr. III", AuthorList
			.fixAuthor_firstNameFirst("von Last, Jr. III, First"));

		// Check caching
		assertTrue(AuthorList
			.fixAuthor_firstNameFirst("John von Neumann and John Smith and Black Brown, Peter") == AuthorList
			.fixAuthor_firstNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
				+ "John Smith and Black Brown, Peter"));

	}

	public void testFixAuthor_lastNameFirstCommas() {

		{ // No commas before and
			assertEquals("", AuthorList.fixAuthor_lastNameFirstCommas("", true, false));
			assertEquals("", AuthorList.fixAuthor_lastNameFirstCommas("", false, false));

			assertEquals("Smith, John", AuthorList.fixAuthor_lastNameFirstCommas("John Smith",
				false, false));
			assertEquals("Smith, J.", AuthorList.fixAuthor_lastNameFirstCommas("John Smith", true,
				false));

			String a = AuthorList.fixAuthor_lastNameFirstCommas(
				"John von Neumann and John Smith and Black Brown, Peter", true, false);
			String b = AuthorList.fixAuthor_lastNameFirstCommas("John von Neumann"
				+ (0 == 1 ? "" : " and ") + "John Smith and Black Brown, Peter", true, false);

			// Check caching
			assertEquals(a, b);
			assertTrue(a == b);

			assertEquals("Smith, John and Black Brown, Peter", AuthorList
				.fixAuthor_lastNameFirstCommas("John Smith and Black Brown, Peter", false, false));
			assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthor_lastNameFirstCommas(
				"John Smith and Black Brown, Peter", true, false));

			assertEquals("von Neumann, John, Smith, John and Black Brown, Peter", AuthorList
				.fixAuthor_lastNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", false, false));
			assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", AuthorList
				.fixAuthor_lastNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", true, false));

			assertEquals("von Neumann, J. P.", AuthorList.fixAuthor_lastNameFirstCommas(
				"John Peter von Neumann", true, false));
		}
		{ // Oxford Commas
			assertEquals("", AuthorList.fixAuthor_lastNameFirstCommas("", true, true));
			assertEquals("", AuthorList.fixAuthor_lastNameFirstCommas("", false, true));

			assertEquals("Smith, John", AuthorList.fixAuthor_lastNameFirstCommas("John Smith",
				false, true));
			assertEquals("Smith, J.", AuthorList.fixAuthor_lastNameFirstCommas("John Smith", true,
				true));

			String a = AuthorList.fixAuthor_lastNameFirstCommas(
				"John von Neumann and John Smith and Black Brown, Peter", true, true);
			String b = AuthorList.fixAuthor_lastNameFirstCommas("John von Neumann"
				+ (0 == 1 ? "" : " and ") + "John Smith and Black Brown, Peter", true, true);

			// Check caching
			assertEquals(a, b);
			assertTrue(a == b);

			assertEquals("Smith, John and Black Brown, Peter", AuthorList
				.fixAuthor_lastNameFirstCommas("John Smith and Black Brown, Peter", false, true));
			assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthor_lastNameFirstCommas(
				"John Smith and Black Brown, Peter", true, true));

			assertEquals("von Neumann, John, Smith, John, and Black Brown, Peter", AuthorList
				.fixAuthor_lastNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", false, true));
			assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", AuthorList
				.fixAuthor_lastNameFirstCommas(
					"John von Neumann and John Smith and Black Brown, Peter", true, true));

			assertEquals("von Neumann, J. P.", AuthorList.fixAuthor_lastNameFirstCommas(
				"John Peter von Neumann", true, true));
		}
	}

	public void testFixAuthor_lastNameFirst() {

		// Test helper method

		assertEquals("Smith, John", AuthorList.fixAuthor_lastNameFirst("John Smith"));

		assertEquals("Smith, John and Black Brown, Peter", AuthorList
			.fixAuthor_lastNameFirst("John Smith and Black Brown, Peter"));

		assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
			.fixAuthor_lastNameFirst("John von Neumann and John Smith and Black Brown, Peter"));

		assertEquals("von Last, Jr, First", AuthorList
			.fixAuthor_lastNameFirst("von Last, Jr ,First"));

		assertTrue(AuthorList
			.fixAuthor_lastNameFirst("John von Neumann and John Smith and Black Brown, Peter") == AuthorList
			.fixAuthor_lastNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
				+ "John Smith and Black Brown, Peter"));

		// Test Abbreviation == false
		assertEquals("Smith, John", AuthorList.fixAuthor_lastNameFirst("John Smith", false));

		assertEquals("Smith, John and Black Brown, Peter", AuthorList.fixAuthor_lastNameFirst(
			"John Smith and Black Brown, Peter", false));

		assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
			.fixAuthor_lastNameFirst("John von Neumann and John Smith and Black Brown, Peter",
				false));

		assertEquals("von Last, Jr, First", AuthorList.fixAuthor_lastNameFirst(
			"von Last, Jr ,First", false));

		assertTrue(AuthorList.fixAuthor_lastNameFirst(
			"John von Neumann and John Smith and Black Brown, Peter", false) == AuthorList
			.fixAuthor_lastNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
				+ "John Smith and Black Brown, Peter", false));

		// Test Abbreviate == true
		assertEquals("Smith, J.", AuthorList.fixAuthor_lastNameFirst("John Smith", true));

		assertEquals("Smith, J. and Black Brown, P.", AuthorList.fixAuthor_lastNameFirst(
			"John Smith and Black Brown, Peter", true));

		assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.",
			AuthorList.fixAuthor_lastNameFirst(
				"John von Neumann and John Smith and Black Brown, Peter", true));

		assertEquals("von Last, Jr, F.", AuthorList.fixAuthor_lastNameFirst("von Last, Jr ,First",
			true));

		assertTrue(AuthorList.fixAuthor_lastNameFirst(
			"John von Neumann and John Smith and Black Brown, Peter", true) == AuthorList
			.fixAuthor_lastNameFirst("John von Neumann" + (0 == 1 ? "" : " and ")
				+ "John Smith and Black Brown, Peter", true));

	}

	public void testFixAuthor_lastNameOnlyCommas() {

		{ // No comma before and
			assertEquals("", AuthorList.fixAuthor_lastNameOnlyCommas("", false));
			assertEquals("Smith", AuthorList.fixAuthor_lastNameOnlyCommas("John Smith", false));
			assertEquals("Smith", AuthorList.fixAuthor_lastNameOnlyCommas("Smith, Jr, John", false));

			assertTrue(AuthorList.fixAuthor_lastNameOnlyCommas(
				"John von Neumann and John Smith and Black Brown, Peter", false) == AuthorList
				.fixAuthor_lastNameOnlyCommas("John von Neumann" + (0 == 1 ? "" : " and ")
					+ "John Smith and Black Brown, Peter", false));

			assertEquals("von Neumann, Smith and Black Brown", AuthorList
				.fixAuthor_lastNameOnlyCommas(
					"John von Neumann and John Smith and Black Brown, Peter", false));
		}
		{ // Oxford Comma
			assertEquals("", AuthorList.fixAuthor_lastNameOnlyCommas("", true));
			assertEquals("Smith", AuthorList.fixAuthor_lastNameOnlyCommas("John Smith", true));
			assertEquals("Smith", AuthorList.fixAuthor_lastNameOnlyCommas("Smith, Jr, John", true));

			assertTrue(AuthorList.fixAuthor_lastNameOnlyCommas(
				"John von Neumann and John Smith and Black Brown, Peter", true) == AuthorList
				.fixAuthor_lastNameOnlyCommas("John von Neumann" + (0 == 1 ? "" : " and ")
					+ "John Smith and Black Brown, Peter", true));

			assertEquals("von Neumann, Smith, and Black Brown", AuthorList
				.fixAuthor_lastNameOnlyCommas(
					"John von Neumann and John Smith and Black Brown, Peter", true));
		}
	}

	public void testFixAuthorForAlphabetization() {
		assertEquals("Smith, J.", AuthorList.fixAuthorForAlphabetization("John Smith"));
		assertEquals("Neumann, J.", AuthorList.fixAuthorForAlphabetization("John von Neumann"));
		assertEquals("Neumann, J.", AuthorList.fixAuthorForAlphabetization("J. von Neumann"));
		assertEquals(
			"Neumann, J. and Smith, J. and Black Brown, Jr., P.",
			AuthorList
				.fixAuthorForAlphabetization("John von Neumann and John Smith and de Black Brown, Jr., Peter"));
	}

	public static int size(String bibtex) {
		return (AuthorList.getAuthorList(bibtex)).size();
	}

	public void testSize() {

		assertEquals(0, size(""));
		assertEquals(1, size("Bar"));
		assertEquals(1, size("Foo Bar"));
		assertEquals(1, size("Foo von Bar"));
		assertEquals(1, size("von Bar, Foo"));
		assertEquals(1, size("Bar, Foo"));
		assertEquals(1, size("Bar, Jr., Foo"));
		assertEquals(1, size("Bar, Foo"));
		assertEquals(2, size("John Neumann and Foo Bar"));
		assertEquals(2, size("John von Neumann and Bar, Jr, Foo"));

		assertEquals(3, size("John von Neumann and John Smith and Black Brown, Peter"));

		String s = "John von Neumann";
		for (int i = 0; i < 25; i++) {
			assertEquals(i + 1, size(s));
			s += " and Albert Einstein";
		}
	}

	public void testGetAuthor() {

		try {
			AuthorList.getAuthorList("").getAuthor(0);
			fail();
		} catch (Exception e) {
		}

		AuthorList.Author author = AuthorList.getAuthorList("John Smith and von Neumann, Jr, John")
			.getAuthor(0);
		assertEquals("John", author.getFirst());
		assertEquals("J.", author.getFirstAbbr());
		assertEquals("John Smith", author.getFirstLast(false));
		assertEquals("J. Smith", author.getFirstLast(true));
		assertEquals(null, author.getJr());
		assertEquals("Smith", author.getLast());
		assertEquals("Smith, John", author.getLastFirst(false));
		assertEquals("Smith, J.", author.getLastFirst(true));
		assertEquals("Smith", author.getLastOnly());
		assertEquals("Smith, J.", author.getNameForAlphabetization());
		assertEquals(null, author.getVon());

		author = AuthorList.getAuthorList("Peter Black Brown").getAuthor(0);
		assertEquals("Peter Black", author.getFirst());
		assertEquals("P. B.", author.getFirstAbbr());
		assertEquals("Peter Black Brown", author.getFirstLast(false));
		assertEquals("P. B. Brown", author.getFirstLast(true));
		assertEquals(null, author.getJr());
		assertEquals(null, author.getVon());

		author = AuthorList.getAuthorList("John Smith and von Neumann, Jr, John").getAuthor(1);
		assertEquals("John", author.getFirst());
		assertEquals("J.", author.getFirstAbbr());
		assertEquals("John von Neumann, Jr", author.getFirstLast(false));
		assertEquals("J. von Neumann, Jr", author.getFirstLast(true));
		assertEquals("Jr", author.getJr());
		assertEquals("Neumann", author.getLast());
		assertEquals("von Neumann, Jr, John", author.getLastFirst(false));
		assertEquals("von Neumann, Jr, J.", author.getLastFirst(true));
		assertEquals("von Neumann", author.getLastOnly());
		assertEquals("Neumann, Jr, J.", author.getNameForAlphabetization());
		assertEquals("von", author.getVon());
	}

	public void testGetAuthorsNatbib() {
		assertEquals("", AuthorList.getAuthorList("").getAuthorsNatbib());
		assertEquals("Smith", AuthorList.getAuthorList("John Smith").getAuthorsNatbib());
		assertEquals("Smith and Black Brown", AuthorList.getAuthorList(
			"John Smith and Black Brown, Peter").getAuthorsNatbib());
		assertEquals("von Neumann et al.", AuthorList.getAuthorList(
			"John von Neumann and John Smith and Black Brown, Peter").getAuthorsNatbib());
		
		/*
		 * [ 1465610 ] (Double-)Names containing hyphen (-) not handled correctly
		 */
		assertEquals("Last-Name et al.", AuthorList.getAuthorList(
			"First Second Last-Name" + " and John Smith and Black Brown, Peter").getAuthorsNatbib());

		// Test caching
		AuthorList al = AuthorList
			.getAuthorList("John von Neumann and John Smith and Black Brown, Peter");
		assertTrue(al.getAuthorsNatbib() == al.getAuthorsNatbib());
	}

	public void testGetAuthorsLastOnly() {

		{ // No comma before and
			assertEquals("", AuthorList.getAuthorList("").getAuthorsLastOnly(false));
			assertEquals("Smith", AuthorList.getAuthorList("John Smith").getAuthorsLastOnly(false));
			assertEquals("Smith", AuthorList.getAuthorList("Smith, Jr, John").getAuthorsLastOnly(
				false));

			assertEquals("von Neumann, Smith and Black Brown", AuthorList.getAuthorList(
				"John von Neumann and John Smith and Black Brown, Peter").getAuthorsLastOnly(false));
		}
		{ // Oxford comma
			assertEquals("", AuthorList.getAuthorList("").getAuthorsLastOnly(true));
			assertEquals("Smith", AuthorList.getAuthorList("John Smith").getAuthorsLastOnly(true));
			assertEquals("Smith", AuthorList.getAuthorList("Smith, Jr, John").getAuthorsLastOnly(
				true));

			assertEquals("von Neumann, Smith, and Black Brown", AuthorList.getAuthorList(
				"John von Neumann and John Smith and Black Brown, Peter").getAuthorsLastOnly(true));
		}
	}

	public void testGetAuthorsLastFirst() {
		{ // No commas before and
			AuthorList al;

			al = AuthorList.getAuthorList("");
			assertEquals("", al.getAuthorsLastFirst(true, false));
			assertEquals("", al.getAuthorsLastFirst(false, false));

			al = AuthorList.getAuthorList("John Smith");
			assertEquals("Smith, John", al.getAuthorsLastFirst(false, false));
			assertEquals("Smith, J.", al.getAuthorsLastFirst(true, false));

			al = AuthorList.getAuthorList("John Smith and Black Brown, Peter");
			assertEquals("Smith, John and Black Brown, Peter", al.getAuthorsLastFirst(false, false));
			assertEquals("Smith, J. and Black Brown, P.", al.getAuthorsLastFirst(true, false));

			al = AuthorList.getAuthorList("John von Neumann and John Smith and Black Brown, Peter");
			// Method description is different than code -> additional comma
			// there
			assertEquals("von Neumann, John, Smith, John and Black Brown, Peter", al
				.getAuthorsLastFirst(false, false));
			assertEquals("von Neumann, J., Smith, J. and Black Brown, P.", al.getAuthorsLastFirst(
				true, false));

			al = AuthorList.getAuthorList("John Peter von Neumann");
			assertEquals("von Neumann, J. P.", al.getAuthorsLastFirst(true, false));
		}
		{ // Oxford comma
			AuthorList al;

			al = AuthorList.getAuthorList("");
			assertEquals("", al.getAuthorsLastFirst(true, true));
			assertEquals("", al.getAuthorsLastFirst(false, true));

			al = AuthorList.getAuthorList("John Smith");
			assertEquals("Smith, John", al.getAuthorsLastFirst(false, true));
			assertEquals("Smith, J.", al.getAuthorsLastFirst(true, true));

			al = AuthorList.getAuthorList("John Smith and Black Brown, Peter");
			assertEquals("Smith, John and Black Brown, Peter", al.getAuthorsLastFirst(false, true));
			assertEquals("Smith, J. and Black Brown, P.", al.getAuthorsLastFirst(true, true));

			al = AuthorList.getAuthorList("John von Neumann and John Smith and Black Brown, Peter");
			assertEquals("von Neumann, John, Smith, John, and Black Brown, Peter", al
				.getAuthorsLastFirst(false, true));
			assertEquals("von Neumann, J., Smith, J., and Black Brown, P.", al.getAuthorsLastFirst(
				true, true));

			al = AuthorList.getAuthorList("John Peter von Neumann");
			assertEquals("von Neumann, J. P.", al.getAuthorsLastFirst(true, true));
		}
	}

	public void testGetAuthorsLastFirstAnds() {
		assertEquals("Smith, John", AuthorList.getAuthorList("John Smith").getAuthorsLastFirstAnds(
			false));
		assertEquals("Smith, John and Black Brown, Peter", AuthorList.getAuthorList(
			"John Smith and Black Brown, Peter").getAuthorsLastFirstAnds(false));
		assertEquals("von Neumann, John and Smith, John and Black Brown, Peter", AuthorList
			.getAuthorList("John von Neumann and John Smith and Black Brown, Peter")
			.getAuthorsLastFirstAnds(false));
		assertEquals("von Last, Jr, First", AuthorList.getAuthorList("von Last, Jr ,First")
			.getAuthorsLastFirstAnds(false));

		assertEquals("Smith, J.", AuthorList.getAuthorList("John Smith").getAuthorsLastFirstAnds(
			true));
		assertEquals("Smith, J. and Black Brown, P.", AuthorList.getAuthorList(
			"John Smith and Black Brown, Peter").getAuthorsLastFirstAnds(true));
		assertEquals("von Neumann, J. and Smith, J. and Black Brown, P.", AuthorList.getAuthorList(
			"John von Neumann and John Smith and Black Brown, Peter").getAuthorsLastFirstAnds(true));
		assertEquals("von Last, Jr, F.", AuthorList.getAuthorList("von Last, Jr ,First")
			.getAuthorsLastFirstAnds(true));

	}

	public void testGetAuthorsFirstFirst() {

		AuthorList al;

		al = AuthorList.getAuthorList("");
		assertEquals("", al.getAuthorsFirstFirst(true, false));
		assertEquals("", al.getAuthorsFirstFirst(false, false));
		assertEquals("", al.getAuthorsFirstFirst(true, true));
		assertEquals("", al.getAuthorsFirstFirst(false, true));

		al = AuthorList.getAuthorList("John Smith");
		assertEquals("John Smith", al.getAuthorsFirstFirst(false, false));
		assertEquals("J. Smith", al.getAuthorsFirstFirst(true, false));
		assertEquals("John Smith", al.getAuthorsFirstFirst(false, true));
		assertEquals("J. Smith", al.getAuthorsFirstFirst(true, true));

		al = AuthorList.getAuthorList("John Smith and Black Brown, Peter");
		assertEquals("John Smith and Peter Black Brown", al.getAuthorsFirstFirst(false, false));
		assertEquals("J. Smith and P. Black Brown", al.getAuthorsFirstFirst(true, false));
		assertEquals("John Smith and Peter Black Brown", al.getAuthorsFirstFirst(false, true));
		assertEquals("J. Smith and P. Black Brown", al.getAuthorsFirstFirst(true, true));

		al = AuthorList.getAuthorList("John von Neumann and John Smith and Black Brown, Peter");
		assertEquals("John von Neumann, John Smith and Peter Black Brown", al.getAuthorsFirstFirst(
			false, false));
		assertEquals("J. von Neumann, J. Smith and P. Black Brown", al.getAuthorsFirstFirst(true,
			false));
		assertEquals("John von Neumann, John Smith, and Peter Black Brown", al
			.getAuthorsFirstFirst(false, true));
		assertEquals("J. von Neumann, J. Smith, and P. Black Brown", al.getAuthorsFirstFirst(true,
			true));

		al = AuthorList.getAuthorList("John Peter von Neumann");
		assertEquals("John Peter von Neumann", al.getAuthorsFirstFirst(false, false));
		assertEquals("John Peter von Neumann", al.getAuthorsFirstFirst(false, true));
		assertEquals("J. P. von Neumann", al.getAuthorsFirstFirst(true, false));
		assertEquals("J. P. von Neumann", al.getAuthorsFirstFirst(true, true));
	}

	public void testGetAuthorsFirstFirstAnds() {
		assertEquals("John Smith", AuthorList.getAuthorList("John Smith")
			.getAuthorsFirstFirstAnds());
		assertEquals("John Smith and Peter Black Brown", AuthorList.getAuthorList(
			"John Smith and Black Brown, Peter").getAuthorsFirstFirstAnds());
		assertEquals("John von Neumann and John Smith and Peter Black Brown", AuthorList
			.getAuthorList("John von Neumann and John Smith and Black Brown, Peter")
			.getAuthorsFirstFirstAnds());
		assertEquals("First von Last, Jr. III", AuthorList
			.getAuthorList("von Last, Jr. III, First").getAuthorsFirstFirstAnds());
	}

	public void testGetAuthorsForAlphabetization() {
		assertEquals("Smith, J.", AuthorList.getAuthorList("John Smith")
			.getAuthorsForAlphabetization());
		assertEquals("Neumann, J.", AuthorList.getAuthorList("John von Neumann")
			.getAuthorsForAlphabetization());
		assertEquals("Neumann, J.", AuthorList.getAuthorList("J. von Neumann")
			.getAuthorsForAlphabetization());
		assertEquals("Neumann, J. and Smith, J. and Black Brown, Jr., P.", AuthorList
			.getAuthorList("John von Neumann and John Smith and de Black Brown, Jr., Peter")
			.getAuthorsForAlphabetization());
	}

}
