package net.sf.jabref.export.layout.format;

import net.sf.jabref.AuthorList;
import net.sf.jabref.export.layout.AbstractParamLayoutFormatter;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * Versatile author name formatter that takes arguments to control the formatting style.
 */
public class Authors extends AbstractParamLayoutFormatter {

    /*
    AuthorSort = [FirstFirst | LastFirst | LastFirstFirstFirst]
    AuthorAbbr = [FullName | Initials | FirstInitial | MiddleInitial | InitialsNoSpace | LastName]
    AuthorSep = [Comma | And | Colon | Semicolon | Sep=<string>]
    AuthorLastSep = [And | Comma | Colon | Semicolon | Amp | Oxford | LastSep=<string>]
    AuthorPunc = [FullPunc | NoPunc | NoComma | NoPeriod]
    AuthorNumber = [inf | <number>]
    EtAlString = [ et al. | EtAl=<string>]
    */
    
    static ArrayList<String>
        authorOrder = new ArrayList<String>(),
        authorAbbr = new ArrayList<String>(),
        authorPunc = new ArrayList<String>(),
        separators = new ArrayList<String>(),
        lastSeparators = new ArrayList<String>();

    static Pattern numberPattern = Pattern.compile("[0-9]+");

    static {
        authorOrder.add("FirstFirst");
        authorOrder.add("LastFirst");
        authorOrder.add("LastFirstFirstFirst");

        authorAbbr.add("FullName");
        authorAbbr.add("Initials");
        authorAbbr.add("FirstInitial");
        authorAbbr.add("MiddleInitial");
        authorAbbr.add("LastName");
        authorAbbr.add("InitialsNoSpace");

        authorPunc.add("FullPunc");
        authorPunc.add("NoPunc");
        authorPunc.add("NoComma");
        authorPunc.add("NoPeriod");

        separators.add("Comma");
        separators.add("And");
        separators.add("Colon");
        separators.add("Semicolon");
        separators.add("Sep");
        
        lastSeparators.add("And");
        lastSeparators.add("Colon");
        lastSeparators.add("Semicolon");
        lastSeparators.add("Amp");
        lastSeparators.add("Oxford");
        lastSeparators.add("LastSep");

    }

    final static int
        FIRST_FIRST = 0,
        LAST_FIRST = 1,
        LF_FF = 2;

    final static String
        COMMA = ", ",
        AMP = " & ",
        COLON = ": ",
        SEMICOLON = "; ",
        AND = " and ",
        OXFORD = ", and ";

    int flMode = FIRST_FIRST;

    boolean
        abbreviate = true,
        firstInitialOnly = false,
        middleInitial = false,
        lastNameOnly = false,
        abbrDots = true,
        abbrSpaces = true;

    boolean setSep = false;

    int maxAuthors = -1;

    String
        firstFirstSeparator = " ",
        lastFirstSeparator = ", ",
        separator = COMMA,
        lastSeparator = AND,
        etAlString = " et al.",
        jrSeparator = " ";


    public void setArgument(String arg) {
        String[] parts = parseArgument(arg);
        for (int i = 0; i < parts.length; i++) {
            int index = parts[i].indexOf("=");
            if (index > 0) {
                String key = parts[i].substring(0, index);
                String value = parts[i].substring(index+1);
                handleArgument(key, value);
            }
            else handleArgument(parts[i], "");

        }
    }


    private void handleArgument(String key, String value) {
        //System.out.println(": "+key);
        if (authorOrder.contains(key)) {
            if (key.equals("FirstFirst"))
                flMode = FIRST_FIRST;
            else if (key.equals("LastFirst"))
                flMode = LAST_FIRST;
            else if (key.equals("LastFirstFirstFirst"))
                flMode = LF_FF;
        }
        else if (authorAbbr.contains(key)) {
            if (key.equals("FullName")) {
                abbreviate = false;
            }
            else if (key.equals("Initials")) {
                abbreviate = true;
                firstInitialOnly = false;
            }
            else if (key.equals("FirstInitial")) {
                abbreviate = true;
                firstInitialOnly = true;
            }
            else if (key.equals("MiddleInitial")) {
                abbreviate = true;
                middleInitial = true;
            }
            else if (key.equals("LastName")) {
                lastNameOnly = true;
            }
            else if (key.equals("InitialsNoSpace")) {
                abbreviate = true;
                abbrSpaces = false;
            }
        }
        else if (authorPunc.contains(key)) {
            if (key.equals("FullPunc")) {
                abbrDots = true;
                lastFirstSeparator = ", ";
            }
            else if (key.equals("NoPunc")) {
                abbrDots = false;
                lastFirstSeparator = " ";
            }
            else if (key.equals("NoComma")) {
                abbrDots = true;
                lastFirstSeparator = " ";
            }
            else if (key.equals("NoPeriod")) {
                abbrDots = false;
                lastFirstSeparator = ", ";
            }
        }

        // AuthorSep = [Comma | And | Colon | Semicolon | sep=<string>]
        // AuthorLastSep = [And | Comma | Colon | Semicolon | Amp | Oxford | lastsep=<string>]
        else if (separators.contains(key) || lastSeparators.contains(key)) {
            if (key.equals("Comma")) {
                if (!setSep) {
                    separator = COMMA;
                    setSep = true;
                } else lastSeparator = COMMA;
            }
            else if (key.equals("And")) {
                if (!setSep) {
                    separator = AND;
                    setSep = true;
                } else lastSeparator = AND;
            }
            else if (key.equals("Colon")) {
                if (!setSep) {
                    separator = COLON;
                    setSep = true;
                } else lastSeparator = COLON;
            }
            else if (key.equals("Semicolon")) {
                if (!setSep) {
                    separator = SEMICOLON;
                    setSep = true;
                } else lastSeparator = SEMICOLON;
            }
            else if (key.equals("Oxford")) {
                lastSeparator = OXFORD;
            }
            else if (key.equals("Amp")) {
                lastSeparator = AMP;
            }
            else if (key.equals("Sep") && (value.length() > 0)) {
                separator = value;
                setSep = true;
            }
            else if (key.equals("LastSep") && (value.length() > 0)) {
                lastSeparator = value;
            }
        }
        else if (key.equals("EtAl") && (value.length() > 0)) {
            etAlString = value;
        }
        else if (numberPattern.matcher(key).matches()) {
            // Just a number:
            int num = Integer.parseInt(key);
            maxAuthors = num;
        }


    }

    public String format(String fieldText) {
        StringBuilder sb = new StringBuilder();
        AuthorList al = AuthorList.getAuthorList(fieldText);

        if ((maxAuthors < 0) || (al.size() <= maxAuthors)) {
            for (int i=0; i<al.size(); i++) {
                AuthorList.Author a = al.getAuthor(i);

                addSingleName(sb, a, (flMode == FIRST_FIRST) || ((flMode == LF_FF) && (i > 0)));

                if (i < al.size()-2)
                    sb.append(separator);
                else if (i < al.size()-1)
                    sb.append(lastSeparator);
            }
        }

        else {
            addSingleName(sb, al.getAuthor(0), flMode == FIRST_FIRST);
            sb.append(etAlString);
        }

        return sb.toString();
    }

    private void addSingleName(StringBuilder sb, AuthorList.Author a, boolean firstFirst) {
        String firstNamePart = a.getFirst();
        String lastNamePart = a.getLast();
        String von = a.getVon();
        if ((von != null) && (von.length() > 0))
            lastNamePart = von+" "+lastNamePart;
        String jr = a.getJr();
        if ((jr != null) && (jr.length() > 0))
            lastNamePart = lastNamePart+jrSeparator+jr;

        if (abbreviate) {
            firstNamePart = a.getFirstAbbr();
            if (firstInitialOnly && (firstNamePart.length() > 2))
                firstNamePart = firstNamePart.substring(0, 2);
            else if (middleInitial) {
                String abbr = firstNamePart;
                firstNamePart = a.getFirst();
                int index = firstNamePart.indexOf(" ");
                //System.out.println(firstNamePart);
                //System.out.println(index);
                if (index >= 0) {
                    firstNamePart = firstNamePart.substring(0, index+1);
                    if (abbr.length() > 3) {
                        firstNamePart = firstNamePart + abbr.substring(3);
                    }
                }
            }
            if (!abbrDots)
                firstNamePart = firstNamePart.replaceAll("\\.", "");
            if (!abbrSpaces)
                firstNamePart = firstNamePart.replaceAll(" ", "");
        }

        if (lastNameOnly) {
            sb.append(lastNamePart);
        }
        else if (firstFirst) {
            sb.append(firstNamePart).append(firstFirstSeparator);
            sb.append(lastNamePart);
        }
        else {
            sb.append(lastNamePart).append(lastFirstSeparator).append(firstNamePart);
        }

    }

    public static void main(String[] args) {
        Authors format = new Authors();
        format.setArgument("MiddleInitial,FullPunc,Amp,Semicolon,10,EtAl= m.fl.");
        System.out.println(format.format("Alfredsen, Jr, Jo Arve and Morten Omholt Alver and Yngvar von Olsen and Sebastian A. L. M. Kooijman"));
    }
}
