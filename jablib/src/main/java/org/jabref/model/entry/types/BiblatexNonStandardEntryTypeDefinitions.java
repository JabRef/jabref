package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;

import static org.jabref.model.entry.types.BiblatexEntryTypeDefinitions.MISC;
import static org.jabref.model.entry.types.StandardEntryType.Artwork;
import static org.jabref.model.entry.types.StandardEntryType.Audio;
import static org.jabref.model.entry.types.StandardEntryType.Bibnote;
import static org.jabref.model.entry.types.StandardEntryType.Commentary;
import static org.jabref.model.entry.types.StandardEntryType.Image;
import static org.jabref.model.entry.types.StandardEntryType.Jurisdiction;
import static org.jabref.model.entry.types.StandardEntryType.Legislation;
import static org.jabref.model.entry.types.StandardEntryType.Legal;
import static org.jabref.model.entry.types.StandardEntryType.Letter;
import static org.jabref.model.entry.types.StandardEntryType.Movie;
import static org.jabref.model.entry.types.StandardEntryType.Music;
import static org.jabref.model.entry.types.StandardEntryType.Performance;
import static org.jabref.model.entry.types.StandardEntryType.Review;
import static org.jabref.model.entry.types.StandardEntryType.Standard;
import static org.jabref.model.entry.types.StandardEntryType.Video;

public class BiblatexNonStandardEntryTypeDefinitions {

    private static final BibEntryType ARTWORK = MISC.withType(Artwork);
    private static final BibEntryType AUDIO = MISC.withType(Audio);
    private static final BibEntryType BIBNOTE = MISC.withType(Bibnote);
    private static final BibEntryType COMMENTARY = MISC.withType(Commentary);
    private static final BibEntryType IMAGE = MISC.withType(Image);
    private static final BibEntryType JURISDICTION = MISC.withType(Jurisdiction);
    private static final BibEntryType LEGISLATION = MISC.withType(Legislation);
    private static final BibEntryType LEGAL = MISC.withType(Legal);
    private static final BibEntryType LETTER = MISC.withType(Letter);
    private static final BibEntryType MOVIE = MISC.withType(Movie);
    private static final BibEntryType MUSIC = MISC.withType(Music);
    private static final BibEntryType PERFORMANCE = MISC.withType(Performance);
    private static final BibEntryType REVIEW = MISC.withType(Review);
    private static final BibEntryType STANDARD = MISC.withType(Standard);
    private static final BibEntryType VIDEO = MISC.withType(Video);

    public static final List<BibEntryType> ALL = Arrays.asList(ARTWORK, AUDIO, BIBNOTE, COMMENTARY, IMAGE, JURISDICTION, LEGISLATION, LEGAL,
            LETTER, MOVIE, MUSIC, PERFORMANCE, REVIEW, STANDARD, VIDEO);
}

