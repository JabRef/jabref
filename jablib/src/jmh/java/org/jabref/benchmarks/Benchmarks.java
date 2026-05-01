package org.jabref.benchmarks;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.BibWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.layout.format.HTMLChars;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.search.LuceneIndexer;
import org.jabref.logic.search.indexing.DefaultLinkedFilesIndexer;
import org.jabref.logic.search.retrieval.LinkedFilesSearcher;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.BibDatabaseModeDetection;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.search.SearchFlags;
import org.jabref.model.search.query.SearchQuery;
import org.jabref.model.search.query.SearchResults;

import com.airhacks.afterburner.injection.Injector;
import org.apache.commons.io.FileUtils;
import org.mockito.Answers;
import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@State(Scope.Thread)
public class Benchmarks {

    private String bibtexString;
    private final BibDatabase database = new BibDatabase();
    private String latexConversionString;
    private String htmlConversionString;
    private LuceneIndexer linkedFilesIndexer;
    private LinkedFilesSearcher linkedFilesSearcher;
    private SearchQuery fulltextSearchQuery;
    private Path luceneIndexDir;
    private List<BibEntry> pdfEntries;

    @Setup
    public void init() throws IOException {
        Injector.setModelOrService(CliPreferences.class, JabRefCliPreferences.getInstance());

        Random randomizer = new Random();
        for (int i = 0; i < 1000; i++) {
            BibEntry entry = new BibEntry();
            entry.setCitationKey("id" + i);
            entry.setField(StandardField.TITLE, "This is my title " + i);
            entry.setField(StandardField.AUTHOR, "Firstname Lastname and FirstnameA LastnameA and FirstnameB LastnameB" + i);
            entry.setField(StandardField.JOURNAL, "Journal Title " + i);
            entry.setField(StandardField.KEYWORDS, "testkeyword");
            entry.setField(StandardField.YEAR, "1" + i);
            entry.setField(new UnknownField("rnd"), "2" + randomizer.nextInt());
            database.insertEntry(entry);
        }

        bibtexString = getOutputWriter().toString();

        latexConversionString = "{A} \\textbf{bold} approach {\\it to} ${{\\Sigma}}{\\Delta}$ modulator \\textsuperscript{2} \\$";

        htmlConversionString = "<b>&Ouml;sterreich</b> &#8211; &amp; characters &#x2aa2; <i>italic</i>";

        luceneIndexDir = Files.createTempDirectory("jabref-benchmark-lucene");

        FilePreferences filePreferences = mock(FilePreferences.class);
        when(filePreferences.shouldFulltextIndexLinkedFiles()).thenReturn(true);

        Path pdfResourceDir = Path.of("src/test/resources/pdfs");
        BibDatabaseContext linkedFilesContext = mock(BibDatabaseContext.class);
        when(linkedFilesContext.getDatabasePath()).thenReturn(Optional.of(pdfResourceDir.resolve("dummy.bib")));
        when(linkedFilesContext.getFileDirectories(filePreferences)).thenReturn(List.of(pdfResourceDir));
        when(linkedFilesContext.getFulltextIndexPath()).thenReturn(luceneIndexDir);

        pdfEntries = List.of(
                new BibEntry(StandardEntryType.PhdThesis)
                        .withCitationKey("ExampleThesis2017")
                        .withFiles(List.of(new LinkedFile("Example Thesis", "thesis-example.pdf", StandardFileType.PDF.getName())))
        );
        when(linkedFilesContext.getEntries()).thenReturn(pdfEntries);

        linkedFilesIndexer = new DefaultLinkedFilesIndexer(linkedFilesContext, filePreferences);
        linkedFilesIndexer.addToIndex(pdfEntries, mock(BackgroundTask.class));

        linkedFilesSearcher = new LinkedFilesSearcher(linkedFilesContext, linkedFilesIndexer, filePreferences);

        fulltextSearchQuery = new SearchQuery("title", EnumSet.of(SearchFlags.FULLTEXT));
    }

    private StringWriter getOutputWriter() throws IOException {
        StringWriter outputWriter = new StringWriter();
        BibWriter bibWriter = new BibWriter(outputWriter, OS.NEWLINE);
        SelfContainedSaveConfiguration saveConfiguration = new SelfContainedSaveConfiguration(SaveOrder.getDefaultSaveOrder(), false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, false);
        FieldPreferences fieldPreferences = new FieldPreferences(true, List.of(), List.of());
        CitationKeyPatternPreferences citationKeyPatternPreferences = mock(CitationKeyPatternPreferences.class, Answers.RETURNS_DEEP_STUBS);

        BibDatabaseWriter databaseWriter = new BibDatabaseWriter(
                bibWriter,
                saveConfiguration,
                fieldPreferences,
                citationKeyPatternPreferences,
                new BibEntryTypesManager());
        databaseWriter.writePartOfDatabase(new BibDatabaseContext(database, new MetaData()), database.getEntries());
        return outputWriter;
    }

    @Benchmark
    public ParserResult parse() throws IOException {
        CliPreferences preferences = Injector.instantiateModelOrService(CliPreferences.class);
        BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences());
        return parser.parse(new StringReader(bibtexString));
    }

    @Benchmark
    public String write() throws IOException {
        return getOutputWriter().toString();
    }

    @Benchmark
    public SearchResults search() {
        return linkedFilesSearcher.search(fulltextSearchQuery);
    }

    @Benchmark
    public void index() throws IOException {
        linkedFilesIndexer.removeAllFromIndex();
        linkedFilesIndexer.addToIndex(pdfEntries, mock(BackgroundTask.class));
    }

    @Benchmark
    public BibDatabaseMode inferBibDatabaseMode() {
        return BibDatabaseModeDetection.inferMode(database);
    }

    @Benchmark
    public String latexToUnicodeConversion() {
        LatexToUnicodeFormatter f = new LatexToUnicodeFormatter();
        return f.format(latexConversionString);
    }

    @Benchmark
    public String latexToHTMLConversion() {
        HTMLChars f = new HTMLChars();
        return f.format(latexConversionString);
    }

    @Benchmark
    public String htmlToLatexConversion() {
        HtmlToLatexFormatter f = new HtmlToLatexFormatter();
        return f.format(htmlConversionString);
    }

    @Benchmark
    public boolean keywordGroupContains() {
        KeywordGroup group = new WordKeywordGroup("testGroup", GroupHierarchyType.INDEPENDENT, StandardField.KEYWORDS, "testkeyword", false, ',', false);
        return group.containsAll(database.getEntries());
    }

    @TearDown
    public void tearDown() throws IOException {
        linkedFilesIndexer.closeAndWait();
        FileUtils.deleteDirectory(luceneIndexDir.toFile());
    }

    static void main(String[] args) throws IOException {
        Main.main(args);
    }
}
