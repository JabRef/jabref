package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHistoryTest {
    private FileHistory history;

    @BeforeEach
    void setUp() {
        history = FileHistory.of(List.of());
    }

    @Test
    void newItemsAreAddedInRightOrder() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        assertEquals(Arrays.asList(Path.of("bb"), Path.of("aa")), history);
    }

    @Test
    void itemsAlreadyInListIsMovedToTop() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        history.newFile(Path.of("aa"));
        assertEquals(Arrays.asList(Path.of("aa"), Path.of("bb")), history);
    }

    @Test
    void removeItemsLeavesOtherItemsInRightOrder() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        history.newFile(Path.of("cc"));

        history.removeItem(Path.of("bb"));

        assertEquals(Arrays.asList(Path.of("cc"), Path.of("aa")), history);
    }

    @Test
    void sizeTest() {
        assertEquals(0, history.size());
        history.newFile(Path.of("aa"));
        assertEquals(1, history.size());
        history.newFile(Path.of("bb"));
        assertEquals(2, history.size());
    }

    @Test
    void isEmptyTest() {
        assertTrue(history.isEmpty());
        history.newFile(Path.of("aa"));
        assertFalse(history.isEmpty());
    }

    @Test
    void getFileAtTest() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        history.newFile(Path.of("cc"));
        assertEquals(Path.of("bb"), history.get(1));
    }

    @Test
    void oldestEntryIsDroppedWhenSizeExceedsLimit() {
        for (int i = 1; i <= 8; i++) {
            history.newFile(Path.of("file" + i));
        }
        assertEquals(8, history.size());

        history.newFile(Path.of("file9"));

        assertEquals(8, history.size());
        assertEquals(Path.of("file9"), history.get(0));
        assertFalse(history.contains(Path.of("file1")));
    }
}


