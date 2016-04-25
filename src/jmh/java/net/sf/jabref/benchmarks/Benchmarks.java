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
import net.sf.jabref.logic.search.SearchQuery;
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

    String bibtexString;
    BibDatabase database = new BibDatabase();

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

    public static void main(String[] args) throws IOException, RunnerException {
        Main.main(args);
    }
}
