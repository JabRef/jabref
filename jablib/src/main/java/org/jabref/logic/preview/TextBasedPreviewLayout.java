package org.jabref.logic.preview;

import java.io.IOException;
import java.io.Reader;

import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Implements the preview based JabRef's <a href="https://docs.jabref.org/import-export/export/customexports">Custom export filters</a>.
/// Caching supports only one instance
public final class TextBasedPreviewLayout implements PreviewLayout {
    public static final String NAME = "PREVIEW";
    public static final String DEFAULT = "<font face=\"sans-serif\">" +
            "<b>\\bibtextype</b><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>\\end{citationkey}__NEWLINE__" +
            "\\begin{author}<BR><BR>\\format[Authors(LastFirst, FullName,Sep= / ,LastSep= / ),HTMLChars]{\\author}\\end{author}__NEWLINE__" +
            "\\begin{editor & !author}<BR><BR>\\format[Authors(LastFirst,FullName,Sep= / ,LastSep= / ),HTMLChars]{\\editor} (\\format[IfPlural(Eds.,Ed.)]{\\editor})\\end{editor & !author}__NEWLINE__" +
            "\\begin{title}<BR><b>\\format[HTMLChars]{\\title}</b> \\end{title}__NEWLINE__" +
            "<BR>\\begin{date}\\date\\end{date}\\begin{edition}, \\edition. edition\\end{edition}__NEWLINE__" +
            "\\begin{editor & author}<BR><BR>\\format[Authors(LastFirst,FullName,Sep= / ,LastSep= / ),HTMLChars]{\\editor} (\\format[IfPlural(Eds.,Ed.)]{\\editor})\\end{editor & author}__NEWLINE__" +
            "\\begin{booktitle}<BR><i>\\format[HTMLChars]{\\booktitle}</i>\\end{booktitle}__NEWLINE__" +
            "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}" +
            "\\begin{editor & !author}<BR>\\end{editor & !author}\\begin{!editor}<BR>\\end{!editor}\\begin{journal}<BR><i>\\format[HTMLChars]{\\journal}</i> \\end{journal} \\begin{volume}, Vol. \\volume\\end{volume}\\begin{series}<BR>\\format[HTMLChars]{\\series}\\end{series}\\begin{number}, No. \\format[HTMLChars]{\\number}\\end{number}__NEWLINE__" +
            "\\begin{school} \\format[HTMLChars]{\\school}, \\end{school}__NEWLINE__" +
            "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__" +
            "\\begin{publisher}<BR>\\format[HTMLChars]{\\publisher}\\end{publisher}\\begin{location}: \\format[HTMLChars]{\\location} \\end{location}__NEWLINE__" +
            "\\begin{pages}<BR> p. \\format[FormatPagesForHTML]{\\pages}\\end{pages}__NEWLINE__" +
            "\\begin{doi}<BR>doi <a href=\"https://doi.org/\\format[DOIStrip]{\\doi}\">\\format[DOIStrip]{\\doi}</a>\\end{doi}__NEWLINE__" +
            "\\begin{url}<BR>URL <a href=\"\\url\">\\url</a>\\end{url}__NEWLINE__" +
            "\\begin{abstract}<BR><BR><b>Abstract: </b>\\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__" +
            "\\begin{owncitation}<BR><BR><b>Own citation: </b>\\format[HTMLChars]{\\owncitation} \\end{owncitation}__NEWLINE__" +
            "\\begin{comment}<BR><BR><b>Comment: </b>\\format[Markdown,HTMLChars(keepCurlyBraces)]{\\comment}\\end{comment}__NEWLINE__" +
            "</font>__NEWLINE__";

    private static final Logger LOGGER = LoggerFactory.getLogger(TextBasedPreviewLayout.class);
    private Layout layout;
    private String text;
    private LayoutFormatterPreferences layoutFormatterPreferences;
    private JournalAbbreviationRepository abbreviationRepository;

    public TextBasedPreviewLayout(String text,
                                  @NonNull LayoutFormatterPreferences layoutFormatterPreferences,
                                  @NonNull JournalAbbreviationRepository abbreviationRepository) {
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.abbreviationRepository = abbreviationRepository;
        setText(text);
    }

    public TextBasedPreviewLayout(Layout layout) {
        this.layout = layout;
        this.text = layout.getText();
    }

    public void setText(String text) {
        this.text = text;
        Reader reader = Reader.of(text.replace("__NEWLINE__", "\n"));
        try {
            layout = new LayoutHelper(reader, layoutFormatterPreferences, abbreviationRepository).getLayoutFromText();
        } catch (IOException e) {
            LOGGER.error("Could not generate layout", e);
        }
    }

    @Override
    public String generatePreview(BibEntry entry, BibDatabaseContext databaseContext) {
        if (layout != null) {
            return layout.doLayout(entry, databaseContext.getDatabase());
        } else {
            return "";
        }
    }

    @Override
    public String getText() {
        return text.replace("__NEWLINE__", "\n");
    }

    @Override
    public String getName() {
        return NAME;
    }

    public String getShortTitle() {
        return NAME;
    }

    @Override
    public String getDisplayName() {
        return Localization.lang("Customized preview style");
    }

    public static TextBasedPreviewLayout of(@NonNull String style,
                                            @NonNull LayoutFormatterPreferences layoutFormatterPreferences,
                                            @NonNull JournalAbbreviationRepository abbreviationRepository) {
        return new TextBasedPreviewLayout(
                style,
                layoutFormatterPreferences,
                abbreviationRepository);
    }
}
