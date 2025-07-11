package org.jabref.model.entry.identifier;

import java.net.URI;
import java.util.Optional;

import org.jabref.model.entry.field.Field;

public abstract class CitationCountIdentifier implements  Identifier {

    @Override
    public Field getDefaultField() {
        return null;
    }

}
