package org.jabref.logic.layout;

import org.jabref.logic.cleanup.Formatter;

/**
 * When having to use a LayoutFormatter as Formatter, this class is helpful. One usecase is {@link org.jabref.logic.cleanup.FieldFormatterCleanup}
 */
public class LayoutFormatterBasedFormatter extends Formatter {

    private final LayoutFormatter layoutFormatter;

    public LayoutFormatterBasedFormatter(LayoutFormatter layoutFormatter) {
        this.layoutFormatter = layoutFormatter;
    }

    @Override
    public String getName() {
        return layoutFormatter.getClass().getName();
    }

    @Override
    public String getKey() {
        return layoutFormatter.getClass().getName();
    }

    @Override
    public String format(String value) {
        return layoutFormatter.format(value);
    }

    @Override
    public String getDescription() {
        return layoutFormatter.getClass().getName();
    }

    @Override
    public String getExampleInput() {
        return layoutFormatter.getClass().getName();
    }
}
