package org.jabref.gui.desktop;

public final class BrowserUtils {

    private static final String BROWSER_CHROME = "chrome";
    private static final String BROWSER_EDGE = "edge";
    private static final String BROWSER_SAFARI = "safari";
    private static final String BROWSER_FIREFOX = "firefox";
    private static final String BROWSER_BRAVE = "brave";
    private static final String BROWSER_SKIM = "skim";

    private BrowserUtils() {
    }

    public static boolean isBrowserSupportingPageJump(String appNameLower) {
        return isChrome(appNameLower)
                || isEdge(appNameLower)
                || contains(appNameLower, BROWSER_SAFARI)
                || contains(appNameLower, BROWSER_FIREFOX)
                || contains(appNameLower, BROWSER_BRAVE);
    }

    public static boolean isChrome(String appNameLower) {
        return contains(appNameLower, BROWSER_CHROME);
    }

    public static boolean isEdge(String appNameLower) {
        return contains(appNameLower, BROWSER_EDGE);
    }

    public static boolean isSkim(String appNameLower) {
        return contains(appNameLower, BROWSER_SKIM);
    }

    private static boolean contains(String appNameLower, String browserName) {
        return appNameLower != null && appNameLower.contains(browserName);
    }
}
