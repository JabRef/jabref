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

    private final Map<String, LwtaAbbreviation> lwtaToAbbreviationObject;

    // incomplete list
    private final String[] WORDS_TO_REMOVE = new String[]{"the", "and", "&", "of", "but", "sans", "section", "series", "part"};

    /**
     * instantiates this class with a csv file
     */
    public LwtaAbbreviationRepository(Path file) throws IOException {
        AbbreviationParser parser = new AbbreviationParser();
        lwtaToAbbreviationObject = new HashMap<>();

        parser.readLwtaAbbreviations(file);
        Collection<LwtaAbbreviation> abbreviations = parser.getLwtaAbbreviations();

        for (LwtaAbbreviation abbreviation : abbreviations) {
            lwtaToAbbreviationObject.put(abbreviation.getUnAbbreviated(), abbreviation);
        }
    }

    /**
     * returns true if the abbreviation can be applied to the word given
     */
    private boolean canAbbreviate(String word, LwtaAbbreviation lwtaAbbreviation) {
        return switch (lwtaAbbreviation.getPosition()) {
            case IN_WORD ->
                    word.contains(lwtaAbbreviation.getUnAbbreviated());
            case STARTS_WORD ->
                    word.startsWith(lwtaAbbreviation.getUnAbbreviated());
            case ENDS_WORD ->
                    word.endsWith(lwtaAbbreviation.getUnAbbreviated());
            default ->
                    false;
        };
    }

    /**
     * abbreviates the word with the set of abbreviations. Recursive, prefers abbreviating the longest chunks first.
     */
    private String abbreviateWord(String word, Set<String> abbreviations) {
        List<String> possibleAbbreviations = new ArrayList<>();

        // We need to keep capitalisation for our final result, but lower case allows us to compare
        boolean capitalised = Character.isUpperCase(word.charAt(0));
        String wordLowerCase = word.toLowerCase();

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
        LwtaAbbreviation abbreviationUsed = lwtaToAbbreviationObject.get(possibleAbbreviations.getFirst());

        Set<String> possibleAbbSet = new HashSet<>(possibleAbbreviations);
        String wordAbb = "";

        switch (abbreviationUsed.getPosition()) {
            case ENDS_WORD -> {
                for (int i = 0; i < word.length(); i++) {
                    String head = word.substring(0, i);
                    String tail = word.substring(i);
                    if (tail.equalsIgnoreCase(abbreviationUsed.getUnAbbreviated())) {
                        String prefix = "";
                        if (abbreviationUsed.getAllowsPrefix()) {
                            prefix = abbreviateWord(head, possibleAbbSet);
                        }

                        wordAbb = prefix + abbreviationUsed.getAbbreviation();
                    }
                }
            }
            case STARTS_WORD -> {
                for (int i = 0; i < word.length(); i++) {
                    String head = word.substring(0, i);
                    String tail = word.substring(i);
                    if (head.equalsIgnoreCase(abbreviationUsed.getUnAbbreviated())) {
                        String suffix = "";
                        if (abbreviationUsed.getAllowsSuffix()) {
                            suffix = abbreviateWord(tail, possibleAbbSet);
                        }

                        wordAbb = abbreviationUsed.getAbbreviation() + suffix;
                    }
                }
            }
            case IN_WORD -> {
                String[] unAbbreviatedPieces = word.split(abbreviationUsed.getAbbreviation(), 2);
                if (unAbbreviatedPieces.length == 0) {
                    return abbreviationUsed.getAbbreviation();
                }
                String head = unAbbreviatedPieces[0];
                String tail = unAbbreviatedPieces[1];
                String prefix = "";
                if (abbreviationUsed.getAllowsPrefix()) {
                    prefix = abbreviateWord(head, possibleAbbSet);
                }
                String suffix = "";
                if (abbreviationUsed.getAllowsSuffix()) {
                    suffix = abbreviateWord(tail, possibleAbbSet);
                }
                wordAbb = prefix + abbreviationUsed.getAbbreviation() + suffix;
            }
            default ->
                    wordAbb = word;
        }

        // Now capitalise the abbreviation correctly:
        if (capitalised && wordAbb.length() > 0) {
            wordAbb = wordAbb.substring(0, 1).toUpperCase() + wordAbb.substring(1);
        }

        return wordAbb;
    }

    /**
     * turns a journal name into its lwta abbreviation
     */
    String abbreviateJournalName(String name) {
        // Remove commas and replace full stops with commas
        name = name.replace(",", "");
        name = name.replace(".", ",");

        // Split into words:
        String[] words = name.split(" ");
        ArrayList<String> wordsToBeAbbreviated = new ArrayList<>();
        ArrayList<String> abbreviatedWords = new ArrayList<>();

        // Remove articles/prepositions
        for (String word : words) {
            boolean removeWord = false;

            for (String wordToRemove : WORDS_TO_REMOVE) {
                if (word.equalsIgnoreCase(wordToRemove)) {
                    removeWord = true;
                    break;
                }
            }

            if (!removeWord) {
                wordsToBeAbbreviated.add(word);
            }
        }

        // Single word titles should not be abbreviated:
        if (wordsToBeAbbreviated.size() == 1) {
            return wordsToBeAbbreviated.getFirst();
        }

        // Abbreviate each word:
        for (String word : wordsToBeAbbreviated) {
            String abbreviated = word;
            boolean abbreviatedAlready = false;
            for (String unAbbreviated : lwtaToAbbreviationObject.keySet()) {
                // If the word is just punctuation and an abbreviation, just use that:
                String lowerWord = word.toLowerCase();
                if (lowerWord.contains(unAbbreviated.toLowerCase()) && lowerWord.replaceAll("[^\\sa-zA-Z0-9]", "").equalsIgnoreCase(unAbbreviated)) {
                    abbreviated = lwtaToAbbreviationObject.get(unAbbreviated).getAbbreviation();
                    // Fix capitalisation:
                    if (Character.isUpperCase(word.charAt(0))) {
                        abbreviated = abbreviated.substring(0, 1).toUpperCase() + abbreviated.substring(1);
                    }

                    abbreviatedAlready = true;
                    break;
                }
            }

            if (!abbreviatedAlready) {
                abbreviated = abbreviateWord(word, lwtaToAbbreviationObject.keySet());
            }

            abbreviatedWords.add(abbreviated);
        }

        // put the abbreviated words back together
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < abbreviatedWords.size() - 1; i++) {
            sb.append(abbreviatedWords.get(i));
            sb.append(" ");
        }
        sb.append(abbreviatedWords.getLast());

        return sb.toString();
    }
}

