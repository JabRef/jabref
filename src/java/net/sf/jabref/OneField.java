package net.sf.jabref;

import java.util.HashMap;

public class OneField {

    private String _name;
    private String _value;
    private int _stringType;
    // enum
    // STRING_NONE : does not have a @string keyword
    // STRING_SIMPLE,: has only One keyword
    // STRING_COMPOSITE: has multiple (string concatenation)
    
    String _simpleString;
    String _compositeLeft;
    String _compositeRight;
}
