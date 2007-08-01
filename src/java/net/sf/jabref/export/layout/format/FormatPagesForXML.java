package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

public class FormatPagesForXML implements LayoutFormatter {

  public String format(String field) {
    return field.replaceAll("--", "&#x2013;");
  }
}
