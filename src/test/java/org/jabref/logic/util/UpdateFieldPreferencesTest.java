package org.jabref.logic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class UpdateFieldPreferencesTest {

    UpdateFieldPreferences sut;

    @Test
    public void givenUpdateFieldPreferences_whenNewEntry_thenVerifyColumnValues() {
        sut = new UpdateFieldPreferences(true, true, "Owner", true, true, "Time", "Format");
        assertEquals(true, sut.isUseOwner());
        assertEquals(true, sut.isOverwriteOwner());
        assertEquals("Owner", sut.getDefaultOwner());
        assertEquals(true, sut.isUseTimeStamp());
        assertEquals(true, sut.isOverwriteTimeStamp());
        assertEquals("Time", sut.getTimeStampField());
        assertEquals("Format", sut.getTimeStampFormat());
    }
}
