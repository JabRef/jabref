package org.jabref.logic.util.io;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileHistoryTest {
    private FileHistory history;

    @BeforeEach
    void setUp() {
        history = new FileHistory(new ArrayList<>());
    }

    @Test
    void newItemsAreAddedInRightOrder() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        assertEquals(Arrays.asList(Path.of("bb"), Path.of("aa")), history.getHistory());
    }

    @Test
    void itemsAlreadyInListIsMovedToTop() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        history.newFile(Path.of("aa"));
        assertEquals(Arrays.asList(Path.of("aa"), Path.of("bb")), history.getHistory());
    }

    @Test
    void removeItemsLeavesOtherItemsInRightOrder() {
        history.newFile(Path.of("aa"));
        history.newFile(Path.of("bb"));
        history.newFile(Path.of("cc"));

        history.removeItem(Path.of("bb"));

        assertEquals(Arrays.asList(Path.of("cc"), Path.of("aa")), history.getHistory());
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
        assertEquals(Path.of("bb"), history.getFileAt(1));
    }
}


