package org.jabref.gui.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxCssLogFilterTest {

    private final JavaFxCssLogFilter filter = new JavaFxCssLogFilter();

    @Test
    void filtersModenaCssConversionWarnings() {
        String msg = "Caught 'java.lang.ClassCastException: class java.lang.String cannot be cast to class javafx.scene.paint.Paint' "
                + "while converting value for '-fx-background-color' from rule '*.text-input' in stylesheet jar:file:///C:/.../javafx-controls-23.0.1-win.jar!/com/sun/javafx/scene/control/skin/modena/modena.bss";
        LogRecord rec = new LogRecord(Level.WARNING, msg);
        assertFalse(filter.isLoggable(rec));
    }

    @Test
    void filtersBaseCssLookupWarnings() {
        String msg = "Could not resolve '-jr-gray-2' while resolving lookups for '-fx-prompt-text-fill' "
                + "from rule '*.text-input' in stylesheet file:/D:/git-repositories/JabRef/build/resources/main/org/jabref/gui/Base.css";
        LogRecord rec = new LogRecord(Level.WARNING, msg);
        assertFalse(filter.isLoggable(rec));
    }

    @Test
    void passesThroughOtherWarnings() {
        LogRecord other = new LogRecord(Level.WARNING, "Some unrelated warning message");
        assertTrue(filter.isLoggable(other));
    }
}
