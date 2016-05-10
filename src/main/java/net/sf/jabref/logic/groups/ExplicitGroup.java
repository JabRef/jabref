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
package net.sf.jabref.logic.groups;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.importer.fileformat.ParseException;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.QuotedStringTokenizer;
import net.sf.jabref.logic.util.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Select explicit bibtex entries. It is also known as static group.
 *
 * @author jzieren
 */
public class ExplicitGroup extends KeywordGroup {

    public static final String ID = "ExplicitGroup:";

    private final List<String> legacyEntryKeys = new ArrayList<>();

    private static final Log LOGGER = LogFactory.getLog(ExplicitGroup.class);

    public ExplicitGroup(String name, GroupHierarchyType context) throws ParseException {
        super(name, "groups", name, true, false, context);
    }

    public static ExplicitGroup fromString(String s) throws ParseException {
        if (!s.startsWith(ExplicitGroup.ID)) {
            throw new IllegalArgumentException("ExplicitGroup cannot be created from \"" + s + "\".");
        }
        QuotedStringTokenizer tok = new QuotedStringTokenizer(s.substring(ExplicitGroup.ID.length()),
                AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR);

        String name = tok.nextToken();
        int context = Integer.parseInt(tok.nextToken());
        ExplicitGroup newGroup = new ExplicitGroup(name, GroupHierarchyType.getByNumber(context));
        newGroup.addLegacyEntryKeys(tok);
        return newGroup;
    }

    /**
     * Called only when created fromString.
     * JabRef used to store the entries of an explicit group in the serialization, e.g.
     *  ExplicitGroup:GroupName\;0\;Key1\;Key2\;;
     * This method exists for backwards compatibility.
     */
    private void addLegacyEntryKeys(QuotedStringTokenizer tok) {
        while (tok.hasMoreTokens()) {
            String key = StringUtil.unquote(tok.nextToken(), AbstractGroup.QUOTE_CHAR);
            addLegacyEntryKey(key);
        }
    }

    public void addLegacyEntryKey(String key) {
        this.legacyEntryKeys.add(key);
    }

    @Override
    public AbstractGroup deepCopy() {
        try {
            ExplicitGroup copy = new ExplicitGroup(getName(), getContext());
            copy.legacyEntryKeys.addAll(legacyEntryKeys);
            return copy;
        } catch (ParseException exception) {
            // this should never happen, because the constructor obviously succeeded in creating _this_ instance!
            LOGGER.error("Internal error in ExplicitGroup.deepCopy(). "
                    + "Please report this on https://github.com/JabRef/jabref/issues", exception);
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ExplicitGroup)) {
            return false;
        }
        ExplicitGroup other = (ExplicitGroup) o;
        return Objects.equals(getName(), other.getName()) && Objects.equals(getHierarchicalContext(),
                other.getHierarchicalContext()) && Objects.equals(getLegacyEntryKeys(), other.getLegacyEntryKeys());
    }

    /**
     * Returns a String representation of this group and its entries.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExplicitGroup.ID).append(
                StringUtil.quote(getName(), AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)).
                append(AbstractGroup.SEPARATOR).append(getContext().ordinal()).append(AbstractGroup.SEPARATOR);

        // write legacy entry keys in well-defined order for CVS compatibility
        Set<String> sortedKeys = new TreeSet<>();
        sortedKeys.addAll(legacyEntryKeys);

        for (String sortedKey : sortedKeys) {
            sb.append(StringUtil.quote(sortedKey, AbstractGroup.SEPARATOR, AbstractGroup.QUOTE_CHAR)).append(
                    AbstractGroup.SEPARATOR);
        }
        return sb.toString();
    }

    /**
     * Remove all stored cite keys, resulting in an empty group.
     */
    public void clearLegacyEntryKeys() {
        legacyEntryKeys.clear();
    }

    @Override
    public String getDescription() {
        return ExplicitGroup.getDescriptionForPreview();
    }

    public static String getDescriptionForPreview() {
        return Localization.lang("This group contains entries based on manual assignment. "
                + "Entries can be assigned to this group by selecting them "
                + "then using either drag and drop or the context menu. "
                + "Entries can be removed from this group by selecting them "
                + "then using the context menu.");
    }

    @Override
    public String getShortDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("<b>").append(getName()).append("</b> -").append(Localization.lang("static group"));
        switch (getHierarchicalContext()) {
        case INCLUDING:
            sb.append(", ").append(Localization.lang("includes subgroups"));
            break;
        case REFINING:
            sb.append(", ").append(Localization.lang("refines supergroup"));
            break;
        default:
            break;
        }
        return sb.toString();
    }

    public List<String> getLegacyEntryKeys() {
        return legacyEntryKeys;
    }

    @Override
    public String getTypeId() {
        return ExplicitGroup.ID;
    }

    public int getNumEntries() {
        return legacyEntryKeys.size();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
