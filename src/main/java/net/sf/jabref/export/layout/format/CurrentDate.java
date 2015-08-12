/*
 * Copyright (C) 2003-2005 Andreas Rudert.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.export.layout.format;

import java.util.Date;
import java.text.SimpleDateFormat;

import net.sf.jabref.export.layout.LayoutFormatter;

/**
 * Inserts the current date (the time a database is being exported).
 * 
 * <p>If a fieldText is given, it must be a valid {@link SimpleDateFormat} pattern.
 * If none is given, the format pattern will be <code>yyyy.MM.dd hh:mm:ss z</code></p>
 *
 * @author andreas_sf at rudert-home dot de
 */
public class CurrentDate implements LayoutFormatter
{

    private static final String defaultFormat = "yyyy.MM.dd hh:mm:ss z";


    /*
     *  (non-Javadoc)
     * @see net.sf.jabref.export.layout.LayoutFormatter#format(java.lang.String)
     */
    @Override
    public String format(String fieldText)
    {
        String format = CurrentDate.defaultFormat;
        if (fieldText != null && fieldText.trim() != null && !fieldText.trim().isEmpty()) {
            format = fieldText;
        }
        return new SimpleDateFormat(format).format(new Date());
    }
}
