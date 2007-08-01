package net.sf.jabref.imports;

import java.util.Iterator;
import java.util.TreeSet;
import java.util.Vector;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.Util;

public class TextAnalyzer {

  BibtexEntry be = null;

  public TextAnalyzer(String text) {
    guessBibtexFields(text);
  }

  public BibtexEntry getEntry() {
    return be;
  }

  public void guessBibtexFields(String text) {

      TreeSet<Substring> usedParts = new TreeSet<Substring>();

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
        int pos = text.indexOf(cand[0]);
        usedParts.add(new Substring("pages", pos, pos+year.length()));
        Util.pr("Guessing 'pages': '" + pages + "'");
      } else if (cand.length > 1) {
        int found = -1;
        checkScope: for (int i=0; i<cand.length; i++) {
          split = clean(cand[i].replaceAll("\\s", "")).split("-");
               //   Util.pr("Pg: "+pages);
          int first = Integer.parseInt(split[0]),
              second = Integer.parseInt(split[1]);
          if (second-first > 3) {
            found = i;
            break checkScope;
          }
        }
        if (found >= 0) {
          pages = clean(cand[found].replaceAll("-|( - )", "--"));
          int pos = text.indexOf(cand[found]);
          Util.pr("Guessing 'pages': '" + pages + "'");
          usedParts.add(new Substring("pages", pos, pos+pages.length()));
        }
      }

      //String journalRx = "(\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((vol\\.|Vol\\.|Volume|volume))??(.??)(\\d{1,3})(\\.|,|\\s)";
      String journal = null,
          volume = null;
      String journalRx = "(,|\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((.){0,2})((vol\\.|Vol\\.|Volume|volume))??\\s??(\\d{1,3})(\\.|,|\\s|:)";
      cand = getMatches(text, journalRx);
      if (cand.length > 0) {
        //Util.pr("guessing 'journal': '" + cand[0] + "'");
        cand[0] = cand[0].trim();
        int pos = cand[0].lastIndexOf(' ');
        if (pos > 0) {
          volume = clean(cand[0].substring(pos+1));
          Util.pr("Guessing 'volume': '" + volume + "'");
          journal = clean(cand[0].substring(0, pos));
          //Util.pr("guessing 'journal': '" + journal + "'");
          pos = journal.lastIndexOf(' ');
          if (pos > 0) {
            String last = journal.substring(pos+1).toLowerCase();
            if (last.equals("volume") || last.equals("vol") || last.equals("v"))
              journal = clean(journal.substring(0, pos));
          }
          pos = text.indexOf(journal);
          usedParts.add(new Substring("journal", pos, pos+journal.length()));
          Util.pr("Guessing 'journal': '" + journal + "'");
        }
        //Util.pr("Journal? '"+cand[0]+"'");
      } else {
        // No journal found. Maybe the year precedes the volume? Try another regexp:
        journalRx = "(,|\\.|\\n)\\s??([a-zA-Z\\. ]{8,30}+)((.){0,2})\\s??(\\d{1,3})(\\.|,|\\s|:)";
      }

      // Then try to find title and authors.
      Substring ss;
      Vector<String> free = new Vector<String>();
      int piv = 0;
      for (Iterator<Substring> i=usedParts.iterator(); i.hasNext();) {
        ss = i.next();
        if (ss.begin()-piv > 10) {
          Util.pr("... "+text.substring(piv, ss.begin()));
          free.add(clean(text.substring(piv, ss.begin())));
        }
        piv = ss.end();
      }
      if (text.length()-piv > 10) {
        free.add(clean(text.substring(piv)));
      }
      Util.pr("Free parts:");
      for (String s : free){
        Util.pr(": '"+s+"'");
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
            || (c==':') || (c==')'))
          left++;
        else
          found = true;
      }
      found = false;
      while (!found && right>left) {
        char c = s.charAt(right);
        if (Character.isWhitespace(c) || (c=='.') || (c==',') || (c==')')
            || (c==':') || (c=='('))
          right--;
        else
          found = true;
      }
      //Util.pr(s+"\n"+left+" "+right);
      return s.substring(left, Math.min(right+1, s.length()));
    }

    private class Substring implements Comparable<Substring> {
		int begin, end;

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

		public int compareTo(Substring other) {
			return (new Integer(begin)).compareTo(new Integer(other.begin()));
		}
	}
}
