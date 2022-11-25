package org.jabref.logic.layout.format;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.layout.LayoutFormatter;

/**
 * A layout formatter that is the composite of the given Formatters executed in order.
 */
public class CompositeFormat implements LayoutFormatter {

    private final List<LayoutFormatter> formatters;

    /**
     * If called with this constructor, this formatter does nothing.
     */
    public CompositeFormat() {
        formatters = Collections.emptyList();
    }

    public CompositeFormat(LayoutFormatter first, LayoutFormatter second) {
        formatters = Arrays.asList(first, second);
    }

    public CompositeFormat(LayoutFormatter[] formatters) {
        this.formatters = Arrays.asList(formatters);
    }

    @Override
    public String format(String fieldText) {
        String result = fieldText;
        for (LayoutFormatter formatter : formatters) {
            result = formatter.format(result);
        }
        return result;
    }
}
