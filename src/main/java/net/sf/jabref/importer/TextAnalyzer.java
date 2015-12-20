/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.importer;

import java.util.TreeSet;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import net.sf.jabref.model.entry.BibEntry;

class TextAnalyzer {

    private final BibEntry be = null;

    private static final Log LOGGER = LogFactory.getLog(TextAnalyzer.class);

    // Needs to give a year definitely in the future.
    // Used for guessing the
    // year field when parsing textual data. :-)
    private static final int FUTURE_YEAR = 2050;

    public TextAnalyzer(String text) {
        guessBibtexFields(text);
    }

    public BibEntry getEntry() {
        return be;
    }

    private void guessBibtexFields(String text) {

        TreeSet<Substring> usedParts = new TreeSet<>();

        text = "  " + text + "  ";

        String[] split;

        // Look for the year:
        String year = null;
        String yearRx = "(\\s|\\()\\d\\d\\d\\d(\\.|,|\\))";
        String[] cand = getMatches(text, yearRx);
        if (cand.length == 1) {
            // Only one four-digit number, so we guess that is the year.
            year = clean(cand[0]);
            int pos = text.indexOf(year);
            usedParts.add(new Substring("year", pos, pos + year.length()));
            LOGGER.debug("Guessing 'year': '" + year + "'");
        } else if (cand.length > 1) {
            // More than one four-digit numbers, so we look for one giving a reasonable year:

            int good = -1;
            int yearFound = -1;
            for (int i = 0; i < cand.length; i++) {
                int number = Integer.parseInt(cand[i].trim());
                if (number == yearFound) {
                    continue;
                }
                if (number < 2500) {
                    if (good == -1) {
                        good = i;
                        yearFound = number;
                    } else {
                        // More than one found. Be a bit more specific.
                        if ((yearFound < FUTURE_YEAR) && (number < FUTURE_YEAR)) {
                            good = -1;
                            break; // Give up, both seem good enough.
                        } else if ((yearFound >= FUTURE_YEAR) && (number < FUTURE_YEAR)) {
                            good = i;
                            yearFound = number;
                        }
                    }
                }
            }
            if (good >= 0) {
                year = clean(cand[good]);
                int pos = text.indexOf(year);
                usedParts.add(new Substring("year", pos, pos + year.length()));
                LOGGER.debug("Guessing 'year': '" + year + "'");
            }
        }

        // Look for Pages:
        String pages;
        String pagesRx = "\\s(\\d{1,4})( ??)-( ??)(\\d{1,4})(\\.|,|\\s)";
        cand = getMatches(text, pagesRx);
        if (cand.length == 1) {
            pages = clean(cand[0].replaceAll("-|( - )", "--"));
            int pos = text.indexOf(cand[0]);
            usedParts.add(new Substring("pages", pos, pos + year.length()));
            LOGGER.debug("Guessing 'pages': '" + pages + "'");
        } else if (cand.length > 1) {
            int found = -1;
            for (int i = 0; i < cand.length; i++) {
                split = clean(cand[i].replaceAll("\\s", "")).split("-");
                //   Util.pr("Pg: "+pages);
                int first = Integer.parseInt(split[0]);
                int second = Integer.parseInt(split[1]);
                if ((second - first) > 3) {
                    found = i;
                    break;
                }
            }
            if (found >= 0) {
                pages = clean(cand[found].replaceAll("-|( - )", "--"));
                int pos = text.indexOf(cand[found]);
                LOGGER.debug("Guessing 'pages': '" + pages + "'");
                usedParts.add(new Substring("pages", pos, pos + pages.length()));
            }
        }

        //String journalRx = "(\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((vol\\.|Vol\\.|Volume|volume))??(.??)(\\d{1,3})(\\.|,|\\s)";
        String journal;
        String volume;
        String journalRx = "(,|\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((.){0,2})((vol\\.|Vol\\.|Volume|volume))??\\s??(\\d{1,3})(\\.|,|\\s|:)";
        cand = getMatches(text, journalRx);
        if (cand.length > 0) {
            //Util.pr("guessing 'journal': '" + cand[0] + "'");
            cand[0] = cand[0].trim();
            int pos = cand[0].lastIndexOf(' ');
            if (pos > 0) {
                volume = clean(cand[0].substring(pos + 1));
                LOGGER.debug("Guessing 'volume': '" + volume + "'");
                journal = clean(cand[0].substring(0, pos));
                //Util.pr("guessing 'journal': '" + journal + "'");
                pos = journal.lastIndexOf(' ');
                if (pos > 0) {
                    String last = journal.substring(pos + 1).toLowerCase();
                    if ("volume".equals(last) || "vol".equals(last) || "v".equals(last)) {
                        journal = clean(journal.substring(0, pos));
                    }
                }
                pos = text.indexOf(journal);
                usedParts.add(new Substring("journal", pos, pos + journal.length()));
                LOGGER.debug("Guessing 'journal': '" + journal + "'");
            }
            //Util.pr("Journal? '"+cand[0]+"'");
        } else {
            // No journal found. Maybe the year precedes the volume? Try another regexp:
        }

        // Then try to find title and authors.
        Substring ss;
        Vector<String> free = new Vector<>();
        int piv = 0;
        for (Substring usedPart : usedParts) {
            ss = usedPart;
            if ((ss.begin() - piv) > 10) {
                LOGGER.debug("... " + text.substring(piv, ss.begin()));
                free.add(clean(text.substring(piv, ss.begin())));
            }
            piv = ss.end();
        }
        if ((text.length() - piv) > 10) {
            free.add(clean(text.substring(piv)));
        }
        LOGGER.debug("Free parts:");
        for (String s : free) {
            LOGGER.debug(": '" + s + "'");
        }
    }

    private static String[] getMatches(String text, String regexp) {
        int piv = 0;
        String[] test = text.split(regexp);
        if (test.length < 2) {
            return new String[0];
        }

        String[] out = new String[test.length - 1];
        for (int i = 0; i < out.length; i++) {
            String[] curr = text.split(regexp, i + 2);
            out[i] = text.substring(piv + curr[i].length(), text.length() - curr[i + 1].length());
            piv += curr[i].length() + out[i].length();
            //Util.pr("--"+out[i]+"\n-> "+piv);
        }
        return out;
    }

    private static String clean(String s) {
        boolean found = false;
        int left = 0;
        int right = s.length() - 1;
        while (!found && (left < s.length())) {
            char c = s.charAt(left);
            if (Character.isWhitespace(c) || (c == '.') || (c == ',') || (c == '(')
                    || (c == ':') || (c == ')')) {
                left++;
            } else {
                found = true;
            }
        }
        found = false;
        while (!found && (right > left)) {
            char c = s.charAt(right);
            if (Character.isWhitespace(c) || (c == '.') || (c == ',') || (c == ')')
                    || (c == ':') || (c == '(')) {
                right--;
            } else {
                found = true;
            }
        }
        //Util.pr(s+"\n"+left+" "+right);
        return s.substring(left, Math.min(right + 1, s.length()));
    }


    private static class Substring implements Comparable<Substring> {

        final int begin;
        final int end;


        public Substring(String name, int begin, int end) {
            this.begin = begin;
            this.end = end;
        }

        public int begin() {
            return begin;
        }

        public int end() {
            return end;
        }

        @Override
        public int compareTo(Substring other) {
            return Integer.valueOf(begin).compareTo(other.begin());
        }
    }
}
