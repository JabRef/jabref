package org.jabref.gui.walkthrough.declarative;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Keeps [WalkthroughNodeIds] honest: a constant whose node lost its id resolves to nothing, and
/// the walkthrough step then spots on nothing instead of failing.
class WalkthroughNodeIdsTest {

    private static final Path MAIN = Path.of("src", "main");

    /// Each id and the file that declares it on a node.
    private static Stream<Object[]> declarations() {
        return Map.of(
                          WalkthroughNodeIds.COLUMNS_LIST, "java/org/jabref/gui/preferences/table/TableTab.java",
                          WalkthroughNodeIds.MAIN_FILE_DIRECTORY_RADIO, "java/org/jabref/gui/preferences/linkedfiles/LinkedFilesTab.java",
                          WalkthroughNodeIds.LINKED_FILE_BROWSE, "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml",
                          WalkthroughNodeIds.LINKED_FILE_DESCRIPTION, "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml",
                          WalkthroughNodeIds.LINKED_FILE_TYPE, "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml",
                          WalkthroughNodeIds.LINKED_FILE_SOURCE_URL, "resources/org/jabref/gui/linkedfile/LinkedFileEditDialog.fxml",
                          WalkthroughNodeIds.GROUP_NAME, "resources/org/jabref/gui/groups/GroupDialog.fxml",
                          WalkthroughNodeIds.GROUP_DESCRIPTION, "resources/org/jabref/gui/groups/GroupDialog.fxml",
                          WalkthroughNodeIds.GROUP_EXPLICIT_RADIO, "resources/org/jabref/gui/groups/GroupDialog.fxml")
                  .entrySet().stream()
                  .map(entry -> new Object[] {entry.getKey(), entry.getValue()});
    }

    /// FXML repeats the literal, Java sets the id from the constant — so each declaration is
    /// searched for the form it can take.
    @ParameterizedTest
    @MethodSource("declarations")
    void idIsDeclaredOnANode(String id, String declaringFile) throws IOException {
        Path file = MAIN.resolve(declaringFile);
        String content = Files.readString(file);
        String declaration = declaringFile.endsWith(".fxml")
                             ? "id=\"" + id + "\""
                             : "setId(WalkthroughNodeIds." + constantName(id) + ")";
        assertTrue(content.contains(declaration),
                file + " does not contain " + declaration + ", so no node carries the walkthrough id \"" + id + "\"");
    }

    @ParameterizedTest
    @MethodSource("declarations")
    void noStylesheetSelectsById(String id, String declaringFile) throws IOException {
        try (Stream<Path> resources = Files.walk(MAIN.resolve("resources"))) {
            Iterable<Path> stylesheets = resources.filter(path -> path.toString().endsWith(".css"))::iterator;
            for (Path stylesheet : stylesheets) {
                assertFalse(Files.readString(stylesheet).contains("#" + id),
                        stylesheet + " selects by the node id \"" + id + "\", which exists for walkthrough spotlighting only");
            }
        }
    }

    private static String constantName(String id) {
        return Arrays.stream(WalkthroughNodeIds.class.getFields())
                     .filter(field -> id.equals(fieldValue(field)))
                     .map(Field::getName)
                     .findFirst()
                     .orElseThrow();
    }

    private static Object fieldValue(Field field) {
        try {
            return field.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError(field + " is not readable", e);
        }
    }
}
