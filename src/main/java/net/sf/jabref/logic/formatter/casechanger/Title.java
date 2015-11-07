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
package net.sf.jabref.logic.formatter.casechanger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Represents a title of a bibtex entry.
 */
public final class Title {

    private final List<Word> words = new LinkedList<>();

    public Title(String title) {
        this.words.addAll(new TitleParser().parse(title));
    }

    public List<Word> getWords() {
        return words;
    }

    public Optional<Word> getFirstWord() {
        if (getWords().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(getWords().get(0));
    }

    public Optional<Word> getLastWord() {
        if (getWords().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(getWords().get(getWords().size() - 1));
    }

    @Override
    public String toString() {
        return words.stream().map(Word::toString).collect(Collectors.joining(" "));
    }

}
