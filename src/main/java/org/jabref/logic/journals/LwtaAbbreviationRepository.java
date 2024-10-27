package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LwtaAbbreviationRepository {

    private Map<String, LwtaAbbreviation> lwtaToAbbreviationObject;

    // incomplete list
    private final String[] WORDS_TO_REMOVE = new String[]{"the", "and", "&", "of", "but", "sans", "section", "series", "part"};

    public LwtaAbbreviationRepository(Path file) throws IOException {
        AbbreviationParser parser = new AbbreviationParser();
        lwtaToAbbreviationObject = new HashMap<>();

        parser.readLwtaAbbreviations(file);
        Collection<LwtaAbbreviation> abbreviations = parser.getLwtaAbbreviations();

        for (LwtaAbbreviation abbreviation : abbreviations) {
            lwtaToAbbreviationObject.put(abbreviation.getUnAbbreviated(), abbreviation);
        }
    }

    private boolean canAbbreviate(String word, LwtaAbbreviation lwtaAbbreviation) {
        switch (lwtaAbbreviation.getPosition()) {
            case IN_WORD:
                return word.contains(lwtaAbbreviation.getUnAbbreviated());
            case STARTS_WORD:
                return word.startsWith(lwtaAbbreviation.getUnAbbreviated());
            case ENDS_WORD:
                return word.endsWith(lwtaAbbreviation.getUnAbbreviated());
            default:
                return false;
        }
    }

    private String abbreviateWord(String word, Set<String> abbreviations) {
        List<String> possibleAbbreviations = new ArrayList<>();

        // Turn to lower case so we can compare case-insensitively
        String wordLowerCase = new String(word).toLowerCase();

        for (String abbreviation : abbreviations) {
            LwtaAbbreviation lwtaAbbreviation = lwtaToAbbreviationObject.get(abbreviation);

            if (canAbbreviate(wordLowerCase, lwtaAbbreviation)) {
                possibleAbbreviations.add(abbreviation);
            }
        }

        if (possibleAbbreviations.isEmpty()) {
            return word;
        }

        // Now we have to decide conflicts -- for example, maybe both "balti-" and "baltimore" are matched. We'll go by the longer abbreviation first
        possibleAbbreviations.sort((String string1, String string2) -> string2.length() - string1.length());
        LwtaAbbreviation abbreviationUsed = lwtaToAbbreviationObject.get(possibleAbbreviations.get(0));

        Set<String> possibleAbbSet = new HashSet<>(possibleAbbreviations);
        switch (abbreviationUsed.getPosition()) {
            case ENDS_WORD:
                for (int i = 0; i < word.length(); i++) {
                    String head = word.substring(0, i);
                    String tail = word.substring(i, word.length());
                    if (tail.equalsIgnoreCase(abbreviationUsed.getUnAbbreviated())) {
                        return abbreviateWord(head, possibleAbbSet) + abbreviationUsed.getAbbreviation();
                    }
                }
                break;

            case STARTS_WORD:
                for (int i = 0; i < word.length(); i++) {
                    String head = word.substring(0, i);
                    String tail = word.substring(i, word.length());
                    if (head.equalsIgnoreCase(abbreviationUsed.getUnAbbreviated())) {
                        return abbreviationUsed.getAbbreviation() + abbreviateWord(tail, possibleAbbSet);
                    }
                }
                break;

            case IN_WORD:
                String[] unAbbreviatedPieces = word.split(abbreviationUsed.getAbbreviation(), 2);
                if (unAbbreviatedPieces.length == 0) {
                    return abbreviationUsed.getAbbreviation();
                }
                String head = unAbbreviatedPieces[0];
                String tail = unAbbreviatedPieces[1];
                return abbreviateWord(head, possibleAbbSet) + abbreviationUsed.getAbbreviation() + abbreviateWord(tail, possibleAbbSet);

            default:
                return word;
        }
        return word;
    }

    String abbreviateJournalName(String name) {
        // Remove commas and replace full stops with commas
        name.replaceAll(",", "");
        name.replaceAll(".", ",");

        // Split into words:
        String[] words = name.split(" ");
        ArrayList<String> wordsToBeAbbreviated = new ArrayList<>();
        ArrayList<String> abbreviatedWords = new ArrayList<>();

        // Remove articles/prepositions
        for (String word : words) {
            boolean removeWord = false;

            for (int i = 0; i < WORDS_TO_REMOVE.length; i++) {
                if (word.equalsIgnoreCase(WORDS_TO_REMOVE[i])) {
                    removeWord = true;
                    break;
                }
            }

            if (!removeWord) {
                wordsToBeAbbreviated.add(word);
            }
        }

        // Single word titles not abbreviated:
        if (wordsToBeAbbreviated.size() == 1) {
            return wordsToBeAbbreviated.get(0);
        }

        // Abbreviate each word:
        for (String word : wordsToBeAbbreviated) {
            String abbreviated = word;
            for (String unAbbreviated : lwtaToAbbreviationObject.keySet()) {
                // If the word is just punction and an abbreviation, just use that:
                if (word.toLowerCase().contains(unAbbreviated.toLowerCase()) && word.replaceAll("[^\\sa-zA-Z0-9]", "").equalsIgnoreCase(unAbbreviated)) {
                    abbreviated = lwtaToAbbreviationObject.get(unAbbreviated).getAbbreviation();
                    break;
                }
                abbreviated = abbreviateWord(word, lwtaToAbbreviationObject.keySet());
            }
            abbreviatedWords.add(abbreviated);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < abbreviatedWords.size() - 1; i++) {
            sb.append(abbreviatedWords.get(i) + " ");
        }
        sb.append(abbreviatedWords.get(abbreviatedWords.size() - 1));

        return sb.toString();
    }
}

