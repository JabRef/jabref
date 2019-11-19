package org.jabref.logic.importer.util;

import org.junit.jupiter.api.Test;

public class GrobidServiceTest {

    @Test
    public void testConnectionReturn() {
        //TODO: THE CURRENT VERSION IS FOR TESTING PURPOSES ONLY
        try {
            String response = GrobidService.processCitation("Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching native speakers to listen to foreign-accented speech. Journal of Multilingual and Multicultural Development, 23(4), 245-259.", 1);
            System.out.println(".");
            System.out.println(response);
        } catch (Exception e) {
            System.out.println("Does not work");
            System.out.println(e.getMessage());
            System.out.println(e.getLocalizedMessage());
        }
    }

}
