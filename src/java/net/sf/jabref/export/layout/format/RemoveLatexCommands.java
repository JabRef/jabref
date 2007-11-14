package net.sf.jabref.export.layout.format;

import net.sf.jabref.Globals;
import net.sf.jabref.export.layout.LayoutFormatter;

public class RemoveLatexCommands implements LayoutFormatter {

  int i;

  public String format(String field) {

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

      else if (Character.isLetter(c) ||
                (Globals.SPECIAL_COMMAND_CHARS.indexOf(""+c) >= 0)) {
         escaped = false;
         if (!incommand)
           sb.append(c);
           // Else we are in a command, and should not keep the letter.
         else {
           currentCommand.append( c);
           if ((currentCommand.length() == 1)
               && (Globals.SPECIAL_COMMAND_CHARS.indexOf(currentCommand.toString()) >= 0)) {
             // This indicates that we are in a command of the type \^o or \~{n}
 /*            if (i >= field.length()-1)
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
 */
             incommand = false;
             escaped = false;

           }

        }
      }

      else if (Character.isLetter(c)) {
        escaped = false;
        if (!incommand)
          sb.append(c);
          // Else we are in a command, and should not keep the letter.
        else
          currentCommand.append(c);
      }
      else {
        //if (!incommand || ((c!='{') && !Character.isWhitespace(c)))
        if (!incommand || (!Character.isWhitespace(c) && (c != '{')))
          sb.append(c);
        else {
          if (c != '{')
            sb.append(c);
        }
        incommand = false;
        escaped = false;
      }
    }

    return sb.toString();
  }
}
