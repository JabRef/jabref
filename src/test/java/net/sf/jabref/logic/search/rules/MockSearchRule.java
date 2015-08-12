/**
 * Copyright (C) 2015 JabRef contributors
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
package net.sf.jabref.logic.search.rules;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.logic.search.SearchRule;

/**
 * Mock search rule that returns the values passed. Useful for testing.
 */
public class MockSearchRule implements SearchRule {

    private final boolean result;
    private final boolean valid;

    public MockSearchRule(boolean result, boolean valid) {
        this.result = result;
        this.valid = valid;
    }

    @Override
    public boolean applyRule(String query, BibtexEntry bibtexEntry) {
        return result;
    }

    @Override
    public boolean validateSearchStrings(String query) {
        return valid;
    }
}
