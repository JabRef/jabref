package org.jabref.logic.citationkeypattern;

import java.util.Optional;
import java.util.stream.Stream;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;

import static org.jabref.logic.citationkeypattern.CitationKeyGenerator.DEFAULT_UNWANTED_CHARACTERS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * Tests whole citation key patterns such as <code>[authorsAlpha][year]</code>.
 * The concrete patterns such as <code>authorsAlpha</code> should better be tested at {@link BracketedPatternTest}.
 * <p>
 * Concurrent execution leads to issues on GitHub actions.
 */
class CitationKeyGeneratorTest {

    private static final BibEntry AUTHOR_EMPTY = createABibEntryAuthor("");

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1 = "Isaac Newton";
    private static final BibEntry AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1 = createABibEntryAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1);

    private static final BibEntry AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2 = createABibEntryAuthor("Isaac Newton and James Maxwell");

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3 = "Isaac Newton and James Maxwell and Albert Einstein";
    private static final BibEntry AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3 = createABibEntryAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3);

    private static final String AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_AND_OTHERS_COUNT_3 = "Isaac Newton and James Maxwell and others";
    private static final BibEntry AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_AND_OTHERS_COUNT_3 = createABibEntryAuthor(AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_AND_OTHERS_COUNT_3);

    private static final BibEntry AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1 = createABibEntryAuthor("Wil van der Aalst");
    private static final BibEntry AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2 = createABibEntryAuthor("Wil van der Aalst and Tammo van Lessen");

    private static final BibEntry AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1 = createABibEntryAuthor("I. Newton");
    private static final BibEntry AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2 = createABibEntryAuthor("I. Newton and J. Maxwell");
    private static final BibEntry AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3 = createABibEntryAuthor("I. Newton and J. Maxwell and A. Einstein");
    private static final BibEntry AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4 = createABibEntryAuthor("I. Newton and J. Maxwell and A. Einstein and N. Bohr");
    private static final BibEntry AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5 = createABibEntryAuthor("I. Newton and J. Maxwell and A. Einstein and N. Bohr and Harry Unknown");

    private static final String TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH = "application migration effort in the cloud - the case of cloud platforms";
    private static final String TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON = "{BPEL} conformance in open source engines: the case of static analysis";
    private static final String TITLE_STRING_CASED = "Process Viewing Patterns";
    private static final String TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD = "BPMN Conformance in Open Source Engines";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING = "The Difference Between Graph-Based and Block-Structured Business Process Modelling Languages";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON = "Cloud Computing: The Next Revolution in IT";
    private static final String TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD = "Towards Choreography-based Process Distribution in the Cloud";
    private static final String TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS = "On the Measurement of Design-Time Adaptability for Process-Based Systems ";

    private static final String AUTHSHORT = "[authshort]";
    private static final String AUTHNOFMTH = "[auth%d_%d]";
    private static final String AUTHFOREINI = "[authForeIni]";
    private static final String AUTHFIRSTFULL = "[authFirstFull]";
    private static final String AUTHORLAST = "[authorLast]";
    private static final String AUTHORLASTFOREINI = "[authorLastForeIni]";
    private static final String AUTHORINI = "[authorIni]";
    private static final String AUTHORN = "[authors%d]";
    private static final String AUTHETAL = "[authEtAl]";
    private static final String AUTH_ETAL = "[auth.etal]";
    private static final String AUTHAUTHEA = "[auth.auth.ea]";

    private static ImportFormatPreferences importFormatPreferences;

    @BeforeEach
    void setUp() {
        importFormatPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
    }

    private static BibEntry createABibEntryAuthor(String author) {
        return new BibEntry().withField(StandardField.AUTHOR, author);
    }

    static String generateKey(BibEntry entry, String pattern) {
        return generateKey(entry, true, pattern, new BibDatabase());
    }

    static String generateKey(BibEntry entry, boolean transliterate, String pattern) {
        return generateKey(entry, transliterate, pattern, new BibDatabase());
    }

    static String generateKey(BibEntry entry, String pattern, BibDatabase database) {
        return generateKey(entry, true, pattern, database);
    }

    static String generateKey(BibEntry entry, boolean transliterate, String pattern, BibDatabase database) {
        GlobalCitationKeyPatterns keyPattern = GlobalCitationKeyPatterns.fromPattern(pattern);
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(transliterate, false, false, false, CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A, "", "", DEFAULT_UNWANTED_CHARACTERS, keyPattern, "", ',');

        return new CitationKeyGenerator(keyPattern, database, patternPreferences).generateKey(entry);
    }

    @Test
    void andInAuthorName() throws ParseException {
        Optional<BibEntry> entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}", importFormatPreferences);
        assertEquals("Holland", CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefAndInAuthorNames() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.AUTHOR, "Simon Holland");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("Holland", CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]", database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void andAuthorNames() throws ParseException {
        String bibtexString = "@ARTICLE{whatevery, author={Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland}}";
        Optional<BibEntry> entry = BibtexParser.singleFromString(bibtexString, importFormatPreferences);
        assertEquals("HerlandHaugeHelgeland", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[authors3]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefAndAuthorNames() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.AUTHOR, "Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("HerlandHaugeHelgeland", CitationKeyGenerator.cleanKey(generateKey(entry1, "[authors3]", database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void specialLatexCharacterInAuthorName() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Popovi\\v{c}ov\\'{a}}}", importFormatPreferences);
        assertEquals("Popovicova", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @ParameterizedTest(name = "expectedResult={0}, bibtexString={1}")
    @CsvSource(quoteCharacter = '"', textBlock = """
            # see https://sourceforge.net/forum/message.php?msg_id=4498555
            "Koe", "@ARTICLE{kohn, author={Andreas Köning}, year={2000}}",

            # Accent ague: Á á Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź
            "Aoe", "@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}",
            "Eoe", "@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}",
            "Ioe", "@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}",
            "Loe", "@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}",
            "Noe", "@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}",
            "Ooe", "@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}",
            "Roe", "@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}",
            "Soe", "@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}",
            "Uoe", "@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}",
            "Yoe", "@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}",
            "Zoe", "@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}",

            # Accent grave: À È Ì Ò Ù
            "Aoe", "@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}",
            "Eoe", "@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}",
            "Ioe", "@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}",
            "Ooe", "@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}",
            "Uoe", "@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}",

            # Special cases
            # We keep "-" in citation keys, thus Al-Ketan with three letters is "Al-"
            "Al-", "@ARTICLE{kohn, author={Oraib Al-Ketan}, year={2000}}",
            "DAl", "@ARTICLE{kohn, author={Andrés D'Alessandro}, year={2000}}",
            "Arn", "@ARTICLE{kohn, author={Andrés Aʹrnold}, year={2000}}"
            """)
    void makeLabelAndCheckLegalKeys(String expectedResult, String bibtexString) throws ParseException {
        BibEntry bibEntry = BibtexParser.singleFromString(bibtexString, importFormatPreferences).get();
        String citationKey = generateKey(bibEntry, "[auth3]", new BibDatabase());

        String cleanedKey = CitationKeyGenerator.cleanKey(citationKey, DEFAULT_UNWANTED_CHARACTERS);

        assertEquals(expectedResult, cleanedKey);
    }

    private static Stream<Arguments> firstAuthor() {
        return Stream.of(
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5),
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("Koening", createABibEntryAuthor("K{\\\"o}ning")),
                Arguments.of("", createABibEntryAuthor(""))
        );
    }

    @ParameterizedTest
    @MethodSource("firstAuthor")
    void firstAuthor(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, "[auth]"));
    }

    @Test
    void university() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={{Link{\\\"{o}}ping University}}}", importFormatPreferences);
        assertEquals("UniLinkoeping", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    /**
     * Tests if cleanKey replaces Non-ASCII chars. There are quite a few chars that should be replaced. Perhaps there is
     * a better method than the current.
     * <p>
     * not tested/ not in hashmap UNICODE_CHARS:
     * {@code
     * Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə Đ đ   Ů ů    Ǣ ǣ ǖ ǘ ǚ ǜ
     * Ǣ ǣ ǖ ǘ ǚ ǜ
     * Đ đ   Ů ů
     * Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə
     * }
     *
     * @see CitationKeyGenerator#cleanKey(String, String)
     */
    @ParameterizedTest(name = "expectedResult={0}, accents={1}")
    @CsvSource(quoteCharacter = '"', textBlock = """
            "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYy", "ÀàÈèÌìÒòÙù Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ",
            "AeaeEeIiOeoeUeueYy", "ÄäËëÏïÖöÜüŸÿ",
            "AaaaUu", "ÅåŮů",
            "CcGgKkLlNnRrSsTt", "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ",
            "AaEeGgIiOoUu", "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ",
            "CcEeGgIiZz", "Ċ ċ Ė ė Ġ ġ İ ı Ż ż",
            "AaEeIiOoUu", "Ą ą Ę ę Į į Ǫ ǫ Ų ų", # O or Q? o or q?
            "AaEeIiOoUuYy", "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ",
            "AaCcDdEeIiLlNnOoRrSsTtUuZz", "Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž",
            "AaEeIiNnOoUuYy", "ÃãẼẽĨĩÑñÕõŨũỸỹ",
            "DdHhLlLlMmNnRrRrSsTt", "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ",
            "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy", "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    ",
            "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt", "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ",
            "AaCcDdEeIiLlNnOoRrSsTtUuZz", " Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   ",
            "AaEeIiOoUuYy", "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ",
            "AaEeGgIiOoUu", "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   ",
            "AaEeIiOoUuYy", "Ả ả Ẻ ẻ Ỉ ỉ Ỏ ỏ Ủ ủ Ỷ ỷ",
            "EeIiUu", "Ḛ ḛ Ḭ ḭ Ṵ ṵ",
            "CcEeGgIiZzAaEeIiOoUu", "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   ",
            "DdHhLlLlMmNnRrRrSsTt", "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   "
            """)
    void checkLegalKey(String expectedResult, String accents) {
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefUniversity() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoeping", CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]", database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void department() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, Department of Electrical Engineering}}}", importFormatPreferences);
        assertEquals("UniLinkoepingEE", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefDepartment() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University, Department of Electrical Engineering}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoepingEE", CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]", database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void school() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, School of Computer Engineering}}}", importFormatPreferences);
        assertEquals("UniLinkoepingCE", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void generateKeyAbbreviateCorporateAuthorDepartmentWithoutAcademicInstitute() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{null, author={{Department of Localhost NullGenerators}}}", importFormatPreferences);
        assertEquals("DLN", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void generateKeyAbbreviateCorporateAuthorSchoolWithoutAcademicInstitute() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{null, author={{The School of Null}}}", importFormatPreferences);
        assertEquals("SchoolNull", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefSchool() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University, School of Computer Engineering}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoepingCE", CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]", database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void instituteOfTechnology() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString("@ARTICLE{kohn, author={{Massachusetts Institute of Technology}}}", importFormatPreferences);
        assertEquals("MIT", CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]", new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefInstituteOfTechnology() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.AUTHOR, "{Massachusetts Institute of Technology}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("MIT", CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]", database), DEFAULT_UNWANTED_CHARACTERS));
    }

    private static Stream<Arguments> authIniN() {
        return Stream.of(
                Arguments.of("", AUTHOR_EMPTY, "[authIni4]"),
                Arguments.of("Newt", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni4]"),
                Arguments.of("NeMa", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, "[authIni4]"),
                Arguments.of("NeME", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, "[authIni4]"),
                Arguments.of("NMEB", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, "[authIni4]"),
                Arguments.of("NMEB", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, "[authIni4]"),
                Arguments.of("N", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni1]"),
                Arguments.of("", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni0]"),
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni6]"),
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni7]")
        );
    }

    @ParameterizedTest
    @MethodSource("authIniN")
    void authIniN(String expected, BibEntry entry, String pattern) {
        assertEquals(expected, generateKey(entry, pattern));
    }

    @Test
    void authIniNEmptyReturnsEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, "[authIni1]"));
    }

    static Stream<Arguments> authAuthEa() {
        return Stream.of(
                Arguments.of("Newton", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1),
                Arguments.of("Newton.Maxwell", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2),
                Arguments.of("Newton.Maxwell.ea", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3)
        );
    }

    /**
     * Tests  [auth.auth.ea]
     */
    @ParameterizedTest
    @MethodSource("authAuthEa")
    void authAuthEa(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHAUTHEA));
    }

    @Test
    void authEaEmptyReturnsEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, AUTHAUTHEA));
    }

    static Stream<Arguments> authEtAl() {
        return Stream.of(
                Arguments.of("Newton.etal", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, AUTH_ETAL),
                Arguments.of("Newton.Maxwell", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTH_ETAL),
                Arguments.of("NewtonEtAl", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, AUTHETAL),
                Arguments.of("NewtonMaxwell", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTHETAL)
        );
    }

    /**
     * Tests the [auth.etal] and [authEtAl] patterns
     */
    @ParameterizedTest
    @MethodSource("authEtAl")
    void authEtAl(String expected, BibEntry entry, String pattern) {
        assertEquals(expected, generateKey(entry, pattern));
    }

    static Stream<Arguments> authShort() {
        return Stream.of(
                Arguments.of("NME+", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4),
                Arguments.of("NME", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3),
                Arguments.of("NM", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1)
        );
    }

    /**
     * Test the [authshort] pattern
     */
    @ParameterizedTest
    @MethodSource("authShort")
    void authShort(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHSHORT));
    }

    @Test
    void authShortEmptyReturnsEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, AUTHSHORT));
    }

    static Stream<Arguments> authNM() {
        return Stream.of(
                Arguments.of("N", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, 1, 1),
                Arguments.of("Max", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, 3, 2),
                Arguments.of("New", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, 3, 1),
                Arguments.of("Bo", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, 2, 4),
                Arguments.of("Bohr", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, 6, 4),
                Arguments.of("Aal", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, 3, 1),
                Arguments.of("Less", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, 4, 2),
                Arguments.of("", AUTHOR_EMPTY, 2, 4)
        );
    }

    /**
     * Test the [authN_M] pattern
     */
    @ParameterizedTest
    @MethodSource("authNM")
    void authNM(String expected, BibEntry entry, int n, int m) {
        String pattern = AUTHNOFMTH.formatted(n, m);
        assertEquals(expected, generateKey(entry, pattern));
    }

    static Stream<Arguments> firstAuthorForenameInitials() {
        return Stream.of(
                Arguments.of("I", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("I", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("I", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1),
                Arguments.of("I", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2)
        );
    }

    /**
     * Tests [authForeIni]
     */
    @ParameterizedTest
    @MethodSource("firstAuthorForenameInitials")
    void firstAuthorForenameInitials(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHFOREINI));
    }

    static Stream<Arguments> firstAuthorVonAndLast() {
        return Stream.of(
                Arguments.of("vanderAalst", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1),
                Arguments.of("vanderAalst", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2)
        );
    }

    /**
     * Tests [authFirstFull]
     */
    @ParameterizedTest
    @MethodSource("firstAuthorVonAndLast")
    void firstAuthorVonAndLast(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHFIRSTFULL));
    }

    static Stream<Arguments> firstAuthorVonAndLastNoVonInName() {
        return Stream.of(
                Arguments.of("Newton", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1),
                Arguments.of("Newton", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2)
        );
    }

    @ParameterizedTest
    @MethodSource("firstAuthorVonAndLastNoVonInName")
    void firstAuthorVonAndLastNoVonInName(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHFIRSTFULL));
    }

    @ParameterizedTest
    @MethodSource
    void authors(String expectedKey, BibEntry entry, String pattern) {
        assertEquals(expectedKey, generateKey(entry, pattern));
    }

    static Stream<Arguments> authors() {
        return Stream.of(
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authors]"),
                Arguments.of("NewtonMaxwell", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, "[authors]"),
                Arguments.of("NewtonMaxwellEinstein", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, "[authors]"),
                Arguments.of("Newton-Maxwell-Einstein", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, "[authors:regex(\"(.)([A-Z])\",\"$1-$2\")]")
        );
    }

    static Stream<Arguments> lastAuthor() {
        return Stream.of(
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("Maxwell", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("Einstein", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3),
                Arguments.of("Bohr", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4),
                Arguments.of("Unknown", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5),
                Arguments.of("Aalst", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1),
                Arguments.of("Lessen", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2)
        );
    }

    /**
     * Tests [authorLast]
     */
    @ParameterizedTest
    @MethodSource("lastAuthor")
    void lastAuthor(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHORLAST));
    }

    static Stream<Arguments> lastAuthorForenameInitials() {
        return Stream.of(
                Arguments.of("I", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("J", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("A", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3),
                Arguments.of("N", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4),
                Arguments.of("H", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5),
                Arguments.of("W", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1),
                Arguments.of("T", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2)
        );
    }

    /**
     * Tests [authorLastForeIni]
     */
    @ParameterizedTest
    @MethodSource("lastAuthorForenameInitials")
    void lastAuthorForenameInitials(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHORLASTFOREINI));
    }

    /**
     * Tests [authorIni]
     */
    @ParameterizedTest
    @MethodSource("oneAuthorPlusIniData")
    void oneAuthorPlusIni(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHORINI));
    }

    static Stream<Arguments> oneAuthorPlusIniData() {
        return Stream.of(
                Arguments.of("Newto", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("NewtoM", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("NewtoME", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3),
                Arguments.of("NewtoMEB", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4),
                Arguments.of("NewtoMEBU", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5),
                Arguments.of("Aalst", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1),
                Arguments.of("AalstL", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2)
        );
    }

    /**
     * Tests the [authorsN] pattern. -> [authors1]
     */
    @ParameterizedTest
    @MethodSource("nAuthors1Data")
    void nAuthors1(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHORN.formatted(1)));
    }

    static Stream<Arguments> nAuthors1Data() {
        return Stream.of(
                Arguments.of("", AUTHOR_EMPTY),
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("NewtonEtAl", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("NewtonEtAl", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3),
                Arguments.of("NewtonEtAl", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4)
        );
    }

    /**
     * Tests the [authorsN] pattern. -> [authors3]
     */
    @ParameterizedTest
    @MethodSource("nAuthors3Data")
    void nAuthors3(String expected, BibEntry entry) {
        assertEquals(expected, generateKey(entry, AUTHORN.formatted(3)));
    }

    static Stream<Arguments> nAuthors3Data() {
        return Stream.of(
                Arguments.of("Newton", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1),
                Arguments.of("NewtonMaxwell", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2),
                Arguments.of("NewtonMaxwellEinstein", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3),
                Arguments.of("NewtonMaxwellEinsteinEtAl", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4)
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        7,7--27
        27,--27
        '',''
        42,42--111
        7,'7,41,73--97'
        7,'41,7,73--97'
        43,43+
    """)
    void firstPage(String expected, String input) {
        assertEquals(expected, CitationKeyGenerator.firstPage(input));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void firstPageNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.firstPage(null));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        L,L7--27
        L--,L--27
        L,L
        L,L42--111
        L,'L7,L41,L73--97'
        L,'L41,L7,L73--97'
        L,L43+
        '',7--27
        --,--27
        '',''
        '',42--111
        '','7,41,73--97'
        '','41,7,73--97'
        '',43+
    """)
    void pagePrefix(String expected, String input) {
        assertEquals(expected, CitationKeyGenerator.pagePrefix(input));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void pagePrefixNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.pagePrefix(null));
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
            27,7--27
            27,--27
            '',''
            111,42--111
            97,'7,41,73--97'
            97,'7,41,97--73'
            43,43+
            0,00--0
            1,1--1
            """)
    void lastPage(String expected, String input) {
        assertEquals(expected, CitationKeyGenerator.lastPage(input));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void lastPageNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.lastPage(null));
    }

    /**
     * Tests [veryShortTitle]
     */
    @ParameterizedTest
    @MethodSource("veryShortTitleData")
    void veryShortTitle(String expected, String titleString) {
        int count = 1;
        assertEquals(expected,
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(titleString)));
    }

    static Stream<Arguments> veryShortTitleData() {
        return Stream.of(
                Arguments.of("application", TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH),
                Arguments.of("BPEL", TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("Process", TITLE_STRING_CASED),
                Arguments.of("BPMN", TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD),
                Arguments.of("Difference", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING),
                Arguments.of("Cloud", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("Towards", TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD),
                Arguments.of("Measurement", TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS)
        );
    }

    /**
     * Tests [shortTitle]
     */
    @ParameterizedTest
    @MethodSource("shortTitleData")
    void shortTitle(String expected, String titleString) {
        int count = 3;
        assertEquals(expected,
                CitationKeyGenerator.getTitleWords(count,
                        CitationKeyGenerator.removeSmallWords(titleString)));
    }

    static Stream<Arguments> shortTitleData() {
        return Stream.of(
                Arguments.of("application migration effort", TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH),
                Arguments.of("BPEL conformance open", TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("Process Viewing Patterns", TITLE_STRING_CASED),
                Arguments.of("BPMN Conformance Open", TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD),
                Arguments.of("Difference Graph Based", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING),
                Arguments.of("Cloud Computing: Next", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("Towards Choreography based", TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD),
                Arguments.of("Measurement Design Time", TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS)
        );
    }

    /**
     * Tests [camel]
     */
    @ParameterizedTest
    @MethodSource("camelData")
    void camel(String expected, String titleString) {
        assertEquals(expected, CitationKeyGenerator.getCamelizedTitle(titleString));
    }

    static Stream<Arguments> camelData() {
        return Stream.of(
                Arguments.of("ApplicationMigrationEffortInTheCloudTheCaseOfCloudPlatforms", TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH),
                Arguments.of("BPELConformanceInOpenSourceEnginesTheCaseOfStaticAnalysis", TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("ProcessViewingPatterns", TITLE_STRING_CASED),
                Arguments.of("BPMNConformanceInOpenSourceEngines", TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD),
                Arguments.of("TheDifferenceBetweenGraphBasedAndBlockStructuredBusinessProcessModellingLanguages", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING),
                Arguments.of("CloudComputingTheNextRevolutionInIT", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("TowardsChoreographyBasedProcessDistributionInTheCloud", TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD),
                Arguments.of("OnTheMeasurementOfDesignTimeAdaptabilityForProcessBasedSystems", TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS)
        );
    }

    /**
     * Tests [title]
     */
    @ParameterizedTest
    @MethodSource("titleData")
    void title(String expected, String titleString) {
        assertEquals(expected, CitationKeyGenerator.camelizeSignificantWordsInTitle(titleString));
    }

    static Stream<Arguments> titleData() {
        return Stream.of(
                Arguments.of("Application Migration Effort in the Cloud the Case of Cloud Platforms", TITLE_STRING_ALL_LOWER_FOUR_SMALL_WORDS_ONE_EN_DASH),
                Arguments.of("BPEL Conformance in Open Source Engines: the Case of Static Analysis", TITLE_STRING_ALL_LOWER_FIRST_WORD_IN_BRACKETS_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("Process Viewing Patterns", TITLE_STRING_CASED),
                Arguments.of("BPMN Conformance in Open Source Engines", TITLE_STRING_CASED_ONE_UPPER_WORD_ONE_SMALL_WORD),
                Arguments.of("The Difference between Graph Based and Block Structured Business Process Modelling Languages", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AT_THE_BEGINNING),
                Arguments.of("Cloud Computing: the Next Revolution in IT", TITLE_STRING_CASED_TWO_SMALL_WORDS_SMALL_WORD_AFTER_COLON),
                Arguments.of("Towards Choreography Based Process Distribution in the Cloud", TITLE_STRING_CASED_TWO_SMALL_WORDS_ONE_CONNECTED_WORD),
                Arguments.of("On the Measurement of Design Time Adaptability for Process Based Systems", TITLE_STRING_CASED_FOUR_SMALL_WORDS_TWO_CONNECTED_WORDS)
        );
    }

    @ParameterizedTest
    @CsvSource(textBlock = """
        w1,[keyword1]
        w2aw2b,[keyword2]
        '',[keyword4]
    """)
    void keywordNKeywordsSeparatedBySpace(String expected, String pattern) {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        assertEquals(expected, generateKey(entry, pattern));
    }

    @Test
    void crossrefkeywordNKeywordsSeparatedBySpace() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry1);
        entry2.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        assertEquals("w1", generateKey(entry1, "[keyword1]", database));
    }

    @ParameterizedTest
    @MethodSource("keywordsNData")
    void keywordsNKeywordsSeparatedBySpace(String expected, String pattern) {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        assertEquals(expected, generateKey(entry, pattern));
    }

    static Stream<Arguments> keywordsNData() {
        return Stream.of(
                Arguments.of("w1w2aw2bw3", "[keywords]"),
                Arguments.of("w1w2aw2b", "[keywords2]"),
                Arguments.of("w1w2aw2bw3", "[keywords55]")
        );
    }

    @Test
    void crossrefkeywordsNKeywordsSeparatedBySpace() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        database.insertEntry(entry2);
        database.insertEntry(entry1);

        assertEquals("w1w2aw2bw3", generateKey(entry1, "[keywords]", database));
    }

    @ParameterizedTest
    @MethodSource("checkLegalKeyUnwantedCharactersData")
    void checkLegalKeyUnwantedCharacters(String expected, String input) {
        assertEquals(expected, CitationKeyGenerator.cleanKey(input, DEFAULT_UNWANTED_CHARACTERS));
    }

    static Stream<Arguments> checkLegalKeyUnwantedCharactersData() {
        return Stream.of(
                Arguments.of("AAAA", "AA AA"),
                Arguments.of("SPECIALCHARS", "SPECIAL CHARS#{\\\"}~,"),
                Arguments.of("", "\n\t\r")
        );
    }

    @ParameterizedTest
    @MethodSource("checkLegalKeyNoUnwantedCharactersData")
    void checkLegalKeyNoUnwantedCharacters(String expected, String input) {
        assertEquals(expected, CitationKeyGenerator.cleanKey(input, ""));
    }

    static Stream<Arguments> checkLegalKeyNoUnwantedCharactersData() {
        return Stream.of(
                Arguments.of("AAAA", "AA AA"),
                Arguments.of("SPECIALCHARS^", "SPECIAL CHARS#{\\\"}~,^"),
                Arguments.of("", "\n\t\r")
        );
    }

    @Test
    void checkLegalNullInNullOut() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.cleanKey(null, DEFAULT_UNWANTED_CHARACTERS));
    }

    @ParameterizedTest
    @MethodSource("applyModifiersData")
    void applyModifiers(String expected, String pattern) {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Green Scheduling of Whatever");
        assertEquals(expected, generateKey(entry, pattern, new BibDatabase()));
    }

    static Stream<Arguments> applyModifiersData() {
        return Stream.of(
                Arguments.of("GSo", "[shorttitleINI]"),
                Arguments.of("GreenSchedulingWhatever", "[shorttitle]")
        );
    }

    @Test
    void crossrefShorttitle() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.TITLE, "Green Scheduling of Whatever");
        database.insertEntry(entry2);
        database.insertEntry(entry1);

        assertEquals("GreenSchedulingWhatever", generateKey(entry1, "[shorttitle]", database));
    }

    @Test
    void crossrefShorttitleInitials() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry().withCitationKey("entry2").withField(StandardField.TITLE, "Green Scheduling of Whatever");
        database.insertEntry(entry2);
        database.insertEntry(entry1);

        assertEquals("GSo", generateKey(entry1, "[shorttitleINI]", database));
    }

    @ParameterizedTest
    @CsvSource({
            "'GreenSchedulingOf:Whatever', 'Green Scheduling of: Whatever'",
            "'GreenSchedulingofWhatever', 'Green Scheduling of `Whatever`'"
    })
    void generateKeyStripsSpecialCharsFromTitle(String expected, String title) {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, title);
        assertEquals(expected, generateKey(entry, "[title]"));
    }

    @Test
    void generateKeyWithOneModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "The Interesting Title");
        assertEquals("theinterestingtitle", generateKey(entry, "[title:lower]"));
    }

    @Test
    void generateKeyWithTwoModifiers() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "The Interesting Title");
        assertEquals("theinterestingtitle", generateKey(entry, "[title:lower:(_)]"));
    }

    @Test
    void generateKeyWithTitleCapitalizeModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("TheInterestingTitleLongerThanThreeWords", generateKey(entry, "[title:capitalize]"));
    }

    @Test
    void generateKeyWithShortTitleCapitalizeModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("InterestingTitleLonger", generateKey(entry, "[shorttitle:capitalize]"));
    }

    @Test
    void generateKeyWithTitleTitleCaseModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "A title WITH some of The key words");
        assertEquals("ATitlewithSomeoftheKeyWords", generateKey(entry, "[title:titlecase]"));
    }

    @Test
    void generateKeyWithShortTitleTitleCaseModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "the InTeresting title longer than THREE words");
        assertEquals("InterestingTitleLonger", generateKey(entry, "[shorttitle:titlecase]"));
    }

    @Test
    void generateKeyWithTitleSentenceCaseModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "A title WITH some of The key words");
        assertEquals("Atitlewithsomeofthekeywords", generateKey(entry, "[title:sentencecase]"));
    }

    @Test
    void generateKeyWithAuthUpperYearShortTitleCapitalizeModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1).withField(StandardField.YEAR, "2019").withField(StandardField.TITLE, "the InTeresting title longer than THREE words");

        assertEquals("NEWTON2019InterestingTitleLonger", generateKey(entry, "[auth:upper][year][shorttitle:capitalize]"));
    }

    @Test
    void generateKeyWithYearAuthUpperTitleSentenceCaseModifier() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3).withField(StandardField.YEAR, "2019").withField(StandardField.TITLE, "the InTeresting title longer than THREE words");

        assertEquals("NewtonMaxwellEtAl_2019_TheInterestingTitleLongerThanThreeWords", generateKey(entry, "[authors2]_[year]_[title:capitalize]"));
    }

    @Test
    void generateKeyWithMinusInCitationStyleOutsideAField() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1).withField(StandardField.YEAR, "2019");

        assertEquals("Newton-2019", generateKey(entry, "[auth]-[year]"));
    }

    @Test
    void generateKeyWithWithFirstNCharacters() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Newton, Isaac").withField(StandardField.YEAR, "2019");

        assertEquals("newt-2019", generateKey(entry, "[auth4:lower]-[year]"));
    }

    @Test
    void generateKeyCorrectKeyLengthWithTruncateModifierAndUnicode() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Gödel, Kurt");

        assertEquals(2, generateKey(bibEntry, "[auth:truncate2]").length());
    }

    @Test
    void generateKeyCorrectKeyLengthWithAuthNofMthAndUnicode() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.AUTHOR, "Gödel, Kurt");

        assertEquals(4, generateKey(bibEntry, "[auth4_1]").length());
    }

    @Test
    void generateKeyWithNonNormalizedUnicode() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Modèle et outil pour soutenir la scénarisation pédagogique de MOOC connectivistes");

        assertEquals("Modele", generateKey(bibEntry, "[veryshorttitle]"));
    }

    @Test
    void generateKeyWithModifierContainingRegexCharacterClass() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Wickedness Managing");
        assertEquals("WM", generateKey(bibEntry, "[title:regex(\"[a-z]+\",\"\")]"));
    }

    @Test
    void generateKeyDoesNotModifyTheKeyWithIncorrectRegexReplacement() {
        String pattern = "[title]";
        GlobalCitationKeyPatterns keyPattern = GlobalCitationKeyPatterns.fromPattern(pattern);
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(true, false, false, false, CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A, "[", "", DEFAULT_UNWANTED_CHARACTERS, keyPattern, "", ',');

        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Wickedness Managing");
        assertEquals("WickednessManaging", new CitationKeyGenerator(keyPattern, new BibDatabase(), patternPreferences).generateKey(bibEntry));
    }

    @Test
    void generateKeyWithFallbackField() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.YEAR, "2021");

        assertEquals("2021", generateKey(bibEntry, "[title:([EPRINT:([YEAR])])]"));
    }

    @ParameterizedTest
    @MethodSource("generateKeyWithLowercaseAuthorData")
    void generateKeyWithLowercaseAuthor(String expected, String author) {
        BibEntry entry = createABibEntryAuthor(author);
        entry.setField(StandardField.YEAR, "2021");
        assertEquals(expected, generateKey(entry, "[auth][year]"));
    }

    static Stream<Arguments> generateKeyWithLowercaseAuthorData() {
        return Stream.of(
                Arguments.of("dAscoli2021", "Stéphane d'Ascoli"),
                Arguments.of("Brekel2021", "Michiel van den Brekel")
        );
    }

    @Test
    void generateKeyCorrectKeyWithAndOthersAtTheEnd() {
        BibEntry entry = createABibEntryAuthor("Alexander Artemenko and others");
        entry.setField(StandardField.YEAR, "2019");
        assertEquals("Artemenko2019", generateKey(entry, "[auth][year]"));
    }

    @Test
    void generateKeyWithTransliteration() {
        BibEntry entry = createABibEntryAuthor("Надежда Карпенко");
        entry.setField(StandardField.YEAR, "2025");
        assertEquals("Karpenko2025", generateKey(entry, "[auth][year]"));
    }

    @Test
    void generateKeyWithoutTransliteration() {
        BibEntry entry = createABibEntryAuthor("Надежда Карпенко");
        entry.setField(StandardField.YEAR, "2025");
        assertEquals("Karpenko2025", generateKey(entry, "[auth][year]"));
    }
}
