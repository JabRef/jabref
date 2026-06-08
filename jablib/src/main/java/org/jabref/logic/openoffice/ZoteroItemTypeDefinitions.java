package org.jabref.logic.openoffice;

import java.util.Map;

import org.jabref.model.entry.types.BiblatexApaEntryType;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.IEEETranEntryType;
import org.jabref.model.entry.types.StandardEntryType;

class ZoteroItemTypeDefinitions {
    private static final Map<String, EntryType> ITEM_TYPES = Map.ofEntries(
            Map.entry("article-journal", StandardEntryType.Article),
            Map.entry("article-magazine", StandardEntryType.Article),
            Map.entry("article-newspaper", StandardEntryType.Article),
            Map.entry("bill", BiblatexApaEntryType.Legislation),
            Map.entry("book", StandardEntryType.Book),
            Map.entry("broadcast", StandardEntryType.Misc),
            Map.entry("chapter", StandardEntryType.InCollection),
            Map.entry("data", StandardEntryType.Dataset),
            Map.entry("dataset", StandardEntryType.Dataset),
            Map.entry("entry-dictionary", StandardEntryType.InReference),
            Map.entry("entry-encyclopedia", StandardEntryType.InReference),
            Map.entry("figure", BiblatexNonStandardEntryType.Image),
            Map.entry("graphic", BiblatexNonStandardEntryType.Image),
            Map.entry("hearing", BiblatexApaEntryType.Jurisdiction),
            Map.entry("instantMessage", StandardEntryType.Misc),
            Map.entry("interview", StandardEntryType.Misc),
            Map.entry("legal_case", BiblatexApaEntryType.Jurisdiction),
            Map.entry("legislation", BiblatexApaEntryType.Legislation),
            Map.entry("manuscript", StandardEntryType.Unpublished),
            Map.entry("map", StandardEntryType.Misc),
            Map.entry("motion_picture", BiblatexNonStandardEntryType.Movie),
            Map.entry("musical_score", BiblatexNonStandardEntryType.Audio),
            Map.entry("pamphlet", StandardEntryType.Booklet),
            Map.entry("paper-conference", StandardEntryType.InProceedings),
            Map.entry("patent", IEEETranEntryType.Patent),
            Map.entry("personal_communication", BiblatexNonStandardEntryType.Letter),
            Map.entry("post-weblog", StandardEntryType.Online),
            Map.entry("report", StandardEntryType.Report),
            Map.entry("review", BiblatexNonStandardEntryType.Review),
            Map.entry("review-book", BiblatexNonStandardEntryType.Review),
            Map.entry("song", BiblatexNonStandardEntryType.Music),
            Map.entry("speech", StandardEntryType.Misc),
            Map.entry("thesis", StandardEntryType.Thesis),
            Map.entry("treaty", BiblatexApaEntryType.Legal),
            Map.entry("webpage", StandardEntryType.Online)
    );
}
