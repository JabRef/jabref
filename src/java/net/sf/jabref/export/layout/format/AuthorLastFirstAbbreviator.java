package net.sf.jabref.export.layout.format;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Uses as input the fields (author or editor) in the LastFirst format.
 * 
 * This formater enables to abbreviate the authors name in the following way:
 * 
 * Ex: Someone, Van Something will be abbreviated as Someone, V.S.
 * 
 * @author Carlos Silla
 * @author Christopher Oezbek <oezi@oezi.de>
 * 
 * @version 1.0 Created on 12/10/2004
 * @version 1.1 Fixed bug
 *          http://sourceforge.net/tracker/index.php?func=detail&aid=1466924&group_id=92314&atid=600306
 */
public class AuthorLastFirstAbbreviator implements LayoutFormatter {

	/**
	 * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
	 */
	public String format(String fieldText) {

        /**
         * This formatter is a duplicate of AuthorAbbreviator, so we simply
         * call that one.
         */
        return (new AuthorAbbreviator()).format(fieldText);

	}
}
