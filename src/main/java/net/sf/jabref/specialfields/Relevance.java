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

import javax.swing.*;

import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;

public class Relevance extends SpecialField {

    private static Relevance INSTANCE;


    private Relevance() {
        ArrayList<SpecialFieldValue> values = new ArrayList<>();
        // action directly set by JabRefFrame
        // DO NOT TRANSLATE "relevant" as this makes the produced .bib files non portable
        values.add(new SpecialFieldValue(this, "relevant", "toggleRelevance", Localization.lang("Toggle relevance"), IconTheme.JabRefIcon.RELEVANCE.getSmallIcon(),
                Localization.lang("Toggle relevance")));
        this.setValues(values);
        TEXT_DONE_PATTERN = "Toggled relevance for %0 entries";
    }

    @Override
    public String getFieldName() {
        return SpecialFieldsUtils.FIELDNAME_RELEVANCE;
    }

    public static Relevance getInstance() {
        if (Relevance.INSTANCE == null) {
            Relevance.INSTANCE = new Relevance();
        }
        return Relevance.INSTANCE;
    }

    @Override
    public Icon getRepresentingIcon() {
        return this.getValues().get(0).getIcon();
    }

    @Override
    public String getToolTip() {
        return this.getValues().get(0).getToolTipText();
    }

    @Override
    public String getMenuString() {
        return Localization.lang("Relevance");
    }

    @Override
    public boolean isSingleValueField() {
        return true;
    }
}
