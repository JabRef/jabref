package net.sf.jabref.logic.labelPattern;

import java.util.ArrayList;

public class GlobalLabelPattern extends AbstractLabelPattern {

    public ArrayList<String> getValue(String key) {
        ArrayList<String> result = data.get(key);
        //  Test to see if we found anything
        if (result == null) {
            // check default value
            result = getDefaultValue();
            if (result == null) {
                // we are the "last" to ask
                // we don't have anything left
                // return the global default pattern
                return LabelPatternUtil.DEFAULT_LABELPATTERN;
            }
        }
        return result;
    }

}
