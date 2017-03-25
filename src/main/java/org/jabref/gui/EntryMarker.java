package org.jabref.gui;

import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.Globals;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.preferences.JabRefPreferences;

public class EntryMarker {

    public static final int MARK_COLOR_LEVELS = 6;
    public static final int MAX_MARKING_LEVEL = MARK_COLOR_LEVELS - 1;
    public static final int IMPORT_MARK_LEVEL = MARK_COLOR_LEVELS;

    private static final Pattern MARK_NUMBER_PATTERN = Pattern.compile(JabRefPreferences.getInstance().MARKING_WITH_NUMBER_PATTERN);

    private EntryMarker() {
    }

    /**
     * @param increment whether the given increment should be added to the current one. Currently never used in JabRef. Could be used to increase marking color ("Mark in specific color").
     */
    public static void markEntry(BibEntry be, int markIncrement, boolean increment, NamedCompound ce) {
        int prevMarkLevel;
        String newValue = null;
        if (be.hasField(FieldName.MARKED_INTERNAL)) {
            String markerString = be.getField(FieldName.MARKED_INTERNAL).get();
            int index = markerString.indexOf(Globals.prefs.getWrappedUsername());
            if (index >= 0) {
                // Already marked 1 for this user.
                prevMarkLevel = 1;
                newValue = markerString.substring(0, index)
                        + markerString.substring(index + Globals.prefs.getWrappedUsername().length())
                        + Globals.prefs.getWrappedUsername().substring(0,
                                Globals.prefs.getWrappedUsername().length() - 1)
                        + ":" + (increment ? Math.min(MAX_MARKING_LEVEL, prevMarkLevel + markIncrement) : markIncrement)
                        + "]";
            } else {
                Matcher m = MARK_NUMBER_PATTERN.matcher(markerString);
                if (m.find()) {
                    try {
                        prevMarkLevel = Integer.parseInt(m.group(1));
                        newValue = markerString.substring(0, m.start(1)) + (increment ? Math.min(MAX_MARKING_LEVEL, prevMarkLevel + markIncrement) : markIncrement) + markerString.substring(m.end(1));
                    } catch (NumberFormatException ex) {
                        // Do nothing.
                    }
                }
            }
        }
        if (newValue == null) {
            newValue = Globals.prefs.getWrappedUsername().substring(0, Globals.prefs.getWrappedUsername().length() - 1) + ":" + markIncrement + "]";
        }

        ce.addEdit(new UndoableFieldChange(be, FieldName.MARKED_INTERNAL,
                be.getField(FieldName.MARKED_INTERNAL).orElse(null), newValue));
        be.setField(FieldName.MARKED_INTERNAL, newValue);
    }

    /**
     * SIDE EFFECT: Unselects given entry
     */
    public static void unmarkEntry(BibEntry be, boolean onlyMaxLevel, BibDatabase database, NamedCompound ce) {
        if (be.hasField(FieldName.MARKED_INTERNAL)) {
            String markerString = be.getField(FieldName.MARKED_INTERNAL).get();
            if ("0".equals(markerString)) {
                if (!onlyMaxLevel) {
                    unmarkOldStyle(be, database, ce);
                }
                return;
            }
            String newValue = null;
            int index = markerString.indexOf(Globals.prefs.getWrappedUsername());
            if (index >= 0) {
                // Marked 1 for this user.
                if (onlyMaxLevel) {
                    return;
                } else {
                    newValue = markerString.substring(0, index)
                            + markerString.substring(index + Globals.prefs.getWrappedUsername().length());
                }
            } else {
                Matcher m = MARK_NUMBER_PATTERN.matcher(markerString);
                if (m.find()) {
                    try {
                        int prevMarkLevel = Integer.parseInt(m.group(1));
                        if (!onlyMaxLevel || (prevMarkLevel == MARK_COLOR_LEVELS)) {
                            if (prevMarkLevel > 1) {
                                newValue = markerString.substring(0, m.start(1)) + markerString.substring(m.end(1));
                            } else {
                                String toRemove = Globals.prefs.getWrappedUsername().substring(0,
                                        Globals.prefs.getWrappedUsername().length() - 1) + ":1]";
                                index = markerString.indexOf(toRemove);
                                if (index >= 0) {
                                    newValue = markerString.substring(0, index) + markerString.substring(index + toRemove.length());
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
            ce.addEdit(new UndoableFieldChange(be, FieldName.MARKED_INTERNAL,
                    be.getField(FieldName.MARKED_INTERNAL).get(), newValue));
            if (newValue == null) {
                be.clearField(FieldName.MARKED_INTERNAL);
            } else {
                be.setField(FieldName.MARKED_INTERNAL, newValue);
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
        Set<Object> owners = new TreeSet<>();
        for (BibEntry entry : database.getEntries()) {
            entry.getField(FieldName.OWNER).ifPresent(owners::add);
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
            ce.addEdit(new UndoableFieldChange(be, FieldName.MARKED_INTERNAL,
                    be.getField(FieldName.MARKED_INTERNAL).orElse(null), null));
            be.clearField(FieldName.MARKED_INTERNAL);
        } else {
            ce.addEdit(new UndoableFieldChange(be, FieldName.MARKED_INTERNAL,
                    be.getField(FieldName.MARKED_INTERNAL).orElse(null), newVal));
            be.setField(FieldName.MARKED_INTERNAL, newVal);
        }
    }

    public static int isMarked(BibEntry be) {
        if (!be.hasField(FieldName.MARKED_INTERNAL)) {
            return 0;
        }
        String s = be.getField(FieldName.MARKED_INTERNAL).get();
        if ("0".equals(s)) {
            return 1;
        }
        int index = s.indexOf(Globals.prefs.getWrappedUsername());
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
        return Globals.prefs.getBoolean(JabRefPreferences.MARK_IMPORTED_ENTRIES);
    }
}
