package net.sf.jabref.logic.labelPattern;

import java.util.ArrayList;

import net.sf.jabref.Globals;

public class DatabaseLabelPattern extends AbstractLabelPattern {

    public ArrayList<String> getValue(String key) {
        ArrayList<String> result = data.get(key);
        //  Test to see if we found anything
        if (result == null) {
            // check default value
            result = getDefaultValue();
            if (result == null) {
                // no default value, ask global label pattern
                result = Globals.prefs.getKeyPattern().getValue(key);
            }
        }
        return result;
    }

}
