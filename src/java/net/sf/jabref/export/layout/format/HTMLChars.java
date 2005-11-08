package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.*;
import net.sf.jabref.Globals;

public class HTMLChars implements LayoutFormatter {


  public String format(String field) {
    int i;
    field = firstFormat(field);

    StringBuffer sb = new StringBuffer("");
    StringBuffer currentCommand = null;
    char c;
    boolean escaped = false, incommand = false;
    for (i=0; i<field.length(); i++) {
      c = field.charAt(i);
      if (escaped && (c == '\\')) {
        sb.append('\\');
        escaped = false;
      }
      else if (c == '\\') {
        escaped = true;
        incommand = true;
        currentCommand = new StringBuffer();
      }
      else if (!incommand && (c=='{' || c=='}')) {
        // Swallow the brace.
      }
      else if (Character.isLetter((char)c) ||
               (Globals.SPECIAL_COMMAND_CHARS.indexOf(""+(char)c) >= 0)) {
        escaped = false;
        if (!incommand)
          sb.append((char)c);
          // Else we are in a command, and should not keep the letter.
        else {
          currentCommand.append( (char) c);

          testCharCom: if ((currentCommand.length() == 1)
              && (Globals.SPECIAL_COMMAND_CHARS.indexOf(currentCommand.toString()) >= 0)) {
            // This indicates that we are in a command of the type \^o or \~{n}
            if (i >= field.length()-1)
              break testCharCom;

            String command = currentCommand.toString();
            i++;
            c = field.charAt(i);
            //System.out.println("next: "+(char)c);
            String combody;
            if (c == '{') {
              IntAndString part = getPart(field, i);
              i += part.i;
              combody = part.s;
            }
            else {
              combody = field.substring(i,i+1);
              //System.out.println("... "+combody);
            }
            Object result = Globals.HTMLCHARS.get(command+combody);
	    
            if (result != null)
              sb.append((String)result);

            incommand = false;
            escaped = false;

          }

        }

      }
      else {
        //if (!incommand || ((c!='{') && !Character.isWhitespace(c)))
        testContent: if (!incommand || (!Character.isWhitespace(c) && (c != '{')))
          sb.append((char)c);
        else {
          // First test if we are already at the end of the string.
          if (i >= field.length()-1)
            break testContent;

          if (c == '{') {

            String command = currentCommand.toString();
            // Then test if we are dealing with a italics or bold command. If so, handle.
            if (command.equals("emph") || command.equals("textit")) {
              IntAndString part = getPart(field, i);
              i += part.i;
                sb.append("<em>").append(part.s).append("</em>");
            }
            else if (command.equals("textbf")) {
              IntAndString part = getPart(field, i);
              i += part.i;
                sb.append("<b>").append(part.s).append("</b>");
            }
          } else
            sb.append((char)c);

        }
        incommand = false;
        escaped = false;
      }
    }

    return sb.toString();
        //field.replaceAll("\\\\emph", "").replaceAll("\\\\em", "").replaceAll("\\\\textbf", "");
  }

  private String firstFormat(String s) {
    return s.replaceAll("&|\\\\&","&amp;").replaceAll("[\\n]{1,}","<p>");//.replaceAll("--", "&mdash;");
  }

  private IntAndString getPart(String text, int i) {
    char c;
    int count = 0;//, i=index;
    StringBuffer part = new StringBuffer();
    while ((count >= 0) && (i < text.length())) {
      i++;
      c = text.charAt(i);
      if (c == '}')
        count--;
      else if (c == '{')
        count++;

      part.append((char)c);
    }
    //System.out.println("part: "+part.toString()+"\nformatted: "+format(part.toString()));
    return new IntAndString(part.length(), format(part.toString()));
  }

  private class IntAndString{
    public int i;
    String s;
    public IntAndString(int i, String s) {
      this.i = i;
      this.s = s;
    }
  }
}
