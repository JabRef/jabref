package org.jabref.model.openoffice.style;

/**
 * What should createCitationMarker do if it discovers that uniqueLetters provided are not
 * sufficient for unique presentation?
 */
public enum NonUniqueCitationMarker {

    /** Give an insufficient representation anyway.  */
    FORGIVEN,

    /** Throw an exception */
    THROWS
}

