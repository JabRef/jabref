/**
 * 
 */
package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.AbstractParamLayoutFormatter;

/**
 * @author ralmond
 * 
 * This formatter takes two arguments and examines the field text.  
 * If the field text represents multiple individuals, that is it contains the string "and"
 * then the field text is replaced with the first argument, otherwise it is replaced with the second.
 * For example:
 * 
 * \format[IfPlural(Eds.,Ed.)]{\editor}
 * 
 * Should expand to 'Eds.' if the document has more than one editor and 'Ed.' if it only has one.
 * 
 *
 */
public class IfPlural extends AbstractParamLayoutFormatter {
  
  protected String pluralText, singularText;
  
  public void setArgument(String arg) {
    String[] parts = parseArgument(arg);

    if (parts.length < 2)
        return; // TODO: too few arguments. Print an error message here?
    pluralText = parts[0];
    singularText = parts[1];

}

public String format(String fieldText) {
    if (pluralText == null)
        return fieldText; // TODO: argument missing or invalid. Print an error message here?
    if (fieldText.matches(".*\\sand\\s.*"))
      return pluralText;
    else 
      return singularText;

}


}
