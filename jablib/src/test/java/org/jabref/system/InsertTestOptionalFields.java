package org.jabref.system;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.model.entry.field.StandardField;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class InsertTestOptionalFields {

    @Test
    void testOptionalFieldVolume() {
        BibEntry bibEntry = new BibEntry();
        bibEntry.setField(StandardField.VOLUME, "VOl. 1");
        BibDatabase database = new BibDatabase(List.of(bibEntry));
        assertEquals(1, database.getEntryCount()); // entry in the database
        Set <String> expected = bibEntry.getFieldAsWords(StandardField.VOLUME);
        Optional <String> actual = bibEntry.getField(StandardField.VOLUME);
        assertNotEquals(expected, actual, "They are not equal."); // expected output : [1, VOl.]
    }

}
