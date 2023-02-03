package org.jabref.model.entry.identifier;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * This class is mainly used to keep track of eprint parsers, and make it easy to navigate the code.
 * */
public abstract class EprintIdentifier implements Identifier {

    @Override
    public Field getDefaultField() {
        return StandardField.EPRINT;
    }
}
