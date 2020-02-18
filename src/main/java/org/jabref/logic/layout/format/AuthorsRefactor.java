package org.jabref.logic.layout.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jabref.logic.layout.AbstractParamLayoutFormatter;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
/**
 * Versatile author name formatter that takes arguments to control the formatting style.
 */
public class AuthorsRefactor extends AbstractParamLayoutFormatter {

    private static boolean[] visited = new boolean[8];

    /*
    AuthorSort = [FirstFirst | LastFirst | LastFirstFirstFirst]
    AuthorAbbr = [FullName | Initials | FirstInitial | MiddleInitial | InitialsNoSpace | LastName]
    AuthorSep = [Comma | And | Colon | Semicolon | Sep=<string>]
    AuthorLastSep = [And | Comma | Colon | Semicolon | Amp | Oxford | LastSep=<string>]
    AuthorPunc = [FullPunc | NoPunc | NoComma | NoPeriod]
    AuthorNumber = [inf | <number>]
    AuthorNumberEtAl = [ {1} | <number>]
    EtAlString = [ et al. | EtAl=<string>]
     */

    private static final List<String> AUTHOR_ORDER = new ArrayList<>();
    private static final List<String> AUTHOR_ABRV = new ArrayList<>();
    private static final List<String> AUTHOR_PUNC = new ArrayList<>();
    private static final List<String> SEPARATORS = new ArrayList<>();
    private static final List<String> LAST_SEPARATORS = new ArrayList<>();

    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+");

    static {
        AuthorsRefactor.AUTHOR_ORDER.add("firstfirst");
        AuthorsRefactor.AUTHOR_ORDER.add("lastfirst");
        AuthorsRefactor.AUTHOR_ORDER.add("lastfirstfirstfirst");

        AuthorsRefactor.AUTHOR_ABRV.add("fullname");
        AuthorsRefactor.AUTHOR_ABRV.add("initials");
        AuthorsRefactor.AUTHOR_ABRV.add("firstinitial");
        AuthorsRefactor.AUTHOR_ABRV.add("middleinitial");
        AuthorsRefactor.AUTHOR_ABRV.add("lastname");
        AuthorsRefactor.AUTHOR_ABRV.add("initialsnospace");

        AuthorsRefactor.AUTHOR_PUNC.add("fullpunc");
        AuthorsRefactor.AUTHOR_PUNC.add("nopunc");
        AuthorsRefactor.AUTHOR_PUNC.add("nocomma");
        AuthorsRefactor.AUTHOR_PUNC.add("noperiod");

        AuthorsRefactor.SEPARATORS.add("comma");
        AuthorsRefactor.SEPARATORS.add("and");
        AuthorsRefactor.SEPARATORS.add("colon");
        AuthorsRefactor.SEPARATORS.add("semicolon");
        AuthorsRefactor.SEPARATORS.add("sep");

        AuthorsRefactor.LAST_SEPARATORS.add("and");
        AuthorsRefactor.LAST_SEPARATORS.add("colon");
        AuthorsRefactor.LAST_SEPARATORS.add("semicolon");
        AuthorsRefactor.LAST_SEPARATORS.add("amp");
        AuthorsRefactor.LAST_SEPARATORS.add("oxford");
        AuthorsRefactor.LAST_SEPARATORS.add("lastsep");

    }

    private static final int
    FIRST_FIRST = 0;
    private static final int LAST_FIRST = 1;
    private static final int LF_FF = 2;

    private static final String
    COMMA = ", ";
    private static final String AMP = " & ";
    private static final String COLON = ": ";
    private static final String SEMICOLON = "; ";
    private static final String AND = " and ";
    private static final String OXFORD = ", and ";

    private int flMode;

    private boolean
    abbreviate = true;
    private boolean firstInitialOnly;
    private boolean middleInitial;
    private boolean lastNameOnly;
    private boolean abbrDots = true;
    private boolean abbrSpaces = true;

    private boolean setSep;
    private boolean setMaxAuthors;
    private int maxAuthors = -1;
    private int authorNumberEtAl = 1;

    private String lastFirstSeparator = ", ";
    private String separator = AuthorsRefactor.COMMA;
    private String lastSeparator = AuthorsRefactor.AND;
    private String etAlString = " et al.";

    @Override
    public void setArgument(String arg) {
        List<String> parts = AbstractParamLayoutFormatter.parseArgument(arg);
        for (String part : parts) {
            int index = part.indexOf('=');
            if (index > 0) {
                String key = part.substring(0, index);
                String value = part.substring(index + 1);
                handleArgument(key, value);
            } else {
                handleArgument(part, "");
            }

        }
    }

    private void handleOrder(String key) {
        if (comp(key, "FirstFirst")) {
            flMode = AuthorsRefactor.FIRST_FIRST;
        } else if (comp(key, "LastFirst")) {
            flMode = AuthorsRefactor.LAST_FIRST;
        } else if (comp(key, "LastFirstFirstFirst")) {
            flMode = AuthorsRefactor.LF_FF;
        }        
    }

    private void handleAbrv(String key) {
        if (comp(key, "FullName")) {
            abbreviate = false;
        } else if (comp(key, "Initials")) {
            abbreviate = true;
            firstInitialOnly = false;
        } else if (comp(key, "FirstInitial")) {
            abbreviate = true;
            firstInitialOnly = true;
        } else if (comp(key, "MiddleInitial")) {
            abbreviate = true;
            middleInitial = true;
        } else if (comp(key, "LastName")) {
            lastNameOnly = true;
        } else if (comp(key, "InitialsNoSpace")) {
            abbreviate = true;
            abbrSpaces = false;
        }        
    }

    private void handlePunc(String key) {
        if (comp(key, "FullPunc")) {
            abbrDots = true;
            lastFirstSeparator = ", ";
        } else if (comp(key, "NoPunc")) {
            abbrDots = false;
            lastFirstSeparator = " ";
        } else if (comp(key, "NoComma")) {
            abbrDots = true;
            lastFirstSeparator = " ";
        } else if (comp(key, "NoPeriod")) {
            abbrDots = false;
            lastFirstSeparator = ", ";
        }
    }

    private void handleSeparater(String key, String value) {
        if (comp(key, "Comma")) {
            if (setSep) {
                lastSeparator = AuthorsRefactor.COMMA;
            } else {
                separator = AuthorsRefactor.COMMA;
                setSep = true;
            }
        } else if (comp(key, "And")) {
            if (setSep) {
                lastSeparator = AuthorsRefactor.AND;
            } else {
                separator = AuthorsRefactor.AND;
                setSep = true;
            }
        } else if (comp(key, "Colon")) {
            if (setSep) {
                lastSeparator = AuthorsRefactor.COLON;
            } else {
                separator = AuthorsRefactor.COLON;
                setSep = true;
            }
        } else if (comp(key, "Semicolon")) {
            if (setSep) {
                lastSeparator = AuthorsRefactor.SEMICOLON;
            } else {
                separator = AuthorsRefactor.SEMICOLON;
                setSep = true;
            }
        } else if (comp(key, "Oxford")) {
            lastSeparator = AuthorsRefactor.OXFORD;
        } else if (comp(key, "Amp")) {
            lastSeparator = AuthorsRefactor.AMP;
        } else if (comp(key, "Sep") && !value.isEmpty()) {
            separator = value;
            setSep = true;
        } else if (comp(key, "LastSep") && !value.isEmpty()) {
            lastSeparator = value;
        }
    }

    private void handleNumberPattern(String key) {
        // Just a number:
        int num = Integer.parseInt(key.trim());
        if (setMaxAuthors) {
            authorNumberEtAl = num;
        } else {
            maxAuthors = num;
            setMaxAuthors = true;
        }
    }

    private void handleArgument(String key, String value) {
        visited[0] = true;
        if (AuthorsRefactor.AUTHOR_ORDER.contains(key.trim().toLowerCase(Locale.ROOT))) {
            visited[1] = true;
            handleOrder(key);
        } else if (AuthorsRefactor.AUTHOR_ABRV.contains(key.trim().toLowerCase(Locale.ROOT))) {
            visited[2] = true;
            handleAbrv(key);
        } else if (AuthorsRefactor.AUTHOR_PUNC.contains(key.trim().toLowerCase(Locale.ROOT))) {
            visited[3] = true;
            handlePunc(key);
        } else if (AuthorsRefactor.SEPARATORS.contains(key.trim().toLowerCase(Locale.ROOT)) || AuthorsRefactor.LAST_SEPARATORS.contains(key.trim().toLowerCase(Locale.ROOT))) {
            visited[4] = true;
            handleSeparater(key, value);
        } else if ("etal".equalsIgnoreCase(key.trim())) {
            visited[5] = true;
            etAlString = value;
        } else if (AuthorsRefactor.NUMBER_PATTERN.matcher(key.trim()).matches()) {
            visited[6] = true;
            handleNumberPattern(key);
        }
        else {
            visited[7] = true;
        }

        try {
            File directory = new File("/Temp");
            if (!directory.exists()){
                directory.mkdir();
            }
            File f = new File(directory + "/handleArgumentRefactor.txt");

            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            double frac = 0;
            for(int i = 0; i < visited.length; ++i) {
                frac += (visited[i] ? 1 : 0);
                bw.write("branch " + i + " was " + (visited[i] ? " visited." : " not visited.") + "\n");
            }

            bw.write("" + frac/visited.length);
            bw.close();
        } catch (Exception e) {
            System.err.println("Did not find the path");
        }
    }

    /**
     * Check for case-insensitive equality between two strings after removing
     * white space at the beginning and end of the first string.
     * @param one The first string - whitespace is trimmed
     * @param two The second string
     * @return true if the strings are deemed equal
     */
    private static boolean comp(String one, String two) {
        return one.trim().equalsIgnoreCase(two);
    }

    @Override
    public String format(String fieldText) {
        if (fieldText == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        AuthorList al = AuthorList.parse(fieldText);

        if ((maxAuthors < 0) || (al.getNumberOfAuthors() <= maxAuthors)) {
            for (int i = 0; i < al.getNumberOfAuthors(); i++) {
                Author a = al.getAuthor(i);

                addSingleName(sb, a, (flMode == AuthorsRefactor.FIRST_FIRST) || ((flMode == AuthorsRefactor.LF_FF) && (i > 0)));

                if (i < (al.getNumberOfAuthors() - 2)) {
                    sb.append(separator);
                } else if (i < (al.getNumberOfAuthors() - 1)) {
                    sb.append(lastSeparator);
                }
            }
        } else {
            for (int i = 0; i < Math.min(al.getNumberOfAuthors() - 1, authorNumberEtAl); i++) {
                if (i > 0) {
                    sb.append(separator);
                }
                addSingleName(sb, al.getAuthor(i), flMode == AuthorsRefactor.FIRST_FIRST);
            }
            sb.append(etAlString);
        }

        return sb.toString();
    }

    private void addSingleName(StringBuilder sb, Author a, boolean firstFirst) {
        StringBuilder lastNameSB = new StringBuilder();
        a.getVon().filter(von -> !von.isEmpty()).ifPresent(von -> lastNameSB.append(von).append(' '));
        a.getLast().ifPresent(lastNameSB::append);
        String jrSeparator = " ";
        a.getJr().filter(jr -> !jr.isEmpty()).ifPresent(jr -> lastNameSB.append(jrSeparator).append(jr));

        String firstNameResult = "";
        if (a.getFirst().isPresent()) {
            if (abbreviate) {
                firstNameResult = a.getFirstAbbr().orElse("");

                if (firstInitialOnly && (firstNameResult.length() > 2)) {
                    firstNameResult = firstNameResult.substring(0, 2);
                } else if (middleInitial) {
                    String abbr = firstNameResult;
                    firstNameResult = a.getFirst().get();
                    int index = firstNameResult.indexOf(' ');
                    //System.out.println(firstNamePart);
                    //System.out.println(index);
                    if (index >= 0) {
                        firstNameResult = firstNameResult.substring(0, index + 1);
                        if (abbr.length() > 3) {
                            firstNameResult = firstNameResult + abbr.substring(3);
                        }
                    }
                }
                if (!abbrDots) {
                    firstNameResult = firstNameResult.replace(".", "");
                }
                if (!abbrSpaces) {
                    firstNameResult = firstNameResult.replace(" ", "");
                }
            } else {
                firstNameResult = a.getFirst().get();
            }
        }

        if (lastNameOnly || (firstNameResult.isEmpty())) {
            sb.append(lastNameSB);
        } else if (firstFirst) {
            String firstFirstSeparator = " ";
            sb.append(firstNameResult).append(firstFirstSeparator);
            sb.append(lastNameSB);
        } else {
            sb.append(lastNameSB).append(lastFirstSeparator).append(firstNameResult);
        }

    }
}
