package net.sf.jabref.cli;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fetcher.CrossRef;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.preferences.JabRefPreferences;

/**
 * Useful for checking out new algorithm improvements and thresholds. Not used inside the JabRef code itself.
 */
public class CrossrefFetcherEvaluator {

    public static void main(String[] args) throws IOException, InterruptedException {
        Globals.prefs = JabRefPreferences.getInstance();
        try (FileReader reader = new FileReader(args[0])) {
            BibtexParser parser = new BibtexParser(Globals.prefs.getImportFormatPreferences());
            ParserResult result = parser.parse(reader);
            BibDatabase db = result.getDatabase();

            List<BibEntry> entries = db.getEntries();

            AtomicInteger dois = new AtomicInteger();
            AtomicInteger doiFound = new AtomicInteger();
            AtomicInteger doiNew = new AtomicInteger();
            AtomicInteger doiIdentical = new AtomicInteger();

            int total = entries.size();

            CountDownLatch countDownLatch = new CountDownLatch(total);

            ExecutorService executorService = Executors.newFixedThreadPool(5);

            for (BibEntry entry : entries) {
                executorService.execute(new Runnable() {

                    @Override
                    public void run() {
                        Optional<DOI> origDOI = entry.getField(FieldName.DOI).flatMap(DOI::build);
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
}
