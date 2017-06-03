package org.jabref.model.pdf;

import java.time.LocalDateTime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;


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
}
