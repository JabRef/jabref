package org.jabref.gui.desktop.os;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowsTest {

    @Test
    void shortUrlGoesToExplorer() {
        assertEquals(List.of("explorer.exe", "\"https://example.com/\""), Windows.urlOpenCommand("https://example.com/"));
    }

    @Test
    void longUrlGoesToTheProtocolHandler() {
        String url = "https://example.com/?state=" + "x".repeat(300);
        assertEquals(List.of("rundll32.exe", "url.dll,FileProtocolHandler", url), Windows.urlOpenCommand(url));
    }
}
