package org.jabref.logic.cleanup;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileFieldCleanupUpdaterTest {

    @Test
    void updateFileFieldReturnsNoChangeIfFileFieldIsUnchanged() {
        BibEntry entry = new BibEntry();
        List<LinkedFile> files = List.of(new LinkedFile("", Path.of("paper.pdf"), "PDF"));
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(files));

        AtomicBoolean schedulerCalled = new AtomicBoolean(false);
        List<FieldChange> changes = FileFieldCleanupUpdater.updateFileField(entry, files, mutation -> {
            schedulerCalled.set(true);
            mutation.run();
        });

        assertEquals(List.of(), changes);
        assertFalse(schedulerCalled.get());
    }

    @Test
    void updateFileFieldSchedulesMutationAndReturnsChangeIfFileFieldDiffers() {
        BibEntry entry = new BibEntry();
        entry.setField(StandardField.FILE, FileFieldWriter.getStringRepresentation(List.of(new LinkedFile("", Path.of("old.pdf"), "PDF"))));

        List<LinkedFile> files = List.of(new LinkedFile("", Path.of("new.pdf"), "PDF"));

        AtomicBoolean schedulerCalled = new AtomicBoolean(false);
        List<FieldChange> changes = FileFieldCleanupUpdater.updateFileField(entry, files, mutation -> {
            schedulerCalled.set(true);
            mutation.run();
        });

        assertTrue(schedulerCalled.get());
        assertEquals(1, changes.size());
        assertEquals(StandardField.FILE, changes.getFirst().getField());
        assertEquals(FileFieldWriter.getStringRepresentation(files), entry.getField(StandardField.FILE).orElseThrow());
    }
}
