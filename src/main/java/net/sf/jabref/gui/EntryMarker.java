/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.gui;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.gui.undo.UndoableFieldChange;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EntryMarker {

    public static final int MARK_COLOR_LEVELS = 6;
    public static final int MAX_MARKING_LEVEL = MARK_COLOR_LEVELS - 1;
    public static final int IMPORT_MARK_LEVEL = MARK_COLOR_LEVELS;

    private static final Pattern MARK_NUMBER_PATTERN = Pattern.compile(JabRefPreferences.getInstance().MARKING_WITH_NUMBER_PATTERN);


    /**
     * @param increment whether the given increment should be added to the current one. Currently never used in JabRef. Could be used to increase marking color ("Mark in specific color").
     */
    public static void markEntry(BibEntry be, int markIncrement, boolean increment, NamedCompound ce) {
        int prevMarkLevel;
        String newValue = null;
        if (be.hasField(InternalBibtexFields.MARKED)) {
            String s = be.getField(InternalBibtexFields.MARKED);
            int index = s.indexOf(Globals.prefs.WRAPPED_USERNAME);
            if (index >= 0) {
                // Already marked 1 for this user.
                prevMarkLevel = 1;
                newValue = s.substring(0, index) + s.substring(index + Globals.prefs.WRAPPED_USERNAME.length()) + Globals.prefs.WRAPPED_USERNAME.substring(0, Globals.prefs.WRAPPED_USERNAME.length() - 1) + ":" + (increment ? Math.min(MAX_MARKING_LEVEL, prevMarkLevel + markIncrement) : markIncrement) + "]";
            } else {
                Matcher m = MARK_NUMBER_PATTERN.matcher(s);
                if (m.find()) {
                    try {
                        prevMarkLevel = Integer.parseInt(m.group(1));
                        newValue = s.substring(0, m.start(1)) + (increment ? Math.min(MAX_MARKING_LEVEL, prevMarkLevel + markIncrement) : markIncrement) + s.substring(m.end(1));
                    } catch (NumberFormatException ex) {
                        // Do nothing.
                    }
                }
            }
        }
        if (newValue == null) {
            newValue = Globals.prefs.WRAPPED_USERNAME.substring(0, Globals.prefs.WRAPPED_USERNAME.length() - 1) + ":" + markIncrement + "]";
        }

        ce.addEdit(new UndoableFieldChange(be, InternalBibtexFields.MARKED, be.getField(InternalBibtexFields.MARKED), newValue));
        be.setField(InternalBibtexFields.MARKED, newValue);
    }

    /**
     * SIDE EFFECT: Unselects given entry
     */
    public static void unmarkEntry(BibEntry be, boolean onlyMaxLevel, BibDatabase database, NamedCompound ce) {
        if (be.hasField(InternalBibtexFields.MARKED)) {
            String s = be.getField(InternalBibtexFields.MARKED);
            if ("0".equals(s)) {
                if (!onlyMaxLevel) {
                    unmarkOldStyle(be, database, ce);
                }
                return;
            }
            String newValue = null;
            int index = s.indexOf(Globals.prefs.WRAPPED_USERNAME);
            if (index >= 0) {
                // Marked 1 for this user.
                if (onlyMaxLevel) {
                    return;
                } else {
                    newValue = s.substring(0, index) + s.substring(index + Globals.prefs.WRAPPED_USERNAME.length());
                }
            } else {
                Matcher m = MARK_NUMBER_PATTERN.matcher(s);
                if (m.find()) {
                    try {
                        int prevMarkLevel = Integer.parseInt(m.group(1));
                        if (!onlyMaxLevel || (prevMarkLevel == MARK_COLOR_LEVELS)) {
                            if (prevMarkLevel > 1) {
                                newValue = s.substring(0, m.start(1)) + s.substring(m.end(1));
                            } else {
                                String toRemove = Globals.prefs.WRAPPED_USERNAME.substring(0, Globals.prefs.WRAPPED_USERNAME.length() - 1) + ":1]";
                                index = s.indexOf(toRemove);
                                if (index >= 0) {
                                    newValue = s.substring(0, index) + s.substring(index + toRemove.length());
                                }
                            }
                        } else {
                            return;
                        }
                    } catch (NumberFormatException ex) {
                        // Do nothing.
                    }
                }
            }

            /*int piv = 0, hit;
            StringBuffer sb = new StringBuffer();
            while ((hit = s.indexOf(G047749118118
            1110lobals.prefs.WRAPPED_USERNAME, piv)) >= 0) {
            	if (hit > 0)
            		sb.append(s.substring(piv, hit));
            	piv = hit + Globals.prefs.WRAPPED_USERNAME.length();
            }
            if (piv < s.length() - 1) {
            	sb.append(s.substring(piv));
            }
            String newVal = sb.length() > 0 ? sb.toString() : null;*/
            ce.addEdit(new UndoableFieldChange(be, InternalBibtexFields.MARKED, be.getField(InternalBibtexFields.MARKED), newValue));
            if (newValue == null) {
                be.clearField(InternalBibtexFields.MARKED);
            } else {
                be.setField(InternalBibtexFields.MARKED, newValue);
            }
        }
    }

    /**
     * An entry is marked with a "0", not in the new style with user names. We
     * want to unmark it as transparently as possible. Since this shouldn't
     * happen too often, we do it by scanning the "owner" fields of the entire
     * database, collecting all user names. We then mark the entry for all users
     * except the current one. Thus only the user who unmarks will see that it
     * is unmarked, and we get rid of the old-style marking.
     *
     * @param be
     * @param ce
     */
    private static void unmarkOldStyle(BibEntry be, BibDatabase database, NamedCompound ce) {
        TreeSet<Object> owners = new TreeSet<>();
        for (BibEntry entry : database.getEntries()) {
            entry.getFieldOptional(InternalBibtexFields.OWNER).ifPresent(owners::add);
        }
        owners.remove(Globals.prefs.get(JabRefPreferences.DEFAULT_OWNER));
        StringBuilder sb = new StringBuilder();
        for (Object owner : owners) {
            sb.append('[');
            sb.append(owner);
            sb.append(']');
        }
        String newVal = sb.toString();
        if (newVal.isEmpty()) {
            ce.addEdit(new UndoableFieldChange(be, InternalBibtexFields.MARKED, be.getField(InternalBibtexFields.MARKED), null));
            be.clearField(InternalBibtexFields.MARKED);
        } else {
            ce.addEdit(new UndoableFieldChange(be, InternalBibtexFields.MARKED, be.getField(InternalBibtexFields.MARKED), newVal));
            be.setField(InternalBibtexFields.MARKED, newVal);
        }
    }

    public static int isMarked(BibEntry be) {
        if (!be.hasField(InternalBibtexFields.MARKED)) {
            return 0;
        }
        String s = be.getField(InternalBibtexFields.MARKED);
        if ("0".equals(s)) {
            return 1;
        }
        int index = s.indexOf(Globals.prefs.WRAPPED_USERNAME);
        if (index >= 0) {
            return 1;
        }

        Matcher m = MARK_NUMBER_PATTERN.matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ex) {
                return 1;
            }
        } else {
            return 0;
        }

    }

    public static boolean shouldMarkEntries() {
        return (Globals.prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES)
                && (Globals.prefs.getBoolean(JabRefPreferences.USE_OWNER)
                        || Globals.prefs.getBoolean(JabRefPreferences.USE_TIME_STAMP)));
    }
}
