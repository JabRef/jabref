package org.jabref.toolkit.converter;

import org.jabref.toolkit.commands.ExtractionMode;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class ExtractionModeConverter extends CaseInsensitiveEnumConverter<ExtractionMode> {
    public ExtractionModeConverter() {
        super(ExtractionMode.class);
    }
}
