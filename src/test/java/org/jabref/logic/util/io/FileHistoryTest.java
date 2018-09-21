package org.jabref.logic.util.io;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileHistoryTest {
    private FileHistory history;

    @BeforeEach
    void setUp() {
        history = new FileHistory(new ArrayList<>());
    }

    @Test
    void newItemsAreAddedInRightOrder() {
        history.newFile(Paths.get("aa"));
        history.newFile(Paths.get("bb"));
        assertEquals(Arrays.asList(Paths.get("bb"), Paths.get("aa")), history.getHistory());
    }

    @Test
    void itemsAlreadyInListIsMovedToTop() {
        history.newFile(Paths.get("aa"));
        history.newFile(Paths.get("bb"));
        history.newFile(Paths.get("aa"));
        assertEquals(Arrays.asList(Paths.get("aa"), Paths.get("bb")), history.getHistory());
    }

    @Test
    void removeItemsLeavesOtherItemsInRightOrder() {
        history.newFile(Paths.get("aa"));
        history.newFile(Paths.get("bb"));
        history.newFile(Paths.get("cc"));

        history.removeItem(Paths.get("bb"));

        assertEquals(Arrays.asList(Paths.get("cc"), Paths.get("aa")), history.getHistory());
    }
}
