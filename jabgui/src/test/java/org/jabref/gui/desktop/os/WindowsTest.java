package org.jabref.gui.desktop.os;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowsTest {

    /// Builds `papers/<name>.pdf` of the given total length. The path separator is the one of the running
    /// platform, which keeps the length the same and the test independent of the platform.
    private static Path pathOfLength(int length) {
        String fileName = "x".repeat(length - "papers".length() - 1 - ".pdf".length()) + ".pdf";
        return Path.of("papers", fileName);
    }

    @Test
    void shortUrlGoesToExplorer() {
        assertEquals(List.of("explorer.exe", "\"https://example.com/\""), Windows.shellOpenCommand("https://example.com/"));
    }

    @Test
    void longUrlGoesToTheProtocolHandler() {
        String url = "https://example.com/?state=" + "x".repeat(300);
        assertEquals(List.of("rundll32.exe", "url.dll,FileProtocolHandler", url), Windows.shellOpenCommand(url));
    }

    @Test
    void argumentOfMaximumUsableLengthGoesToExplorer() {
        String path = "x".repeat(259);
        assertEquals(List.of("explorer.exe", "\"" + path + "\""), Windows.shellOpenCommand(path));
    }

    @Test
    void argumentAtThePathLimitGoesToTheProtocolHandler() {
        String path = "x".repeat(260);
        assertEquals(List.of("rundll32.exe", "url.dll,FileProtocolHandler", path), Windows.shellOpenCommand(path));
    }

    @Test
    void shortPathIsSelectedInExplorer() {
        Path file = pathOfLength(100);
        assertEquals(List.of("explorer.exe", "/select,", file.toString()), Windows.revealCommand(file));
    }

    @Test
    void longPathOpensItsFolderWithoutSelection() {
        assertEquals(List.of("explorer.exe", "\"papers\""), Windows.revealCommand(pathOfLength(260)));
    }

    @Test
    void fileWithoutParentIsSelectedInExplorer() {
        assertEquals(List.of("explorer.exe", "/select,", "paper.pdf"), Windows.revealCommand(Path.of("paper.pdf")));
    }
}
