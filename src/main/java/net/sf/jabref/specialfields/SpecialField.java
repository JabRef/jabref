/*  Copyright (C) 2012-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.specialfields;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.*;

public abstract class SpecialField {

    // currently, menuString is used for undo string
    // public static String TEXT_UNDO;

    // Plain string; NOT treated by Globals.lang
    public String TEXT_DONE_PATTERN;

    private List<SpecialFieldValue> values;
    private List<String> keywords;
    private HashMap<String, SpecialFieldValue> map;


    protected void setValues(List<SpecialFieldValue> values) {
        this.values = values;
        this.keywords = new ArrayList<>();
        this.map = new HashMap<>();
        for (SpecialFieldValue v : values) {
            v.getKeyword().ifPresent(keywords::add);
            v.getFieldValue().ifPresent(fieldValue -> map.put(fieldValue, v));
        }
    }

    public List<SpecialFieldValue> getValues() {
        return this.values;
    }

    public List<String> getKeyWords() {
        return this.keywords;
    }

    public SpecialFieldValue parse(String s) {
        return map.get(s);
    }

    public abstract String getFieldName();

    public abstract Icon getRepresentingIcon();

    public abstract String getMenuString();

    public abstract String getToolTip();

    public boolean isSingleValueField() {
        return false;
    }

}
