/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.logic.l10n;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * A bundle containing localized strings.
 * It wraps an ordinary resource bundle and performs escaping/unescaping of keys and values similar to
 * {@link Localization}. Needed to support JavaFX inline binding.
 */
public class LocalizationBundle extends ResourceBundle {

    private final ResourceBundle baseBundle;

    public LocalizationBundle(ResourceBundle baseBundle) {
        this.baseBundle = Objects.requireNonNull(baseBundle);
    }

    @Override
    protected Object handleGetObject(String key) {
        return Localization.translate(baseBundle, "message", key);
    }

    @Override
    public Enumeration<String> getKeys() {
        ArrayList<String> baseKeys = Collections.list(baseBundle.getKeys());
        List<String> unescapedKeys = baseKeys.stream().map(key -> new LocalizationKey(key).getTranslationValue())
                .collect(Collectors.toList());
        return Collections.enumeration(unescapedKeys);
    }
}
