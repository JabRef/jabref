/*
 Copyright (C) 2003 Morten O. Alver, Nizar N. Batada

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
package net.sf.jabref.groups;

import java.util.Map;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.SearchRule;
import net.sf.jabref.SearchRuleSet;

/**
 * Subclass of SearchRuleSet that ANDs or ORs between its rules, eturning 0 or
 * 1.
 */
class AndOrSearchRuleSet extends SearchRuleSet {

    private boolean and, invert;

    public AndOrSearchRuleSet(boolean and, boolean invert) {
        this.and = and;
        this.invert = invert;
    }

    public int applyRule(Map<String, String> searchString, BibtexEntry bibtexEntry) {
        int score = 0;
        
        // We let each rule add a maximum of 1 to the score.
        for (SearchRule rule : ruleSet) {
			score += rule.applyRule(searchString, bibtexEntry) > 0 ? 1 : 0;
		}

        // Then an AND rule demands that score == number of rules, and
        // an OR rule demands score > 0.
        boolean res;
        if (and)
            res = (score == ruleSet.size());
        else
            res = (score > 0);

        if (invert)
            return (res ? 0 : 1);
        return (res ? 1 : 0);
    }
}
