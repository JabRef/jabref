package org.jabref.model.entry.types;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.entry.BibEntryType;

import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Artwork;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Audio;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Bibnote;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Commentary;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Image;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Jurisdiction;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Legal;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Legislation;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Letter;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Movie;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Music;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Performance;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Review;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Standard;
import static org.jabref.model.entry.types.BiblatexNonStandardEntryType.Video;

public class BiblatexNonStandardEntryTypeDefinitions {

    private static final BibEntryType ARTWORK = new BibEntryType(Artwork, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType AUDIO = new BibEntryType(Audio, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType BIBNOTE = new BibEntryType(Bibnote, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType COMMENTARY = new BibEntryType(Commentary, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType IMAGE = new BibEntryType(Image, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType JURISDICTION = new BibEntryType(Jurisdiction, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType LEGISLATION = new BibEntryType(Legislation, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType LEGAL = new BibEntryType(Legal, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType LETTER = new BibEntryType(Letter, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType MOVIE = new BibEntryType(Movie, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType MUSIC = new BibEntryType(Music, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType PERFORMANCE = new BibEntryType(Performance, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType REVIEW = new BibEntryType(Review, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType STANDARD = new BibEntryType(Standard, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());
    private static final BibEntryType VIDEO = new BibEntryType(Video, BiblatexEntryTypeDefinitions.getMisc().getAllBibFields(), BiblatexEntryTypeDefinitions.getMisc().getRequiredFields());

    public static final List<BibEntryType> ALL = Arrays.asList(ARTWORK, AUDIO, BIBNOTE, COMMENTARY, IMAGE, JURISDICTION, LEGISLATION, LEGAL,
            LETTER, MOVIE, MUSIC, PERFORMANCE, REVIEW, STANDARD, VIDEO);
}

