/*
 * Created on 01.12.2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package net.sf.jabref.imports;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.OutputPrinter;

public class ImportInspectionCommandLine implements ImportInspector {

    List<BibtexEntry> entries = new LinkedList<BibtexEntry>();

    public void addEntry(BibtexEntry entry) {
        entries.add(entry);
    }

    public void setProgress(int current, int max) {
        status.setStatus(Globals.lang("Progress: %0 of %1", String.valueOf(current), String
            .valueOf(max)));
    }
    
    OutputPrinter status = new OutputPrinter() {

        public void setStatus(String s) {
            System.out.println(s);
        }

        public void showMessage(Object message, String title, int msgType) {
            System.out.println(title + ": " + message);
        }

        public void showMessage(String message) {
            System.out.println(message);
        }
    };

    public Collection<BibtexEntry> query(final String query, final EntryFetcher fetcher) {

        FutureTask<Boolean> future = new FutureTask<Boolean>(new Callable<Boolean>() {

            public Boolean call() throws Exception {
                return fetcher.processQuery(query, ImportInspectionCommandLine.this, status);
            }
        });

        new Thread(future).start();

        try {
            if (future.get()) {
                return entries;
            }
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }
        return null;
    }
}
