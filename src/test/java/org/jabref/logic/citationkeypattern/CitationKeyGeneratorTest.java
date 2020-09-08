package org.jabref.logic.citationkeypattern;

import java.util.Collections;
import java.util.Optional;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.FileUpdateMonitor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CitationKeyGeneratorTest {

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1 = "Isaac Newton";
    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2 = "Isaac Newton and James Maxwell";
    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3 = "Isaac Newton and James Maxwell and Albert Einstein";

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1 = "Wil van der Aalst";
    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2 = "Wil van der Aalst and Tammo van Lessen";

    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1 = "I. Newton";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2 = "I. Newton and J. Maxwell";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3 = "I. Newton and J. Maxwell and A. Einstein";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4 = "I. Newton and J. Maxwell and A. Einstein and N. Bohr";
    private static final String AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5 = "I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown";

    private static final String TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH = "application migration effort in the cloud - the case of cloud platforms";
    private static final String TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON = "{BPEL} conformance in open source engines: the case of static analysis";
    private static final String TITLE_STRING_CASED = "Process Viewing Patterns";
    private static final String TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD = "BPMN Conformance in Open Source Engines";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING = "The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON = "Cloud Computing: The Next Revolution in IT";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD = "Towards Choreography-based Process Distribution in the Cloud";
    private static final String TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS = "On the Measurement of Design-Time Adaptability for Process-Based Systems ";

    private static ImportFormatPreferences importFormatPreferences;
    private final FileUpdateMonitor fileMonitor = new DummyFileUpdateMonitor();

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    static String generateKey(BibEntry entry, String pattern) {
        return generateKey(entry, pattern, new BibDatabase());
    }

    static String generateKey(BibEntry entry, String pattern, BibDatabase database) {
        GlobalCitationKeyPattern keyPattern = new GlobalCitationKeyPattern(Collections.emptyList());
        keyPattern.setDefaultValue("[" + pattern + "]");
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                ',');

        return new CitationKeyGenerator(keyPattern, database, patternPreferences).generateKey(entry);
    }

    @Test
    void testAndInAuthorName() throws ParseException {
        Optional<BibEntry> entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Holland",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testCrossrefAndInAuthorNames() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.setField(StandardField.AUTHOR, "Simon Holland");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("Holland",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "auth",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testAndAuthorNames() throws ParseException {
        String bibtexString = "@ARTICLE{whatevery, author={Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland}}";
        Optional<BibEntry> entry = BibtexParser.singleFromString(bibtexString, importFormatPreferences, fileMonitor);
        assertEquals("HerlandHaugeHelgeland",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "authors3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testCrossrefAndAuthorNames() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.setField(StandardField.AUTHOR, "Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("HerlandHaugeHelgeland",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "authors3",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testSpecialLatexCharacterInAuthorName() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={Simon Popovi\\v{c}ov\\'{a}}}", importFormatPreferences, fileMonitor);
        assertEquals("Popovicova",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    /**
     * Test for https://sourceforge.net/forum/message.php?msg_id=4498555 Test the Labelmaker and all kind of accents Á á
     * Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź
     */
    @Test
    void testMakeLabelAndCheckLegalKeys() throws ParseException {

        Optional<BibEntry> entry0 = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={Andreas Köning}, year={2000}}", importFormatPreferences, fileMonitor);
        assertEquals("Koe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Aoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Eoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Ioe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Loe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Noe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Ooe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Roe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Soe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Uoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Yoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Zoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    /**
     * Test the Labelmaker and with accent grave Chars to test: "ÀÈÌÒÙ";
     */
    @Test
    void testMakeLabelAndCheckLegalKeysAccentGrave() throws ParseException {
        Optional<BibEntry> entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Aoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Eoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Ioe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Ooe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Uoe",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Oraib Al-Ketan}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("AlK",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andrés D'Alessandro}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("DAl",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));

        entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Andrés Aʹrnold}, year={2000}}",
                importFormatPreferences, fileMonitor);
        assertEquals("Arn",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "auth3",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    /**
     * Tests if cleanKey replaces Non-ASCII chars. There are quite a few chars that should be replaced. Perhaps
     * there is a better method than the current.
     * @see CitationKeyGenerator#cleanKey(String, String)
     */
    @Test
    void testCheckLegalKey() {
        // not tested/ not in hashmap UNICODE_CHARS:
        // Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə Đ đ   Ů ů    Ǣ ǣ ǖ ǘ ǚ ǜ
        // " Ǣ ǣ ǖ ǘ ǚ ǜ   " +
        // "Đ đ   Ů ů  " +
        // "Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə
        String accents = "ÀàÈèÌìÒòÙù Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ";
        String expectedResult = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYy";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "ÄäËëÏïÖöÜüŸÿ";
        expectedResult = "AeaeEeIiOeoeUeueYy";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ";
        expectedResult = "CcGgKkLlNnRrSsTt";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ";
        expectedResult = "AaEeGgIiOoUu";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ċ ċ Ė ė Ġ ġ İ ı Ż ż";
        expectedResult = "CcEeGgIiZz";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ą ą Ę ę Į į Ǫ ǫ Ų ų";
        expectedResult = "AaEeIiOoUu"; // O or Q? o or q?
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ";
        expectedResult = "AaEeIiOoUuYy";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž";
        expectedResult = "AaCcDdEeIiLlNnOoRrSsTtUuZz";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        expectedResult = "AaEeIiNnOoUuYy";
        accents = "ÃãẼẽĨĩÑñÕõŨũỸỹ";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        accents = "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ";
        expectedResult = "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));

        String totest = "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    "
                + "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ"
                + " Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   " + "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ"
                + "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   " + "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   "
                + "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   ";
        String expectedResults = "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy"
                + "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt" + "AaCcDdEeIiLlNnOoRrSsTtUuZz" + "AaEeIiOoUuYy" + "AaEeGgIiOoUu"
                + "CcEeGgIiZzAaEeIiOoUu" + "DdHhLlLlMmNnRrRrSsTt";
        assertEquals(expectedResults, CitationKeyGenerator.cleanKey(totest, DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testFirstAuthor() {
        assertEquals("Newton", CitationKeyGenerator.firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));
        assertEquals("Newton", CitationKeyGenerator.firstAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));

        // https://sourceforge.net/forum/message.php?msg_id=4498555
        assertEquals("K{\\\"o}ning", CitationKeyGenerator.firstAuthor("K{\\\"o}ning"));

        assertEquals("", CitationKeyGenerator.firstAuthor(""));
    }

    @Test
    void testFirstAuthorNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.firstAuthor(null));
    }

    @Test
    void testUniversity() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University}}}", importFormatPreferences, fileMonitor);
        assertEquals("UniLinkoeping",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testcrossrefUniversity() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.setField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University}}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoeping",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "auth",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testDepartment() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, Department of Electrical Engineering}}}",
                importFormatPreferences, fileMonitor);
        assertEquals("UniLinkoepingEE",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testcrossrefDepartment() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.setField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University, Department of Electrical Engineering}}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoepingEE",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "auth",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testSchool() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, School of Computer Engineering}}}",
                importFormatPreferences, fileMonitor);
        assertEquals("UniLinkoepingCE",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void generateKeyAbbreviateCorporateAuthorDepartmentWithoutAcademicInstitute() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{null, author={{Department of Localhost NullGenerators}}}",
                importFormatPreferences, fileMonitor);
        assertEquals("DLN",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void generateKeyAbbreviateCorporateAuthorSchoolWithoutAcademicInstitute() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{null, author={{The School of Null}}}",
                importFormatPreferences, fileMonitor);
        assertEquals("SchoolNull",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testcrossrefSchool() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.setField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University, School of Computer Engineering}}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoepingCE",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "auth",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testInstituteOfTechnology() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Massachusetts Institute of Technology}}}", importFormatPreferences, fileMonitor);
        assertEquals("MIT",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "auth",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testcrossrefInstituteOfTechnology() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry();
        entry2.setCitationKey("entry2");
        entry2.setField(StandardField.AUTHOR, "{Massachusetts Institute of Technology}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("MIT",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "auth",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testAuthIniN() {
        assertEquals("NMEB", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, 4));
        assertEquals("NMEB", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 4));
        assertEquals("NeME", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 4));
        assertEquals("NeMa", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 4));
        assertEquals("Newt", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 4));
        assertEquals("", "");

        assertEquals("N", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        assertEquals("", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 0));
        assertEquals("", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, -1));

        assertEquals("Newton", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 6));
        assertEquals("Newton", CitationKeyGenerator.authIniN(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 7));
    }

    @Test
    void testAuthIniNNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.authIniN(null, 3));
    }

    @Test
    void testAuthIniNEmptyReturnsEmpty() {
        assertEquals("", CitationKeyGenerator.authIniN("", 1));
    }

    /**
     * Tests  [auth.auth.ea]
     */
    @Test
    void authAuthEa() {
        assertEquals("Newton", CitationKeyGenerator.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("Newton.Maxwell",
                CitationKeyGenerator.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
        assertEquals("Newton.Maxwell.ea",
                CitationKeyGenerator.authAuthEa(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3));
    }

    @Test
    void testAuthEaEmptyReturnsEmpty() {
        assertEquals("", CitationKeyGenerator.authAuthEa(""));
    }

    /**
     * Tests the [auth.etal] and [authEtAl] patterns
     */
    @Test
    void testAuthEtAl() {
        // tests taken from the comments

        // [auth.etal]
        String delim = ".";
        String append = ".etal";
        assertEquals("Newton.etal",
                CitationKeyGenerator.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        assertEquals("Newton.Maxwell",
                CitationKeyGenerator.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));

        // [authEtAl]
        delim = "";
        append = "EtAl";
        assertEquals("NewtonEtAl",
                CitationKeyGenerator.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, delim, append));
        assertEquals("NewtonMaxwell",
                CitationKeyGenerator.authEtal(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, delim, append));
    }

    /**
     * Test the [authshort] pattern
     */
    @Test
    void testAuthShort() {
        // tests taken from the comments
        assertEquals("NME+", CitationKeyGenerator.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("NME", CitationKeyGenerator.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("NM", CitationKeyGenerator.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("Newton", CitationKeyGenerator.authshort(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
    }

    @Test
    void testAuthShortEmptyReturnsEmpty() {
        assertEquals("", CitationKeyGenerator.authshort(""));
    }

    /**
     * Test the [authN_M] pattern
     */
    @Test
    void authNM() {
        assertEquals("N", CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1, 1));
        assertEquals("Max",
                CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3, 2));
        assertEquals("New",
                CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3, 1));
        assertEquals("Bo",
                CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 2, 4));
        assertEquals("Bohr",
                CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, 6, 4));

        assertEquals("Aal",
                CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, 3, 1));
        assertEquals("Less",
                CitationKeyGenerator.authNofMth(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, 4, 2));

        assertEquals("", CitationKeyGenerator.authNofMth("", 2, 4));
    }

    @Test
    void authNMThrowsNPE() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.authNofMth(null, 2, 4));
    }

    /**
     * Tests [authForeIni]
     */
    @Test
    void firstAuthorForenameInitials() {
        assertEquals("I", CitationKeyGenerator
                .firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("I", CitationKeyGenerator
                .firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("I",
                CitationKeyGenerator.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("I",
                CitationKeyGenerator.firstAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
    }

    /**
     * Tests [authFirstFull]
     */
    @Test
    void firstAuthorVonAndLast() {
        assertEquals("vanderAalst", CitationKeyGenerator
                .firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("vanderAalst", CitationKeyGenerator
                .firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    @Test
    void firstAuthorVonAndLastNoVonInName() {
        assertEquals("Newton",
                CitationKeyGenerator.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1));
        assertEquals("Newton",
                CitationKeyGenerator.firstAuthorVonAndLast(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2));
    }

    /**
     * Tests [authors]
     */
    @Test
    void testAllAuthors() {
        assertEquals("Newton", CitationKeyGenerator.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("NewtonMaxwell",
                CitationKeyGenerator.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("NewtonMaxwellEinstein",
                CitationKeyGenerator.allAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
    }

    /**
     * Tests [authorsAlpha]
     */
    @Test
    void authorsAlpha() {
        assertEquals("New", CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("NM", CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("NME", CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("NMEB", CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("NME+", CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("vdAal",
                CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("vdAvL",
                CitationKeyGenerator.authorsAlpha(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorLast]
     */
    @Test
    void lastAuthor() {
        assertEquals("Newton", CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("Maxwell", CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("Einstein",
                CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("Bohr", CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("Unknown", CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("Aalst",
                CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("Lessen",
                CitationKeyGenerator.lastAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorLastForeIni]
     */
    @Test
    void lastAuthorForenameInitials() {
        assertEquals("I",
                CitationKeyGenerator.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("J",
                CitationKeyGenerator.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("A",
                CitationKeyGenerator.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("N",
                CitationKeyGenerator.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("H",
                CitationKeyGenerator.lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("W", CitationKeyGenerator
                .lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("T", CitationKeyGenerator
                .lastAuthorForenameInitials(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests [authorIni]
     */
    @Test
    void oneAuthorPlusIni() {
        assertEquals("Newto",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1));
        assertEquals("NewtoM",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2));
        assertEquals("NewtoME",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3));
        assertEquals("NewtoMEB",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4));
        assertEquals("NewtoMEBU",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5));

        assertEquals("Aalst",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1));
        assertEquals("AalstL",
                CitationKeyGenerator.oneAuthorPlusIni(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors1]
     */
    @Test
    void testNAuthors1() {
        assertEquals("Newton", CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1));
        assertEquals("NewtonEtAl",
                CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 1));
        assertEquals("NewtonEtAl",
                CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 1));
        assertEquals("NewtonEtAl",
                CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 1));
    }

    @Test
    void testNAuthors1EmptyReturnEmpty() {
        assertEquals("", CitationKeyGenerator.nAuthors("", 1));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors3]
     */
    @Test
    void testNAuthors3() {
        assertEquals("Newton", CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 3));
        assertEquals("NewtonMaxwell",
                CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3));
        assertEquals("NewtonMaxwellEinstein",
                CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3));
        assertEquals("NewtonMaxwellEinsteinEtAl",
                CitationKeyGenerator.nAuthors(AUTHOR_STRING_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 3));
    }

    @Test
    void testFirstPage() {
        assertEquals("7", CitationKeyGenerator.firstPage("7--27"));
        assertEquals("27", CitationKeyGenerator.firstPage("--27"));
        assertEquals("", CitationKeyGenerator.firstPage(""));
        assertEquals("42", CitationKeyGenerator.firstPage("42--111"));
        assertEquals("7", CitationKeyGenerator.firstPage("7,41,73--97"));
        assertEquals("7", CitationKeyGenerator.firstPage("41,7,73--97"));
        assertEquals("43", CitationKeyGenerator.firstPage("43+"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testFirstPageNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.firstPage(null));
    }

    @Test
    void testPagePrefix() {
        assertEquals("L", CitationKeyGenerator.pagePrefix("L7--27"));
        assertEquals("L--", CitationKeyGenerator.pagePrefix("L--27"));
        assertEquals("L", CitationKeyGenerator.pagePrefix("L"));
        assertEquals("L", CitationKeyGenerator.pagePrefix("L42--111"));
        assertEquals("L", CitationKeyGenerator.pagePrefix("L7,L41,L73--97"));
        assertEquals("L", CitationKeyGenerator.pagePrefix("L41,L7,L73--97"));
        assertEquals("L", CitationKeyGenerator.pagePrefix("L43+"));
        assertEquals("", CitationKeyGenerator.pagePrefix("7--27"));
        assertEquals("--", CitationKeyGenerator.pagePrefix("--27"));
        assertEquals("", CitationKeyGenerator.pagePrefix(""));
        assertEquals("", CitationKeyGenerator.pagePrefix("42--111"));
        assertEquals("", CitationKeyGenerator.pagePrefix("7,41,73--97"));
        assertEquals("", CitationKeyGenerator.pagePrefix("41,7,73--97"));
        assertEquals("", CitationKeyGenerator.pagePrefix("43+"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testPagePrefixNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.pagePrefix(null));
    }

    @Test
    void testLastPage() {

        assertEquals("27", CitationKeyGenerator.lastPage("7--27"));
        assertEquals("27", CitationKeyGenerator.lastPage("--27"));
        assertEquals("", CitationKeyGenerator.lastPage(""));
        assertEquals("111", CitationKeyGenerator.lastPage("42--111"));
        assertEquals("97", CitationKeyGenerator.lastPage("7,41,73--97"));
        assertEquals("97", CitationKeyGenerator.lastPage("7,41,97--73"));
        assertEquals("43", CitationKeyGenerator.lastPage("43+"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void testLastPageNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.lastPage(null));
    }

    /**
     * Tests [veryShortTitle]
     */
    @Test
    void veryShortTitle() {
        // veryShortTitle is getTitleWords with "1" as count
        int count = 1;
        assertEquals("application",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH)));
        assertEquals("BPEL", CitationKeyGenerator.getTitleWords(count,
                CitationKeyGenerator.removeSmallWords(
                        TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON)));
        assertEquals("Process", CitationKeyGenerator.getTitleWords(count,
                CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED)));
        assertEquals("BPMN",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD)));
        assertEquals("Difference", CitationKeyGenerator.getTitleWords(count,
                CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING)));
        assertEquals("Cloud",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator
                                .removeSmallWords(TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON)));
        assertEquals("Towards",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD)));
        assertEquals("Measurement",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator
                                .removeSmallWords(TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS)));
    }

    /**
     * Tests [shortTitle]
     */
    @Test
    void shortTitle() {
        // shortTitle is getTitleWords with "3" as count and removed small words
        int count = 3;
        assertEquals("application migration effort",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH)));
        assertEquals("BPEL conformance open",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON)));
        assertEquals("Process Viewing Patterns",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED)));
        assertEquals("BPMN Conformance Open",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD)));
        assertEquals("Difference Graph Based",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING)));
        assertEquals("Cloud Computing: Next",
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON)));
        assertEquals("Towards Choreography based",
                CitationKeyGenerator.getTitleWords(count, CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD)));
        assertEquals("Measurement Design Time",
                CitationKeyGenerator.getTitleWords(count, CitationKeyGenerator.removeSmallWords(TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS)));
    }

    /**
     * Tests [camel]
     */
    @Test
    void camel() {
        // camel capitalises and concatenates all the words of the title
        assertEquals("ApplicationMigrationEffortInTheCloudTheCaseOfCloudPlatforms",
                CitationKeyGenerator.getCamelizedTitle(TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH));
        assertEquals("BPELConformanceInOpenSourceEnginesTheCaseOfStaticAnalysis",
                CitationKeyGenerator.getCamelizedTitle(
                        TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("ProcessViewingPatterns", CitationKeyGenerator.getCamelizedTitle(TITLE_STRING_CASED));
        assertEquals("BPMNConformanceInOpenSourceEngines",
                CitationKeyGenerator.getCamelizedTitle(TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD));
        assertEquals("TheDifferenceBetweenGraphBasedAndBlockStructuredBusinessProcessModellingLanguages",
                CitationKeyGenerator.getCamelizedTitle(
                        TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING));
        assertEquals("CloudComputingTheNextRevolutionInIT",
                CitationKeyGenerator.getCamelizedTitle(TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("TowardsChoreographyBasedProcessDistributionInTheCloud",
                CitationKeyGenerator.getCamelizedTitle(TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD));
        assertEquals("OnTheMeasurementOfDesignTimeAdaptabilityForProcessBasedSystems",
                CitationKeyGenerator.getCamelizedTitle(TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS));
    }

    /**
     * Tests [title]
     */
    @Test
    void title() {
        // title capitalises the significant words of the title
        // for the title case the concatenation happens at formatting, which is tested in MakeLabelWithDatabaseTest.java
        assertEquals("Application Migration Effort in the Cloud the Case of Cloud Platforms",
                CitationKeyGenerator
                        .camelizeSignificantWordsInTitle(TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH));
        assertEquals("BPEL Conformance in Open Source Engines: the Case of Static Analysis",
                CitationKeyGenerator.camelizeSignificantWordsInTitle(
                        TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("Process Viewing Patterns",
                CitationKeyGenerator.camelizeSignificantWordsInTitle(TITLE_STRING_CASED));
        assertEquals("BPMN Conformance in Open Source Engines",
                CitationKeyGenerator
                        .camelizeSignificantWordsInTitle(TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD));
        assertEquals("The Difference between Graph Based and Block Structured Business Process Modelling Languages",
                CitationKeyGenerator.camelizeSignificantWordsInTitle(
                        TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING));
        assertEquals("Cloud Computing: the Next Revolution in IT",
                CitationKeyGenerator.camelizeSignificantWordsInTitle(
                        TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON));
        assertEquals("Towards Choreography Based Process Distribution in the Cloud",
                CitationKeyGenerator
                        .camelizeSignificantWordsInTitle(TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD));
        assertEquals("On the Measurement of Design Time Adaptability for Process Based Systems",
                CitationKeyGenerator.camelizeSignificantWordsInTitle(
                        TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS));
    }

    @Test
    void keywordNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        assertEquals("w1", generateKey(entry, "keyword1"));

        // check keywords with space
        assertEquals("w2aw2b", generateKey(entry, "keyword2"));

        // check out of range
        assertEquals("", generateKey(entry, "keyword4"));
    }

    @Test
    void crossrefkeywordNKeywordsSeparatedBySpace() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        entry2.setCitationKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry1);
        entry2.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        assertEquals("w1", generateKey(entry1, "keyword1", database));
    }

    @Test
    void keywordsNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        // all keywords
        assertEquals("w1w2aw2bw3", generateKey(entry, "keywords"));

        // check keywords with space
        assertEquals("w1w2aw2b", generateKey(entry, "keywords2"));

        // check out of range
        assertEquals("w1w2aw2bw3", generateKey(entry, "keywords55"));
    }

    @Test
    void crossrefkeywordsNKeywordsSeparatedBySpace() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        entry2.setCitationKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry1);
        entry2.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        assertEquals("w1w2aw2bw3", generateKey(entry1, "keywords", database));
    }

    @Test
    void testCheckLegalKeyUnwantedCharacters() {
        assertEquals("AAAA", CitationKeyGenerator.cleanKey("AA AA", DEFAULT_UNWANTED_CHARACTERS));
        assertEquals("SPECIALCHARS", CitationKeyGenerator.cleanKey("SPECIAL CHARS#{\\\"}~,", DEFAULT_UNWANTED_CHARACTERS));
        assertEquals("", CitationKeyGenerator.cleanKey("\n\t\r", DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testCheckLegalKeyNoUnwantedCharacters() {
        assertEquals("AAAA", CitationKeyGenerator.cleanKey("AA AA", ""));
        assertEquals("SPECIALCHARS^", CitationKeyGenerator.cleanKey("SPECIAL CHARS#{\\\"}~,^", ""));
        assertEquals("", CitationKeyGenerator.cleanKey("\n\t\r", ""));
    }

    @Test
    void testCheckLegalNullInNullOut() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.cleanKey(null, DEFAULT_UNWANTED_CHARACTERS));
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.cleanKey(null, DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void testApplyModifiers() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "Green Scheduling of Whatever");
        assertEquals("GSo", generateKey(entry, "shorttitleINI"));
        assertEquals("GreenSchedulingWhatever", generateKey(entry, "shorttitle",
                new BibDatabase()));
    }

    @Test
    void testcrossrefShorttitle() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        entry2.setCitationKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry1);
        entry2.setField(StandardField.TITLE, "Green Scheduling of Whatever");

        assertEquals("GreenSchedulingWhatever", generateKey(entry1, "shorttitle",
                database));
    }

    @Test
    void testcrossrefShorttitleInitials() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry();
        BibEntry entry2 = new BibEntry();
        entry1.setField(StandardField.CROSSREF, "entry2");
        entry2.setCitationKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry1);
        entry2.setField(StandardField.TITLE, "Green Scheduling of Whatever");

        assertEquals("GSo", generateKey(entry1, "shorttitleINI", database));
    }

    @Test
    void generateKeyStripsColonFromTitle() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "Green Scheduling of: Whatever");
        assertEquals("GreenSchedulingOfWhatever", generateKey(entry, "title"));
    }

    @Test
    void generateKeyStripsApostropheFromTitle() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "Green Scheduling of `Whatever`");
        assertEquals("GreenSchedulingofWhatever", generateKey(entry, "title"));
    }

    @Test
    void generateKeyWithOneModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "The Interesting Title");
        assertEquals("theinterestingtitle", generateKey(entry, "title:lower"));
    }

    @Test
    void generateKeyWithTwoModifiers() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "The Interesting Title");
        assertEquals("theinterestingtitle", generateKey(entry, "title:lower:(_)"));
    }

    @Test
    void generateKeyWithTitleCapitalizeModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("TheInterestingTitleLongerThanThreeWords", generateKey(entry, "title:capitalize"));
    }

    @Test
    void generateKeyWithShortTitleCapitalizeModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("InterestingTitleLonger", generateKey(entry, "shorttitle:capitalize"));
    }

    @Test
    void generateKeyWithTitleTitleCaseModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "A title WITH some of The key words");
        assertEquals("ATitlewithSomeoftheKeyWords", generateKey(entry, "title:titlecase"));
    }

    @Test
    void generateKeyWithShortTitleTitleCaseModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("InterestingTitleLonger", generateKey(entry, "shorttitle:titlecase"));
    }

    @Test
    void generateKeyWithTitleSentenceCaseModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, "A title WITH some of The key words");
        assertEquals("Atitlewithsomeofthekeywords", generateKey(entry, "title:sentencecase"));
    }

    @Test
    void generateKeyWithAuthUpperYearShortTitleCapitalizeModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1);
        entry.setField(StandardField.YEAR, "2019");
        entry.setField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("NEWTON2019InterestingTitleLonger", generateKey(entry, "[auth:upper][year][shorttitle:capitalize]"));
    }

    @Test
    void generateKeyWithYearAuthUpperTitleSentenceCaseModifier() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3);
        entry.setField(StandardField.YEAR, "2019");
        entry.setField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("NewtonMaxwellEtAl_2019_TheInterestingTitleLongerThanThreeWords", generateKey(entry, "[authors2]_[year]_[title:capitalize]"));
    }

    @Test
    void generateKeyWithMinusInCitationStyleOutsideAField() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1);
        entry.setField(StandardField.YEAR, "2019");

        assertEquals("Newton-2019", generateKey(entry, "[auth]-[year]"));
    }

    @Test
    void generateKeyWithWithFirstNCharacters() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.AUTHOR, "Newton, Isaac");
        entry.setField(StandardField.YEAR, "2019");

        assertEquals("newt-2019", generateKey(entry, "[auth4:lower]-[year]"));
    }
}
