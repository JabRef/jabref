package net.sf.jabref.performance;

import org.junit.Test;

public class LoadAndSavePerformance {

    @Test
    public void testLoadAndSaveWithGeneratedData() {
        String largeFile = new BibtexEntryGenerator().generateBibtexEntries(99999);



    }
}
