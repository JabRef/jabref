package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.*;

public class FormatPagesForHTML implements LayoutFormatter {

  public String format(String field) {
    return field.replaceAll("--", "-");
  }
}
