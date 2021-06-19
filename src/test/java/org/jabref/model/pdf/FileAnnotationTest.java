package org.jabref.model.pdf;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileAnnotationTest {

    @Test
    public void testParseDateMinusBeforeTimezone() {
        String dateString = "D:20170512224019-03'00'";
        LocalDateTime date = FileAnnotation.extractModifiedTime(dateString);
        assertEquals(LocalDateTime.of(2017, 05, 12, 22, 40, 19), date);
    }

    @Test
    public void testParseDatePlusBeforeTimezone() {
        String dateString = "D:20170512224019+03'00'";
        LocalDateTime date = FileAnnotation.extractModifiedTime(dateString);
        assertEquals(LocalDateTime.of(2017, 05, 12, 22, 40, 19), date);
    }

    @Test
    public void testParseDateNoTimezone() {
        String dateString = "D:20170512224019";
        LocalDateTime date = FileAnnotation.extractModifiedTime(dateString);
        assertEquals(LocalDateTime.of(2017, 05, 12, 22, 40, 19), date);
    }

    @Test
    public void testParseNotADate() {
        String dateString = "gsdfgwergsdf";
        LocalDateTime date = FileAnnotation.extractModifiedTime(dateString);
        assertTrue(ChronoUnit.SECONDS.between(LocalDateTime.now(), date) <= 1);
    }

    @Test
    public void testAbbreviateAnnotationName() {
        final FileAnnotation fileAnnotation = new FileAnnotation("John Robertson",
                LocalDateTime.of(2020, 4, 18, 17, 10), 1,
                "this is an annotation that is very long and goes over the character limit of 45",
                FileAnnotationType.FREETEXT, Optional.empty());

        assertEquals("this is an annotation that is very long and g...", fileAnnotation.toString());
    }
}
