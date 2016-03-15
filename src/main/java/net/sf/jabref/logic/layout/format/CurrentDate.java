/*
 Copyright (C) 2005-2015 Andreas Rudert, Oscar Gustafsson

 All programs in this directory and
 subdirectories are published under the GNU General Public License as
 described below.

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or (at
 your option) any later version.

 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 USA

 Further information about the GNU GPL is available at:
 http://www.gnu.org/copyleft/gpl.ja.html
*/
package net.sf.jabref.logic.layout.format;

import java.util.Date;

import net.sf.jabref.logic.layout.LayoutFormatter;

import java.text.SimpleDateFormat;

/**
 * Inserts the current date (the time a database is being exported).
 *
 * <p>If a fieldText is given, it must be a valid {@link SimpleDateFormat} pattern.
 * If none is given, the format pattern will be <code>yyyy-MM-dd hh:mm:ss z</code>.
 * This follows ISO-8601. Reason: <a href="https://xkcd.com/1179/">https://xkcd.com/1179/</a>.</p>
 *
 * @author andreas_sf at rudert-home dot de
 */
public class CurrentDate implements LayoutFormatter {

    // default time stamp follows ISO-8601. Reason: https://xkcd.com/1179/
    private static final String DEFAULT_FORMAT = "yyyy-MM-dd hh:mm:ss z";


    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText) {
        String format = CurrentDate.DEFAULT_FORMAT;
        if ((fieldText != null) && (fieldText.trim() != null) && !fieldText.trim().isEmpty()) {
            format = fieldText;
        }
        return new SimpleDateFormat(format).format(new Date());
    }
}
