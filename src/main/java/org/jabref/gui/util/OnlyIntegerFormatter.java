package org.jabref.gui.util;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

import javafx.scene.control.TextFormatter;
import javafx.util.converter.IntegerStringConverter;

/**
 * Formatter that only accepts integer.
 * <p>
 * More or less taken from http://stackoverflow.com/a/36749659/873661
 */
public class OnlyIntegerFormatter extends TextFormatter<Integer> {

    public OnlyIntegerFormatter() {
        this(0);
    }

    public OnlyIntegerFormatter(Integer defaultValue) {
        super(new IntegerStringConverter(), defaultValue, new IntegerFilter());
    }

    private static class IntegerFilter implements UnaryOperator<Change> {
        private final static Pattern DIGIT_PATTERN = Pattern.compile("\\d*");

        @Override
        public Change apply(TextFormatter.Change aT) {
            return DIGIT_PATTERN.matcher(aT.getText()).matches() ? aT : null;
        }
    }
}
