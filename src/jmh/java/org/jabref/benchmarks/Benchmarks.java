package org.jabref.benchmarks;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.jabref.Globals;
import org.jabref.logic.exporter.BibtexDatabaseWriter;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.layout.format.HTMLChars;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.search.SearchQuery;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.database.BibDatabaseModeDetection;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.WordKeywordGroup;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.preferences.JabRefPreferences;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;

import static org.mockito.Mockito.mock;

@State(Scope.Thread)
public class Benchmarks {

    private String bibtexString;
    private final BibDatabase database = new BibDatabase();
    private String latexConversionString;
    private String htmlConversionString;

    @Setup
    public void init() throws Exception {
        Globals.prefs = JabRefPreferences.getInstance();

        Random randomizer = new Random();
        for (int i = 0; i < 1000; i++) {
            BibEntry entry = new BibEntry();
            entry.setCiteKey("id" + i);
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
    }

    private StringWriter getOutputWriter() throws IOException {
        StringWriter outputWriter = new StringWriter();
        BibtexDatabaseWriter databaseWriter = new BibtexDatabaseWriter(outputWriter, mock(SavePreferences.class), new BibEntryTypesManager());
        databaseWriter.savePartOfDatabase(
                new BibDatabaseContext(database, new MetaData(), new Defaults()), database.getEntries());
        return outputWriter;
    }

    @Benchmark
    public ParserResult parse() throws IOException {
        BibtexParser parser = new BibtexParser(Globals.prefs.getImportFormatPreferences(), new DummyFileUpdateMonitor());
        return parser.parse(new StringReader(bibtexString));
    }

    @Benchmark
    public String write() throws Exception {
        return getOutputWriter().toString();
    }

    @Benchmark
    public List<BibEntry> search() {
        // FIXME: Reuse SearchWorker here
        SearchQuery searchQuery = new SearchQuery("Journal Title 500", false, false);
        return database.getEntries().stream().filter(searchQuery::isMatch).collect(Collectors.toList());
    }

    @Benchmark
    public List<BibEntry> parallelSearch() {
        // FIXME: Reuse SearchWorker here
        SearchQuery searchQuery = new SearchQuery("Journal Title 500", false, false);
        return database.getEntries().parallelStream().filter(searchQuery::isMatch).collect(Collectors.toList());
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

    public static void main(String[] args) throws IOException, RunnerException {
        Main.main(args);
    }
}
