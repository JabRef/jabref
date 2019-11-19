package org.jabref.logic.importer.util;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GrobidServiceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GrobidServiceTest.class);
    
    @Test
    public void testConnectionReturn() {
        //TODO: THE CURRENT VERSION IS FOR TESTING PURPOSES ONLY
        try {
            String response = GrobidService.processCitation("Derwing, T. M., Rossiter, M. J., & Munro, M. J. (2002). Teaching native speakers to listen to foreign-accented speech. Journal of Multilingual and Multicultural Development, 23(4), 245-259.", 1);
            LOGGER.debug(response);
        } catch (Exception e) {
            LOGGER.debug("does not work");
        }
    }

}
