package org.jabref.logic.integrity;

public class BracesCorrector {

    public static String apply(String s) {
        if (s == null) {
            return null;
        } else {
            String c = s.replaceAll("\\\\\\{", "").replaceAll("\\\\\\}", "");

            long diff = c.chars().filter(ch -> ch == '{').count() - c.chars().filter(ch -> ch == '}').count();
            while (diff != 0) {
                if (diff < 0) {
                    s = "{" + s;
                    diff++;
                } else {
                    s = s + "}";
                    diff--;
                }
            }
            return s;
        }
    }

}
