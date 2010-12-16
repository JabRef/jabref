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
    AuthorSort = [FirstLast | LastFirst | LastFirstFirst]
    AuthorAbbr = [Names | Initials | FirstInitial]
    AuthorSep = [Comma | And | Colon | Semicolon | Sep=<string>]
    AuthorLastSep = [And | Colon | Semicolon | Amp | Oxford | LastSep=<string>]
    AuthorPunc = [FullPunc | NoPunc | NoComma | NoPeriod]
    AuthorNumber = [inf | <number>]
    EtAlString = [et al. | EtAl=<string>]
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
        authorOrder.add("LastFirstFirst");

        authorAbbr.add("Names");
        authorAbbr.add("Initials");
        authorAbbr.add("FirstInitial");

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
        lastSeparators.add("Oxfort");
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
        AND = " and ";

    int flMode = 1;

    boolean
        firstFirst = true,
        abbreviate = true,
        firstInitialOnly = false,
        abbrDots = true,
        abbrSpaces = true;

    boolean setSep = false;

    int maxAuthors = -1;

    String
        firstFirstSeparator = " ",
        lastFirstSeparator = ", ",
        separator = SEMICOLON,
        lastSeparator = AMP,
        etAlString = " et al.";


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
            else if (key.equals("LastFirstFirst"))
                flMode = LF_FF;
        }
        else if (authorAbbr.contains(key)) {
            if (key.equals("Names")) {
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
        // AuthorLastSep = [And | Colon | Semicolon | Amp | Oxford | lastsep=<string>]
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


        /*if (key.equals("order")) {
            value = value.toLowerCase();
            if (value.equals("ff") || value.equals("firstfirst"))
                flMode = FIRST_FIRST;
            else if (value.equals("lf") || value.equals("lastfirst"))
                flMode = LAST_FIRST;
            else if (value.equals("lf-ff") || value.equals("lastfirst-firstfirst"))
                flMode = LF_FF;
        }
        else if (key.equals("separator")) {
            
        }*/
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
        if (abbreviate) {
            firstNamePart = a.getFirstAbbr();
            if (firstInitialOnly && (firstNamePart.length() > 2))
                firstNamePart = firstNamePart.substring(0, 2);
            if (!abbrDots)
                firstNamePart = firstNamePart.replaceAll("\\.", "");
            if (!abbrSpaces)
                firstNamePart = firstNamePart.replaceAll(" ", "");
        }

        if (firstFirst) {
            sb.append(firstNamePart).append(firstFirstSeparator);
            String von = a.getVon();
            if ((von != null) && (von.length() > 0))
                sb.append(von).append(" ");
            sb.append(a.getLast());
        }
        else {
            sb.append(a.getLast()).append(lastFirstSeparator).append(firstNamePart);
        }

    }

    public static void main(String[] args) {
        Authors format = new Authors();
        format.setArgument("LastFirstFirst,Initials,NoComma,Comma,LastSep= und ,2,EtAl= m.fl.");
        System.out.println(format.format("Jo-Arve Alfredsen and Morten O. Alver and Yngvar von Olsen and S. A. L. M. Kooijman"));
    }
}
