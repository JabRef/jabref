package org.jabref.logic.integrity;

public class BracesCorrector {

    public static String apply(String s) {
        if (s == null) {
            return null;
        } else {
            String addedBraces = s;
            String c = addedBraces.replaceAll("\\\\\\{", "").replaceAll("\\\\\\}", "");

            long diff = c.chars().filter(ch -> ch == '{').count() - c.chars().filter(ch -> ch == '}').count();
            while (diff != 0) {
                if (diff < 0) {
                    addedBraces = "{" + addedBraces;
                    diff++;
                } else {
                    addedBraces = addedBraces + "}";
                    diff--;
                }
            }
            return addedBraces;
        }
    }

}
