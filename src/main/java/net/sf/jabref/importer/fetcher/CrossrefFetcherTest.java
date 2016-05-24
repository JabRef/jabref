package net.sf.jabref.importer.fetcher;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

public class CrossrefFetcherTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        Globals.prefs = JabRefPreferences.getInstance();

        BibtexParser parser = new BibtexParser(new FileReader(args[0]));
        ParserResult result = parser.parse();
        BibDatabase db = result.getDatabase();

        int total = result.getDatabase().getEntryCount();

        AtomicInteger dois = new AtomicInteger();
        AtomicInteger doiFound = new AtomicInteger();
        AtomicInteger doiNew = new AtomicInteger();
        AtomicInteger doiIdentical = new AtomicInteger();


        List<BibEntry> entries = db.getEntries();
        CountDownLatch countDownLatch = new CountDownLatch(entries.size());

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        for (BibEntry entry : entries) {
            executorService.execute(new Runnable() {

                @Override
                public void run() {
                    Optional<DOI> origDOI = DOI.build(entry.getField("doi"));
                    if (origDOI.isPresent()) {
                        dois.incrementAndGet();
                        Optional<DOI> crossrefDOI = CrossRef.findDOI(entry);
                        if (crossrefDOI.isPresent()) {
                            doiFound.incrementAndGet();
                            if (origDOI.get().getDOI().equalsIgnoreCase(crossrefDOI.get().getDOI())) {
                                doiIdentical.incrementAndGet();
                            } else {
                                System.out.println("DOI not identical for : " + entry);
                            }
                        } else {
                            System.out.println("DOI not found for: " + entry);
                        }
                    } else {
                        Optional<DOI> crossrefDOI = CrossRef.findDOI(entry);
                        if (crossrefDOI.isPresent()) {
                            System.out.println("New DOI found for: " + entry);
                            doiNew.incrementAndGet();
                        }
                    }
                    countDownLatch.countDown();
                }
            });

        }
        countDownLatch.await();

        System.out.println("---------------------------------");
        System.out.println("Total DB size: " + total);
        System.out.println("Total DOIs: " + dois);
        System.out.println("DOIs found: " + doiFound);
        System.out.println("DOIs identical: " + doiIdentical);
        System.out.println("New DOIs found: " + doiNew);

        executorService.shutdown();
    }
}
