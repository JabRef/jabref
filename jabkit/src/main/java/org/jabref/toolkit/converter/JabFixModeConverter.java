package org.jabref.toolkit.converter;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;
import org.jabref.logic.lint.JabFixMode;

import org.jspecify.annotations.NullMarked;
import picocli.CommandLine;

/// Reads a [JabFixMode] by the key a library's configuration names it with, not by its constant name.
@NullMarked
public class JabFixModeConverter implements CommandLine.ITypeConverter<JabFixMode> {
    @Override
    public JabFixMode convert(String value) {
        return JabFixMode.fromKey(value).orElseThrow(() -> new CommandLine.TypeConversionException(
                Localization.lang("Unknown mode: %0. Available modes: %1", value, availableModes())));
    }

    /// The key of every mode, for a message that tells the user what to type.
    public static String availableModes() {
        return Arrays.stream(JabFixMode.values()).map(JabFixMode::getKey).collect(Collectors.joining(", "));
    }
}
