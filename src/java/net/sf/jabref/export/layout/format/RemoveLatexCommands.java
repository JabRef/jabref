package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.*;

public class RemoveLatexCommands implements LayoutFormatter {

  public String format(String field) {
    StringBuffer sb = new StringBuffer("");
    char c;
    boolean escaped = false, incommand = false;
    for (int i=0; i<field.length(); i++) {
      c = field.charAt(i);
      if (escaped && (c == '\\')) {
        sb.append('\\');
        escaped = false;
      }
      else if (c == '\\') {
        escaped = true;
        incommand = true;
      }
      else if (Character.isLetter((char)c)) {
        escaped = false;
        if (!incommand)
          sb.append((char)c);
          // Else we are in a command, and should not keep the letter.
      }
      else {
        if (!incommand || ((c!='{') && !Character.isWhitespace(c)))
        sb.append((char)c);
        incommand = false;
        escaped = false;
      }
    }

    return sb.toString();
        //field.replaceAll("\\\\emph", "").replaceAll("\\\\em", "").replaceAll("\\\\textbf", "");
  }
}
