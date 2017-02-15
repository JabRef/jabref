package net.sf.jabref.benchmarks;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.exporter.BibtexDatabaseWriter;
import net.sf.jabref.logic.exporter.SavePreferences;
import net.sf.jabref.logic.exporter.StringSaveSession;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.importer.ParseException;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.layout.format.HTMLChars;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.model.Defaults;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.BibDatabaseModeDetection;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.groups.KeywordGroup;
import net.sf.jabref.model.groups.WordKeywordGroup;
import net.sf.jabref.model.metadata.MetaData;
import net.sf.jabref.preferences.JabRefPreferences;

import org.openjdk.jmh.Main;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.RunnerException;

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
            entry.setField("title", "This is my title " + i);
            entry.setField("author", "Firstname Lastname and FirstnameA LastnameA and FirstnameB LastnameB" + i);
            entry.setField("journal", "Journal Title " + i);
            entry.setField("keyword", "testkeyword");
            entry.setField("year", "1" + i);
            entry.setField("rnd", "2" + randomizer.nextInt());
            database.insertEntry(entry);
        }
        BibtexDatabaseWriter<StringSaveSession> databaseWriter = new BibtexDatabaseWriter<>(StringSaveSession::new);
        StringSaveSession saveSession = databaseWriter.savePartOfDatabase(
                new BibDatabaseContext(database, new MetaData(), new Defaults()), database.getEntries(),
                new SavePreferences());
        bibtexString = saveSession.getStringValue();

        latexConversionString = "{A} \\textbf{bold} approach {\\it to} ${{\\Sigma}}{\\Delta}$ modulator \\textsuperscript{2} \\$";

        htmlConversionString = "<b>&Ouml;sterreich</b> &#8211; &amp; characters &#x2aa2; <i>italic</i>";
    }

    @Benchmark
    public ParserResult parse() throws IOException {
        BibtexParser parser = new BibtexParser(Globals.prefs.getImportFormatPreferences());
        return parser.parse(new StringReader(bibtexString));
    }

    @Benchmark
    public String write() throws Exception {
        BibtexDatabaseWriter<StringSaveSession> databaseWriter = new BibtexDatabaseWriter<>(StringSaveSession::new);
        StringSaveSession saveSession = databaseWriter.savePartOfDatabase(
                new BibDatabaseContext(database, new MetaData(), new Defaults()), database.getEntries(),
                new SavePreferences());
        return saveSession.getStringValue();
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
    public boolean keywordGroupContains() throws ParseException {
        KeywordGroup group = new WordKeywordGroup("testGroup", GroupHierarchyType.INDEPENDENT, "keyword", "testkeyword", false, ',', false);
        return group.containsAll(database.getEntries());
    }

    public static void main(String[] args) throws IOException, RunnerException {
        Main.main(args);
    }
}
