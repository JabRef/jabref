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
import static org.jabref.model.entry.types.StandardEntryType.Legal;
import static org.jabref.model.entry.types.StandardEntryType.Legislation;
import static org.jabref.model.entry.types.StandardEntryType.Letter;
import static org.jabref.model.entry.types.StandardEntryType.Movie;
import static org.jabref.model.entry.types.StandardEntryType.Music;
import static org.jabref.model.entry.types.StandardEntryType.Performance;
import static org.jabref.model.entry.types.StandardEntryType.Review;
import static org.jabref.model.entry.types.StandardEntryType.Standard;
import static org.jabref.model.entry.types.StandardEntryType.Video;

public class BiblatexNonStandardEntryTypeDefinitions {

    private static final BibEntryType ARTWORK = new BibEntryType(Artwork, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType AUDIO = new BibEntryType(Audio, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType BIBNOTE = new BibEntryType(Bibnote, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType COMMENTARY = new BibEntryType(Commentary, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType IMAGE = new BibEntryType(Image, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType JURISDICTION = new BibEntryType(Jurisdiction, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType LEGISLATION = new BibEntryType(Legislation, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType LEGAL = new BibEntryType(Legal, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType LETTER = new BibEntryType(Letter, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType MOVIE = new BibEntryType(Movie, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType MUSIC = new BibEntryType(Music, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType PERFORMANCE = new BibEntryType(Performance, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType REVIEW = new BibEntryType(Review, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType STANDARD = new BibEntryType(Standard, MISC.getAllBibFields(), MISC.getRequiredFields());
    private static final BibEntryType VIDEO = new BibEntryType(Video, MISC.getAllBibFields(), MISC.getRequiredFields());

    public static final List<BibEntryType> ALL = Arrays.asList(ARTWORK, AUDIO, BIBNOTE, COMMENTARY, IMAGE, JURISDICTION, LEGISLATION, LEGAL,
            LETTER, MOVIE, MUSIC, PERFORMANCE, REVIEW, STANDARD, VIDEO);
}

