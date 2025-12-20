package org.jabref.gui.logging;

import java.util.Locale;
import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Filters out very noisy JavaFX CSS warnings that are known upstream issues and do not indicate
 * actionable problems for JabRef users. See JDK-8268657 and related JavaFX CSS warnings.
 *
 * <p>We only filter java.util.logging WARN records whose message indicates a CSS lookup/conversion
 * problem and that originate from processing a stylesheet (either JavaFX's modena.bss or
 * JabRef's Base.css). All other log records are passed through unmodified.</p>
 */
public class JavaFxCssLogFilter implements Filter {

    @Override
    public boolean isLoggable(final LogRecord record) {
        if (record == null) {
            return true;
        }

        // Only consider warnings that flood the console
        if (record.getLevel() != Level.WARNING) {
            return true;
        }

        final String message = record.getMessage();
        if (message == null || message.isBlank()) {
            return true;
        }

        final String msg = message.toLowerCase(Locale.ROOT);

        // We only care for messages that clearly refer to CSS stylesheet processing
        final boolean mentionsStylesheet = msg.contains("stylesheet ");
        if (!mentionsStylesheet) {
            return true;
        }

        // Restrict to JavaFX default theme (modena.bss) or JabRef's own Base.css
        final boolean isJavaFxTheme = msg.contains("modena.bss") || msg.contains("javafx-controls");
        final boolean isJabRefBaseCss = msg.contains("/org/jabref/gui/base.css");
        if (!(isJavaFxTheme || isJabRefBaseCss)) {
            return true;
        }

        // Match the noisy CSS issues we want to suppress
        final boolean isCssLookupIssue = msg.contains("could not resolve '");
        final boolean isCssConversionIssue = msg.contains("while converting value for '");

        if (isCssLookupIssue || isCssConversionIssue) {
            return false; // suppress this JUL warning
        }

        // Additionally, some messages include attached exceptions about class cast during CSS parsing
        if (record.getThrown() != null) {
            final String ex = record.getThrown().getClass().getName();
            return !"java.lang.ClassCastException".equals(ex) && !"java.lang.IllegalArgumentException".equals(ex); // suppress CSS parsing exceptions from stylesheets
        }

        return true;
    }
}
