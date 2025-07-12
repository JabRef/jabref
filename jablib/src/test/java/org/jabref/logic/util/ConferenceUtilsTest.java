package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ConferenceUtilsTest {

    @Test
    void testExtractAcronym() {
        String title = "ACIS Conference on Software Engineering Research (SERA)";
        assertEquals(Optional.of("SERA"), ConferenceUtil.extractAcronym(title));
    }

    @Test
    void testNoAcronym() {
        String title = "Some Conference Without Acronym";
        assertEquals(Optional.empty(), ConferenceUtil.extractAcronym(title));
    }
    @Test
    void test1(){
        String title="International Conference on Software Engineering (ICSE)";
        assertEquals(Optional.of("ICSE"),ConferenceUtil.extractAcronym(title));
    }
    @Test
    void test2(){
        String title="International Conference on Software Engineering";
        assertEquals(Optional.empty(),ConferenceUtil.extractAcronym(title));
    }
}
