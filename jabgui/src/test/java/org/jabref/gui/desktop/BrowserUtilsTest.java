package org.jabref.gui.desktop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrowserUtilsTest {

    @Test
    void isBrowserSupportingPageJumpRecognizesSupportedBrowsers() {
        assertTrue(BrowserUtils.isBrowserSupportingPageJump("google chrome"));
        assertTrue(BrowserUtils.isBrowserSupportingPageJump("microsoft edge"));
        assertTrue(BrowserUtils.isBrowserSupportingPageJump("safari"));
        assertTrue(BrowserUtils.isBrowserSupportingPageJump("firefox"));
        assertTrue(BrowserUtils.isBrowserSupportingPageJump("brave"));
    }

    @Test
    void isBrowserSupportingPageJumpRejectsUnsupportedBrowsers() {
        assertFalse(BrowserUtils.isBrowserSupportingPageJump("skim"));
        assertFalse(BrowserUtils.isBrowserSupportingPageJump("preview"));
        assertFalse(BrowserUtils.isBrowserSupportingPageJump(""));
        assertFalse(BrowserUtils.isBrowserSupportingPageJump(null));
    }

    @Test
    void browserSpecificChecksRecognizeExpectedApplications() {
        assertTrue(BrowserUtils.isChrome("google chrome"));
        assertFalse(BrowserUtils.isChrome("firefox"));

        assertTrue(BrowserUtils.isEdge("microsoft edge"));
        assertFalse(BrowserUtils.isEdge("safari"));

        assertTrue(BrowserUtils.isSkim("skim"));
        assertFalse(BrowserUtils.isSkim("preview"));
    }
}