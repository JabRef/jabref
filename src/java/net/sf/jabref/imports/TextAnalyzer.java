package net.sf.jabref.imports;

import java.util.TreeSet;
import net.sf.jabref.*;

public class TextAnalyzer {

  BibtexEntry be = null;

  public TextAnalyzer(String text) {
    guessBibtexFields(text);
  }

  public BibtexEntry getEntry() {
    return be;
  }

  public void guessBibtexFields(String text) {

      TreeSet usedParts = new TreeSet();

      text = "  "+text+"  ";

      String[] split = null;

      // Look for the year:
      String year = null;
      String yearRx = "(\\s|\\()\\d\\d\\d\\d(\\.|,|\\))";
      String[] cand = getMatches(text, yearRx);
      if (cand.length == 1) {
        // Only one four-digit number, so we guess that is the year.
        year = clean(cand[0]);
        int pos = text.indexOf(year);
        usedParts.add(new Substring("year", pos, pos+year.length()));
        Util.pr("Guessing 'year': '"+year+"'");
      } else if (cand.length > 1) {
        // More than one four-digit numbers, so we look for one giving a reasonable year:

        int good = -1, yearFound = -1;
        find: for (int i=0; i<cand.length; i++) {
          int number = Integer.parseInt(cand[i].trim());
          if (number == yearFound)
            continue find;
          if (number < 2500) {
            if (good == -1) {
              good = i;
              yearFound = number;
            } else {
              // More than one found. Be a bit more specific.
              if ((yearFound < Globals.FUTURE_YEAR) && (number < Globals.FUTURE_YEAR)) {
                good = -1;
                break find; // Give up, both seem good enough.
              }
              else if ((yearFound >= Globals.FUTURE_YEAR) && (number < Globals.FUTURE_YEAR)) {
                good = i;
                yearFound = number;
              }
            }
          }
        }
        if (good >= 0) {
          year = clean(cand[good]);
          int pos = text.indexOf(year);
          usedParts.add(new Substring("year", pos, pos+year.length()));
          Util.pr("Guessing 'year': '"+year+"'");
        }
      }

      // Look for Pages:
      String pages = null;
      String pagesRx = "\\s(\\d{1,4})( ??)-( ??)(\\d{1,4})(\\.|,|\\s)";
      cand = getMatches(text, pagesRx);
      if (cand.length == 1) {
        pages = clean(cand[0].replaceAll("-|( - )", "--"));
        int pos = text.indexOf(pages);
        usedParts.add(new Substring("pages", pos, pos+year.length()));
        Util.pr("Guessing 'pages': '" + pages + "'");
      } else if (cand.length > 1) {
        int found = -1;
        checkScope: for (int i=0; i<cand.length; i++) {
          split = cand[i].replaceAll("\\s", "").split("-");
          int first = Integer.parseInt(split[0]),
              second = Integer.parseInt(split[1]);
          if (second-first > 2) {
            found = i;
            break checkScope;
          }
        }
        if (found >= 0) {
          pages = clean(cand[found].replaceAll("-|( - )", "--"));
          int pos = text.indexOf(pages);
          usedParts.add(new Substring("pages", pos, pos+year.length()));
          Util.pr("Guessing 'pages': '" + pages + "'");
        }
      }

      //String journalRx = "(\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((vol\\.|Vol\\.|Volume|volume))??(.??)(\\d{1,3})(\\.|,|\\s)";
      String journal = null,
          volume = null;
      String journalRx = "(,|\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((.){0,2})\\s??(\\d{1,3})(\\.|,|\\s|:)";
      cand = getMatches(text, journalRx);
      if (cand.length > 0) {
        int pos = cand[0].trim().lastIndexOf(' ');
        if (pos > 0) {
          volume = clean(cand[0].substring(pos+1));
          Util.pr("Guessing 'volume': '" + volume + "'");
          journal = clean(cand[0].substring(0, pos));
          pos = journal.lastIndexOf(' ');
          if (pos > 0) {
            String last = journal.substring(pos+1).toLowerCase();
            if (last.equals("volume") || last.equals("vol") || last.equals("v"))
              journal = journal.substring(0, pos);
          }
          Util.pr("Guessing 'journal': '" + journal + "'");
        }
        //Util.pr("Journal? '"+cand[0]+"'");
      }
    }


    public String[] getMatches(String text, String regexp) {
      int piv = 0;
      String[] test = text.split(regexp);
      if (test.length < 2)
        return new String[0];

      String[] out = new String[test.length-1];
      for (int i=0; i<out.length; i++) {
        String[] curr = text.split(regexp, i+2);
        out[i] = text.substring(piv+curr[i].length(), text.length()-curr[i+1].length());
        piv += curr[i].length()+out[i].length();
        //Util.pr("--"+out[i]+"\n-> "+piv);
      }
      return out;
    }

    private String clean(String s) {
      boolean found = false;
      int left = 0, right = s.length()-1;
      while (!found && left<s.length()) {
        char c = s.charAt(left);
        if (Character.isWhitespace(c) || (c=='.') || (c==',') || (c=='(')
            || (c==':'))
          left++;
        else
          found = true;
      }
      found = false;
      while (!found && right>left) {
        char c = s.charAt(right);
        if (Character.isWhitespace(c) || (c=='.') || (c==',') || (c==')')
            || (c==':'))
          right--;
        else
          found = true;
      }
      //Util.pr(s+"\n"+left+" "+right);
      return s.substring(left, Math.min(right+1, s.length()));
    }

    private class Substring implements Comparable {
      int begin, end;
      public Substring(String name, int begin, int end) {
        this.begin = begin;
        this.end = end;
      }
      public int begin() { return begin; }
      public int end() { return end; }
      public int compareTo(Object o) {
        Substring other = (Substring)o;
        return (new Integer(begin)).compareTo(new Integer(other.begin()));
      }
        }
}
