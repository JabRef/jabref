package org.jabref.toolkit.converter;

import org.jabref.toolkit.commands.ExtractionMode;

public class ExtractionModeConverter extends CaseInsensitiveEnumConverter<ExtractionMode> {
    public ExtractionModeConverter() {
        super(ExtractionMode.class);
    }
}
