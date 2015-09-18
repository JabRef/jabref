/*  Copyright (C) 2012 JabRef contributors.
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

import net.sf.jabref.Globals;
import net.sf.jabref.logic.l10n.Localization;

public abstract class Rank extends SpecialField {

    Rank() {
        TEXT_DONE_PATTERN = "Set rank to '%0' for %1 entries";
    }

    public static Rank getInstance() {
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_RANKING_COMPACT)) {
            return RankCompact.getInstance();
        } else {
            return RankExtended.getInstance();
        }
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_RANKING;
    }

    @Override
    public String getToolTip() {
        return Localization.lang("Rank");
    }

    @Override
    public String getMenuString() {
        return Localization.lang("Rank");
    }

}
