package org.jabref.cli;

import java.util.List;

import javafx.util.Pair;

import org.jabref.logic.os.OS;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KitCommandLineTest {

    @Test
    void alignStringTable() {
        List<Pair<String, String>> given = List.of(
                new Pair<>("Apple", "Slice"),
                new Pair<>("Bread", "Loaf"),
                new Pair<>("Paper", "Sheet"),
                new Pair<>("Country", "County"));

        String expected = """
                Apple   : Slice
                Bread   : Loaf
                Paper   : Sheet
                Country : County
                """.replace("\n", OS.NEWLINE);

        assertEquals(expected, KitCommandLine.alignStringTable(given));
    }
}
