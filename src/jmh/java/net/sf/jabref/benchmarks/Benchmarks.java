package net.sf.jabref.benchmarks;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.MetaData;
import net.sf.jabref.exporter.BibDatabaseWriter;
import net.sf.jabref.exporter.SaveException;
import net.sf.jabref.exporter.SavePreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.HtmlToLatexFormatter;
import net.sf.jabref.logic.layout.format.HTMLChars;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.logic.search.SearchQuery;
import net.sf.jabref.logic.util.strings.HTMLUnicodeConversionMaps;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.BibDatabaseModeDetection;
import net.sf.jabref.model.entry.BibEntry;

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
    private final List<String> latexConversionStrings = new ArrayList<>();
    private final List<String> htmlConversionStrings = new ArrayList<>();

    @Setup
    public void init() throws IOException, SaveException {
        Globals.prefs = JabRefPreferences.getInstance();

        Random randomizer = new Random();
        for (int i = 0; i < 1000; i++) {
            BibEntry entry = new BibEntry();
            entry.setCiteKey("id" + i);
            entry.setField("title", "This is my title " + i);
            entry.setField("author", "Firstname Lastname and FirstnameA LastnameA and FirstnameB LastnameB" + i);
            entry.setField("journal", "Journal Title " + i);
            entry.setField("year", "1" + i);
            entry.setField("rnd", "2" + randomizer.nextInt());
            database.insertEntry(entry);
        }
        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        StringWriter stringWriter = new StringWriter();

        databaseWriter.writePartOfDatabase(stringWriter,
                new BibDatabaseContext(database, new MetaData(), new Defaults()), database.getEntries(),
                new SavePreferences());
        bibtexString = stringWriter.toString();

        List<String> latexSymbols = new ArrayList<>(HTMLUnicodeConversionMaps.UNICODE_LATEX_CONVERSION_MAP.values());
        int symbolcount = latexSymbols.size();
        StringBuilder sb = new StringBuilder();
        sb.append("{A} \\textbf{bold} ");
        sb.append(latexSymbols.get(Math.abs(randomizer.nextInt() % symbolcount)));
        sb.append(" {\\it italic} {");
        sb.append(latexSymbols.get(Math.abs(randomizer.nextInt() % symbolcount)));
        sb.append(latexSymbols.get(Math.abs(randomizer.nextInt() % symbolcount)));
        sb.append("} abc");
        latexConversionStrings.add(sb.toString());

        List<String> htmlSymbols = new ArrayList<>(HTMLUnicodeConversionMaps.HTML_LATEX_CONVERSION_MAP.keySet());
        symbolcount = htmlSymbols.size();
        sb = new StringBuilder();
        sb.append("A <b>bold</b> ");
        sb.append(htmlSymbols.get(Math.abs(randomizer.nextInt() % symbolcount)));
        sb.append(" <it>italic</it> ");
        sb.append(htmlSymbols.get(Math.abs(randomizer.nextInt() % symbolcount)));
        sb.append(htmlSymbols.get(Math.abs(randomizer.nextInt() % symbolcount)));
        sb.append("&#8211; abc");
        htmlConversionStrings.add(sb.toString());
    }

    @Benchmark
    public ParserResult parse() throws IOException {
        StringReader bibtexStringReader = new StringReader(bibtexString);
        BibtexParser parser = new BibtexParser(bibtexStringReader);
        return parser.parse();
    }

    @Benchmark
    public String write() throws IOException {
        StringWriter stringWriter = new StringWriter();

        BibDatabaseWriter databaseWriter = new BibDatabaseWriter();
        databaseWriter.writePartOfDatabase(stringWriter,
                new BibDatabaseContext(database, new MetaData(), new Defaults()), database.getEntries(),
                new SavePreferences());
        return stringWriter.toString();
    }

    @Benchmark
    public List<BibEntry> search() {
        // FIXME: Reuse SearchWorker here
        SearchQuery searchQuery = new SearchQuery("Journal Title 500", false, false);
        List<BibEntry> matchedEntries = new ArrayList<>();
        matchedEntries.addAll(database.getEntries().stream().filter(searchQuery::isMatch).collect(Collectors.toList()));
        return matchedEntries;
    }

    @Benchmark
    public BibDatabaseMode inferBibDatabaseMode() {
        return BibDatabaseModeDetection.inferMode(database);
    }

    @Benchmark
    public List<String> latexToUnicodeConversion() {
        List<String> result = new ArrayList<>(1000);
        LatexToUnicodeFormatter f = new LatexToUnicodeFormatter();
        for (String s : latexConversionStrings) {
            result.add(f.format(s));
        }
        return result;
    }

    @Benchmark
    public List<String> latexToHTMLConversion() {
        List<String> result = new ArrayList<>(1000);
        HTMLChars f = new HTMLChars();
        for (String s : latexConversionStrings) {
            result.add(f.format(s));
        }
        return result;
    }

    @Benchmark
    public List<String> htmlToLatexConversion() {
        List<String> result = new ArrayList<>(1000);
        HtmlToLatexFormatter f = new HtmlToLatexFormatter();
        for (String s : htmlConversionStrings) {
            result.add(f.format(s));
        }
        return result;
    }

    public static void main(String[] args) throws IOException, RunnerException {
        Main.main(args);
    }
}
