package org.jabref.logic.msbib;

/**
 * This class represents all supported MSBib entry types.
 * <p>
 * Book, BookSection, JournalArticle, ArticleInAPeriodical, ConferenceProceedings, Report,
 * InternetSite, DocumentFromInternetSite, ElectronicSource, Art, SoundRecording, Performance,
 * Film, Interview, Patent, Case, Misc
 *
 * See BIBFORM.XML, shared-bibliography.xsd (ECMA standard)
 */
public enum MSBibEntryType {
    ArticleInAPeriodical,
    Book,
    BookSection,
    JournalArticle,
    ConferenceProceedings,
    Report,
    SoundRecording,
    Performance,
    Art,
    DocumentFromInternetSite,
    InternetSite,
    Film,
    Interview,
    Patent,
    ElectronicSource,
    Case,
    Misc
}
