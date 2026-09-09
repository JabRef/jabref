package org.jabref.gui.walkthrough.declarative;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Keeps [WalkthroughNodeIds] honest: a constant whose node lost its id resolves to nothing, and
/// the walkthrough step then spots on nothing instead of failing.
class WalkthroughNodeIdsTest {

    private static final Path MAIN = Path.of("src", "main");

    /// The file that declares each constant's id on a node, by constant name.
    private static final Map<String, String> DECLARING_FILES = Map.ofEntries(
            Map.entry("MAIN_TABLE", "java/org/jabref/gui/maintable/MainTable.java"),
            Map.entry("GROUPS_SIDE_PANE", "java/org/jabref/gui/sidepane/GroupsSidePaneComponent.java"),
            Map.entry("GLOBAL_SEARCH_FIELD", "java/org/jabref/gui/search/GlobalSearchBar.java"),
            Map.entry("COLUMNS_LIST", "java/org/jabref/gui/preferences/table/TableTab.java"),
            Map.entry("MAIN_FILE_DIRECTORY_RADIO", "java/org/jabref/gui/preferences/linkedfiles/LinkedFilesTab.java"),
            Map.entry("LINKED_FILE_BROWSE", "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml"),
            Map.entry("LINKED_FILE_DESCRIPTION", "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml"),
            Map.entry("LINKED_FILE_TYPE", "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml"),
            Map.entry("LINKED_FILE_SOURCE_URL", "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml"),
            Map.entry("GROUP_NAME", "resources/org/jabref/gui/groups/GroupDialog.fxml"),
            Map.entry("GROUP_DESCRIPTION", "resources/org/jabref/gui/groups/GroupDialog.fxml"),
            Map.entry("GROUP_EXPLICIT_RADIO", "resources/org/jabref/gui/groups/GroupDialog.fxml"));

    /// Driven by the constants themselves, so a constant added without a declaring file shows up as
    /// a case rather than as a silently missing one.
    private static Stream<Arguments> declarations() throws IllegalAccessException {
        List<Arguments> cases = new ArrayList<>();
        for (Field constant : WalkthroughNodeIds.class.getFields()) {
            cases.add(Arguments.of(constant.getName(), constant.get(null), DECLARING_FILES.get(constant.getName())));
        }
        return cases.stream();
    }

    /// FXML repeats the literal, Java sets the id from the constant — so each declaration is
    /// searched for the form it can take.
    @ParameterizedTest
    @MethodSource("declarations")
    void idIsDeclaredOnANode(String constantName, String id, String declaringFile) throws IOException {
        assertNotNull(declaringFile, constantName + " names no declaring file in DECLARING_FILES");

        Path file = MAIN.resolve(declaringFile);
        String declaration = declaringFile.endsWith(".fxml")
                             ? "id=\"" + id + "\""
                             : "setId(WalkthroughNodeIds." + constantName + ")";
        assertTrue(Files.readString(file).contains(declaration),
                file + " does not contain " + declaration + ", so no node carries the walkthrough id \"" + id + "\"");
    }

    @ParameterizedTest
    @MethodSource("declarations")
    void noStylesheetSelectsById(String constantName, String id, String declaringFile) throws IOException {
        try (Stream<Path> underMain = Files.walk(MAIN)) {
            Iterable<Path> stylesheets = underMain.filter(path -> path.toString().endsWith(".css"))::iterator;
            for (Path stylesheet : stylesheets) {
                assertFalse(Files.readString(stylesheet).contains("#" + id),
                        stylesheet + " selects by the node id \"" + id + "\", which the walkthrough resolves on");
            }
        }
    }
}
