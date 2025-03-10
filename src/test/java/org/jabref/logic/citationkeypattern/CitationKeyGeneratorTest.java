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
 *
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
    private static final String AUTHORSALPHA = "[authorsAlpha]";
    private static final String AUTHORSALPHALNI = "[authorsAlphaLNI]";
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
        return generateKey(entry, pattern, new BibDatabase());
    }

    static String generateKey(BibEntry entry, String pattern, BibDatabase database) {
        GlobalCitationKeyPatterns keyPattern = GlobalCitationKeyPatterns.fromPattern(pattern);
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "",
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                "",
                ',');

        return new CitationKeyGenerator(keyPattern, database, patternPreferences).generateKey(entry);
    }

    @Test
    void andInAuthorName() throws ParseException {
        Optional<BibEntry> entry0 = BibtexParser.singleFromString("@ARTICLE{kohn, author={Simon Holland}}",
                importFormatPreferences);
        assertEquals("Holland",
                CitationKeyGenerator.cleanKey(generateKey(entry0.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefAndInAuthorNames() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry().withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.AUTHOR, "Simon Holland");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("Holland",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void andAuthorNames() throws ParseException {
        String bibtexString = "@ARTICLE{whatevery, author={Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland}}";
        Optional<BibEntry> entry = BibtexParser.singleFromString(bibtexString, importFormatPreferences);
        assertEquals("HerlandHaugeHelgeland",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[authors3]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefAndAuthorNames() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.AUTHOR, "Mari D. Herland and Mona-Iren Hauge and Ingeborg M. Helgeland");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("HerlandHaugeHelgeland",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "[authors3]",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void specialLatexCharacterInAuthorName() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={Simon Popovi\\v{c}ov\\'{a}}}", importFormatPreferences);
        assertEquals("Popovicova",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @ParameterizedTest(name = "bibtexString={0}, expectedResult={1}")
    @CsvSource(quoteCharacter = '"', textBlock = """
            # see https://sourceforge.net/forum/message.php?msg_id=4498555
            "@ARTICLE{kohn, author={Andreas Köning}, year={2000}}", "Koe",

            # Accent ague: Á á Ć ć É é Í í Ĺ ĺ Ń ń Ó ó Ŕ ŕ Ś ś Ú ú Ý ý Ź ź
            "@ARTICLE{kohn, author={Andreas Áöning}, year={2000}}", "Aoe",
            "@ARTICLE{kohn, author={Andreas Éöning}, year={2000}}", "Eoe",
            "@ARTICLE{kohn, author={Andreas Íöning}, year={2000}}", "Ioe",
            "@ARTICLE{kohn, author={Andreas Ĺöning}, year={2000}}", "Loe",
            "@ARTICLE{kohn, author={Andreas Ńöning}, year={2000}}", "Noe",
            "@ARTICLE{kohn, author={Andreas Óöning}, year={2000}}", "Ooe",
            "@ARTICLE{kohn, author={Andreas Ŕöning}, year={2000}}", "Roe",
            "@ARTICLE{kohn, author={Andreas Śöning}, year={2000}}", "Soe",
            "@ARTICLE{kohn, author={Andreas Úöning}, year={2000}}", "Uoe",
            "@ARTICLE{kohn, author={Andreas Ýöning}, year={2000}}", "Yoe",
            "@ARTICLE{kohn, author={Andreas Źöning}, year={2000}}", "Zoe",

            # Accent grave: À È Ì Ò Ù
            "@ARTICLE{kohn, author={Andreas Àöning}, year={2000}}", "Aoe",
            "@ARTICLE{kohn, author={Andreas Èöning}, year={2000}}", "Eoe",
            "@ARTICLE{kohn, author={Andreas Ìöning}, year={2000}}", "Ioe",
            "@ARTICLE{kohn, author={Andreas Òöning}, year={2000}}", "Ooe",
            "@ARTICLE{kohn, author={Andreas Ùöning}, year={2000}}", "Uoe",

            # Special cases
            # We keep "-" in citation keys, thus Al-Ketan with three letters is "Al-"
            "@ARTICLE{kohn, author={Oraib Al-Ketan}, year={2000}}", "Al-",
            "@ARTICLE{kohn, author={Andrés D'Alessandro}, year={2000}}", "DAl",
            "@ARTICLE{kohn, author={Andrés Aʹrnold}, year={2000}}", "Arn"
            """
    )
    void makeLabelAndCheckLegalKeys(String bibtexString, String expectedResult) throws ParseException {
        BibEntry bibEntry = BibtexParser.singleFromString(bibtexString, importFormatPreferences).get();
        String citationKey = generateKey(bibEntry, "[auth3]", new BibDatabase());

        String cleanedKey = CitationKeyGenerator.cleanKey(citationKey, DEFAULT_UNWANTED_CHARACTERS);

        assertEquals(expectedResult, cleanedKey);
    }

    @Test
    void firstAuthor() {
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, "[auth]"));
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[auth]"));

        // https://sourceforge.net/forum/message.php?msg_id=4498555
        assertEquals("Koening", generateKey(createABibEntryAuthor("K{\\\"o}ning"), "[auth]"));

        assertEquals("", generateKey(createABibEntryAuthor(""), "[auth]"));
    }

    @Test
    void university() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University}}}", importFormatPreferences);
        assertEquals("UniLinkoeping",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    /**
     * Tests if cleanKey replaces Non-ASCII chars. There are quite a few chars that should be replaced. Perhaps there is
     * a better method than the current.
     * <p>
     * not tested/ not in hashmap UNICODE_CHARS:
     * {@code
     * Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə Đ đ   Ů ů    Ǣ ǣ ǖ ǘ ǚ ǜ
     *  Ǣ ǣ ǖ ǘ ǚ ǜ
     * Đ đ   Ů ů
     * Ł ł   Ő ő Ű ű   Ŀ ŀ   Ħ ħ   Ð ð Þ þ   Œ œ   Æ æ Ø ø Å å   Ə ə
     * }
     *
     * @see CitationKeyGenerator#cleanKey(String, String)
     */
    @ParameterizedTest(name = "accents={0}, expectedResult={1}")
    @CsvSource(quoteCharacter = '"', textBlock = """
            "ÀàÈèÌìÒòÙù Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ", "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYy",
            "ÄäËëÏïÖöÜüŸÿ", "AeaeEeIiOeoeUeueYy",
            "ÅåŮů", "AaaaUu",
            "Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ", "CcGgKkLlNnRrSsTt",
            "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ", "AaEeGgIiOoUu",
            "Ċ ċ Ė ė Ġ ġ İ ı Ż ż", "CcEeGgIiZz",
            "Ą ą Ę ę Į į Ǫ ǫ Ų ų", "AaEeIiOoUu", # O or Q? o or q?
            "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ", "AaEeIiOoUuYy",
            "Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž", "AaCcDdEeIiLlNnOoRrSsTtUuZz",
            "ÃãẼẽĨĩÑñÕõŨũỸỹ", "AaEeIiNnOoUuYy",
            "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ", "DdHhLlLlMmNnRrRrSsTt",
            "À à È è Ì ì Ò ò Ù ù   Â â Ĉ ĉ Ê ê Ĝ ĝ Ĥ ĥ Î î Ĵ ĵ Ô ô Ŝ ŝ Û û Ŵ ŵ Ŷ ŷ  Ä ä Ë ë Ï ï Ö ö Ü ü Ÿ ÿ    ", "AaEeIiOoUuAaCcEeGgHhIiJjOoSsUuWwYyAeaeEeIiOeoeUeueYy",
            "Ã ã Ẽ ẽ Ĩ ĩ Ñ ñ Õ õ Ũ ũ Ỹ ỹ   Ç ç Ģ ģ Ķ ķ Ļ ļ Ņ ņ Ŗ ŗ Ş ş Ţ ţ", "AaEeIiNnOoUuYyCcGgKkLlNnRrSsTt",
            " Ǎ ǎ Č č Ď ď Ě ě Ǐ ǐ Ľ ľ Ň ň Ǒ ǒ Ř ř Š š Ť ť Ǔ ǔ Ž ž   ", "AaCcDdEeIiLlNnOoRrSsTtUuZz",
            "Ā ā Ē ē Ī ī Ō ō Ū ū Ȳ ȳ", "AaEeIiOoUuYy",
            "Ă ă Ĕ ĕ Ğ ğ Ĭ ĭ Ŏ ŏ Ŭ ŭ   ", "AaEeGgIiOoUu",
            "Ả ả Ẻ ẻ Ỉ ỉ Ỏ ỏ Ủ ủ Ỷ ỷ", "AaEeIiOoUuYy",
            "Ḛ ḛ Ḭ ḭ Ṵ ṵ", "EeIiUu",
            "Ċ ċ Ė ė Ġ ġ İ ı Ż ż   Ą ą Ę ę Į į Ǫ ǫ Ų ų   ", "CcEeGgIiZzAaEeIiOoUu",
            "Ḍ ḍ Ḥ ḥ Ḷ ḷ Ḹ ḹ Ṃ ṃ Ṇ ṇ Ṛ ṛ Ṝ ṝ Ṣ ṣ Ṭ ṭ   ", "DdHhLlLlMmNnRrRrSsTt"
            """
    )
    void checkLegalKey(String accents, String expectedResult) {
        assertEquals(expectedResult, CitationKeyGenerator.cleanKey(accents, DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefUniversity() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoeping",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void department() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, Department of Electrical Engineering}}}",
                importFormatPreferences);
        assertEquals("UniLinkoepingEE",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefDepartment() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University, Department of Electrical Engineering}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoepingEE",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void school() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Link{\\\"{o}}ping University, School of Computer Engineering}}}",
                importFormatPreferences);
        assertEquals("UniLinkoepingCE",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void generateKeyAbbreviateCorporateAuthorDepartmentWithoutAcademicInstitute() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{null, author={{Department of Localhost NullGenerators}}}",
                importFormatPreferences);
        assertEquals("DLN",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void generateKeyAbbreviateCorporateAuthorSchoolWithoutAcademicInstitute() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{null, author={{The School of Null}}}",
                importFormatPreferences);
        assertEquals("SchoolNull",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefSchool() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.AUTHOR, "{Link{\\\"{o}}ping University, School of Computer Engineering}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("UniLinkoepingCE",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void instituteOfTechnology() throws ParseException {
        Optional<BibEntry> entry = BibtexParser.singleFromString(
                "@ARTICLE{kohn, author={{Massachusetts Institute of Technology}}}", importFormatPreferences);
        assertEquals("MIT",
                CitationKeyGenerator.cleanKey(generateKey(entry.orElse(null), "[auth]",
                        new BibDatabase()), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void crossrefInstituteOfTechnology() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.AUTHOR, "{Massachusetts Institute of Technology}");
        database.insertEntry(entry1);
        database.insertEntry(entry2);

        assertEquals("MIT",
                CitationKeyGenerator.cleanKey(generateKey(entry1, "[auth]",
                        database), DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void authIniN() {
        assertEquals("", generateKey(AUTHOR_EMPTY, "[authIni4]"));
        assertEquals("Newt", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni4]"));
        assertEquals("NeMa", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, "[authIni4]"));
        assertEquals("NeME", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, "[authIni4]"));
        assertEquals("NMEB", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, "[authIni4]"));
        assertEquals("NMEB", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, "[authIni4]"));

        assertEquals("N", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni1]"));
        assertEquals("", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni0]"));

        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni6]"));
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, "[authIni7]"));
    }

    @Test
    void authIniNEmptyReturnsEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, "[authIni1]"));
    }

    /**
     * Tests  [auth.auth.ea]
     */
    @Test
    void authAuthEa() {
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1, AUTHAUTHEA));
        assertEquals("Newton.Maxwell", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTHAUTHEA));
        assertEquals("Newton.Maxwell.ea", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, AUTHAUTHEA));
    }

    @Test
    void authEaEmptyReturnsEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, AUTHAUTHEA));
    }

    /**
     * Tests the [auth.etal] and [authEtAl] patterns
     */
    @Test
    void authEtAl() {
        // tests taken from the comments

        // [auth.etal]
        assertEquals("Newton.etal", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, AUTH_ETAL));
        assertEquals("Newton.Maxwell", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTH_ETAL));

        // [authEtAl]
        assertEquals("NewtonEtAl", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3, AUTHETAL));
        assertEquals("NewtonMaxwell", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTHETAL));
    }

    /**
     * Test the [authshort] pattern
     */
    @Test
    void authShort() {
        // tests taken from the comments
        assertEquals("NME+", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHSHORT));
        assertEquals("NME", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHSHORT));
        assertEquals("NM", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHSHORT));
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHSHORT));
    }

    @Test
    void authShortEmptyReturnsEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, AUTHSHORT));
    }

    /**
     * Test the [authN_M] pattern
     */
    @Test
    void authNM() {
        assertEquals("N", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHNOFMTH.formatted(1, 1)));
        assertEquals("Max", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHNOFMTH.formatted(3, 2)));
        assertEquals("New", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHNOFMTH.formatted(3, 1)));
        assertEquals("Bo", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHNOFMTH.formatted(2, 4)));
        assertEquals("Bohr", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, AUTHNOFMTH.formatted(6, 4)));

        assertEquals("Aal", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHNOFMTH.formatted(3, 1)));
        assertEquals("Less", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHNOFMTH.formatted(4, 2)));

        assertEquals("", generateKey(AUTHOR_EMPTY, AUTHNOFMTH.formatted(2, 4)));
    }

    /**
     * Tests [authForeIni]
     */
    @Test
    void firstAuthorForenameInitials() {
        assertEquals("I", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHFOREINI));
        assertEquals("I", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHFOREINI));
        assertEquals("I", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1, AUTHFOREINI));
        assertEquals("I", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTHFOREINI));
    }

    /**
     * Tests [authFirstFull]
     */
    @Test
    void firstAuthorVonAndLast() {
        assertEquals("vanderAalst", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHFIRSTFULL));
        assertEquals("vanderAalst", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHFIRSTFULL));
    }

    @Test
    void firstAuthorVonAndLastNoVonInName() {
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1, AUTHFIRSTFULL));
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_2, AUTHFIRSTFULL));
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

    static Stream<Arguments> authorsAlpha() {
        return Stream.of(
                Arguments.of("New", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORSALPHA),
                Arguments.of("NM", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORSALPHA),
                Arguments.of("NME", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORSALPHA),
                Arguments.of("NMEB", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORSALPHA),
                Arguments.of("NME+", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, AUTHORSALPHA),
                Arguments.of("vdAal", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHORSALPHA),
                Arguments.of("vdAvL", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHORSALPHA),
                Arguments.of("NM+", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_AND_OTHERS_COUNT_3, AUTHORSALPHA)
        );
    }

    @ParameterizedTest
    @MethodSource
    void authorsAlphaLNI(String expected, BibEntry entry, String pattern) {
        assertEquals(expected, generateKey(entry, pattern));
    }

    static Stream<Arguments> authorsAlphaLNI() {
        return Stream.of(
                Arguments.of("Ne", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORSALPHALNI),
                Arguments.of("NM", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORSALPHALNI),
                Arguments.of("NME", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORSALPHALNI),
                Arguments.of("NMEB", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORSALPHALNI),
                Arguments.of("NMEB", AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, AUTHORSALPHALNI),
                Arguments.of("Aa", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHORSALPHALNI),
                Arguments.of("AL", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHORSALPHALNI),
                Arguments.of("Ne", AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_AND_OTHERS_COUNT_3, AUTHORSALPHALNI)
        );
    }

    @ParameterizedTest
    @MethodSource
    void authorsAlpha(String expected, BibEntry entry, String pattern) {
        assertEquals(expected, generateKey(entry, pattern));
    }

    /**
     * Tests [authorLast]
     */
    @Test
    void lastAuthor() {
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORLAST));
        assertEquals("Maxwell", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORLAST));
        assertEquals("Einstein", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORLAST));
        assertEquals("Bohr", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORLAST));
        assertEquals("Unknown", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, AUTHORLAST));

        assertEquals("Aalst", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHORLAST));
        assertEquals("Lessen", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHORLAST));
    }

    /**
     * Tests [authorLastForeIni]
     */
    @Test
    void lastAuthorForenameInitials() {
        assertEquals("I", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORLASTFOREINI));
        assertEquals("J", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORLASTFOREINI));
        assertEquals("A", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORLASTFOREINI));
        assertEquals("N", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORLASTFOREINI));
        assertEquals("H", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, AUTHORLASTFOREINI));

        assertEquals("W", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHORLASTFOREINI));
        assertEquals("T", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHORLASTFOREINI));
    }

    /**
     * Tests [authorIni]
     */
    @Test
    void oneAuthorPlusIni() {
        assertEquals("Newto", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORINI));
        assertEquals("NewtoM", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORINI));
        assertEquals("NewtoME", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORINI));
        assertEquals("NewtoMEB", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORINI));
        assertEquals("NewtoMEBU", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_5, AUTHORINI));

        assertEquals("Aalst", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_1, AUTHORINI));
        assertEquals("AalstL", generateKey(AUTHOR_FIRSTNAME_FULL_LASTNAME_FULL_WITH_VAN_COUNT_2, AUTHORINI));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors1]
     */
    @Test
    void nAuthors1() {
        assertEquals("Newton", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORN.formatted(1)));
        assertEquals("NewtonEtAl", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORN.formatted(1)));
        assertEquals("NewtonEtAl", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORN.formatted(1)));
        assertEquals("NewtonEtAl", generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORN.formatted(1)));
    }

    @Test
    void nAuthors1EmptyReturnEmpty() {
        assertEquals("", generateKey(AUTHOR_EMPTY, AUTHORN.formatted(1)));
    }

    /**
     * Tests the [authorsN] pattern. -> [authors3]
     */
    @Test
    void nAuthors3() {
        assertEquals("Newton",
                generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_1, AUTHORN.formatted(3)));
        assertEquals("NewtonMaxwell",
                generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_2, AUTHORN.formatted(3)));
        assertEquals("NewtonMaxwellEinstein",
                generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_3, AUTHORN.formatted(3)));
        assertEquals("NewtonMaxwellEinsteinEtAl",
                generateKey(AUTHOR_FIRSTNAME_INITIAL_LASTNAME_FULL_COUNT_4, AUTHORN.formatted(3)));
    }

    @Test
    void firstPage() {
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
    void firstPageNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.firstPage(null));
    }

    @Test
    void pagePrefix() {
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
    void pagePrefixNull() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.pagePrefix(null));
    }

    @Test
    void lastPage() {
        assertEquals("27", CitationKeyGenerator.lastPage("7--27"));
        assertEquals("27", CitationKeyGenerator.lastPage("--27"));
        assertEquals("", CitationKeyGenerator.lastPage(""));
        assertEquals("111", CitationKeyGenerator.lastPage("42--111"));
        assertEquals("97", CitationKeyGenerator.lastPage("7,41,73--97"));
        assertEquals("97", CitationKeyGenerator.lastPage("7,41,97--73"));
        assertEquals("43", CitationKeyGenerator.lastPage("43+"));
        assertEquals("0", CitationKeyGenerator.lastPage("00--0"));
        assertEquals("1", CitationKeyGenerator.lastPage("1--1"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    void lastPageNull() {
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
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        assertEquals("w1", generateKey(entry, "[keyword1]"));

        // check keywords with space
        assertEquals("w2aw2b", generateKey(entry, "[keyword2]"));

        // check out of range
        assertEquals("", generateKey(entry, "[keyword4]"));
    }

    @Test
    void crossrefkeywordNKeywordsSeparatedBySpace() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2");
        database.insertEntry(entry2);
        database.insertEntry(entry1);
        entry2.setField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        assertEquals("w1", generateKey(entry1, "[keyword1]", database));
    }

    @Test
    void keywordsNKeywordsSeparatedBySpace() {
        BibEntry entry = new BibEntry().withField(StandardField.KEYWORDS, "w1, w2a w2b, w3");

        // all keywords
        assertEquals("w1w2aw2bw3", generateKey(entry, "[keywords]"));

        // check keywords with space
        assertEquals("w1w2aw2b", generateKey(entry, "[keywords2]"));

        // check out of range
        assertEquals("w1w2aw2bw3", generateKey(entry, "[keywords55]"));
    }

    @Test
    void crossrefkeywordsNKeywordsSeparatedBySpace() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.KEYWORDS, "w1, w2a w2b, w3");
        database.insertEntry(entry2);
        database.insertEntry(entry1);

        assertEquals("w1w2aw2bw3", generateKey(entry1, "[keywords]", database));
    }

    @Test
    void checkLegalKeyUnwantedCharacters() {
        assertEquals("AAAA", CitationKeyGenerator.cleanKey("AA AA", DEFAULT_UNWANTED_CHARACTERS));
        assertEquals("SPECIALCHARS", CitationKeyGenerator.cleanKey("SPECIAL CHARS#{\\\"}~,", DEFAULT_UNWANTED_CHARACTERS));
        assertEquals("", CitationKeyGenerator.cleanKey("\n\t\r", DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void checkLegalKeyNoUnwantedCharacters() {
        assertEquals("AAAA", CitationKeyGenerator.cleanKey("AA AA", ""));
        assertEquals("SPECIALCHARS^", CitationKeyGenerator.cleanKey("SPECIAL CHARS#{\\\"}~,^", ""));
        assertEquals("", CitationKeyGenerator.cleanKey("\n\t\r", ""));
    }

    @Test
    void checkLegalNullInNullOut() {
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.cleanKey(null, DEFAULT_UNWANTED_CHARACTERS));
        assertThrows(NullPointerException.class, () -> CitationKeyGenerator.cleanKey(null, DEFAULT_UNWANTED_CHARACTERS));
    }

    @Test
    void applyModifiers() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Green Scheduling of Whatever");
        assertEquals("GSo", generateKey(entry, "[shorttitleINI]"));
        assertEquals("GreenSchedulingWhatever", generateKey(entry, "[shorttitle]",
                new BibDatabase()));
    }

    @Test
    void crossrefShorttitle() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.TITLE, "Green Scheduling of Whatever");
        database.insertEntry(entry2);
        database.insertEntry(entry1);

        assertEquals("GreenSchedulingWhatever", generateKey(entry1, "[shorttitle]",
                database));
    }

    @Test
    void crossrefShorttitleInitials() {
        BibDatabase database = new BibDatabase();
        BibEntry entry1 = new BibEntry()
                .withField(StandardField.CROSSREF, "entry2");
        BibEntry entry2 = new BibEntry()
                .withCitationKey("entry2")
                .withField(StandardField.TITLE, "Green Scheduling of Whatever");
        database.insertEntry(entry2);
        database.insertEntry(entry1);

        assertEquals("GSo", generateKey(entry1, "[shorttitleINI]", database));
    }

    @Test
    void generateKeyStripsColonFromTitle() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Green Scheduling of: Whatever");
        assertEquals("GreenSchedulingOf:Whatever", generateKey(entry, "[title]"));
    }

    @Test
    void generateKeyStripsApostropheFromTitle() {
        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "Green Scheduling of `Whatever`");
        assertEquals("GreenSchedulingofWhatever", generateKey(entry, "[title]"));
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
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1)
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.TITLE, "the InTeresting title longer than THREE words");

        assertEquals("NEWTON2019InterestingTitleLonger", generateKey(entry, "[auth:upper][year][shorttitle:capitalize]"));
    }

    @Test
    void generateKeyWithYearAuthUpperTitleSentenceCaseModifier() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_3)
                .withField(StandardField.YEAR, "2019")
                .withField(StandardField.TITLE, "the InTeresting title longer than THREE words");

        assertEquals("NewtonMaxwellEtAl_2019_TheInterestingTitleLongerThanThreeWords", generateKey(entry, "[authors2]_[year]_[title:capitalize]"));
    }

    @Test
    void generateKeyWithMinusInCitationStyleOutsideAField() {
        BibEntry entry = new BibEntry()
                .withField(StandardField.AUTHOR, AUTHOR_STRING_FIRSTNAME_FULL_LASTNAME_FULL_COUNT_1)
                .withField(StandardField.YEAR, "2019");

        assertEquals("Newton-2019", generateKey(entry, "[auth]-[year]"));
    }

    @Test
    void generateKeyWithWithFirstNCharacters() {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Newton, Isaac")
                                       .withField(StandardField.YEAR, "2019");

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
        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Modèle et outil pour soutenir la scénarisation pédagogique de MOOC connectivistes");

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
        CitationKeyPatternPreferences patternPreferences = new CitationKeyPatternPreferences(
                false,
                false,
                false,
                CitationKeyPatternPreferences.KeySuffix.SECOND_WITH_A,
                "[", // Invalid regexp
                "",
                DEFAULT_UNWANTED_CHARACTERS,
                keyPattern,
                "",
                ',');

        BibEntry bibEntry = new BibEntry().withField(StandardField.TITLE, "Wickedness Managing");
        assertEquals("WickednessManaging",
                new CitationKeyGenerator(keyPattern, new BibDatabase(), patternPreferences).generateKey(bibEntry));
    }

    @Test
    void generateKeyWithFallbackField() {
        BibEntry bibEntry = new BibEntry().withField(StandardField.YEAR, "2021");

        assertEquals("2021", generateKey(bibEntry, "[title:([EPRINT:([YEAR])])]"));
    }

    @Test
    void generateKeyWithLowercaseAuthorLastnameUseVonPart() {
        BibEntry entry = createABibEntryAuthor("Stéphane d'Ascoli");
        entry.setField(StandardField.YEAR, "2021");
        assertEquals("dAscoli2021", generateKey(entry, "[auth][year]"));
    }

    @Test
    void generateKeyWithLowercaseAuthorWithVonAndLastname() {
        BibEntry entry = createABibEntryAuthor("Michiel van den Brekel");
        entry.setField(StandardField.YEAR, "2021");
        assertEquals("Brekel2021", generateKey(entry, "[auth][year]"));
    }

    @Test
    void generateKeyCorrectKeyWithAndOthersAtTheEnd() {
        BibEntry entry = createABibEntryAuthor("Alexander Artemenko and others");
        entry.setField(StandardField.YEAR, "2019");
        assertEquals("Artemenko2019", generateKey(entry, "[auth][year]"));
    }
}
