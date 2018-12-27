package org.jabref.logic.formatter.casechanger;

import java.util.Collections;
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
        return Collections.unmodifiableList(words);
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
