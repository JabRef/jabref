package org.jabref.logic.openoffice.oocsltext;

import org.jspecify.annotations.NullMarked;

@NullMarked
public class MissingStyleDefinedCitationLabelException extends Exception {

    public MissingStyleDefinedCitationLabelException() {
        super("The selected BST style does not define any citation format for style-defined citations.");
    }
}
