package org.jabref.gui.actions;

import java.util.Optional;

import org.jabref.gui.IconTheme;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

public enum ActionsFX {

    COPY_MORE(Localization.lang("Copy") + "..."),
    COPY_TITLE(Localization.lang("Copy title"), KeyBinding.COPY_TITLE),
    COPY_KEY(Localization.lang("Copy BibTeX key"), KeyBinding.COPY_BIBTEX_KEY),
    COPY_CITE_KEY(Localization.lang("Copy \\cite{BibTeX key}"), KeyBinding.COPY_CITE_BIBTEX_KEY),
    COPY_KEY_AND_TITLE(Localization.lang("Copy BibTeX key and title"), KeyBinding.COPY_BIBTEX_KEY_AND_TITLE),
    COPY_KEY_AND_LINK(Localization.lang("Copy BibTeX key and link"), KeyBinding.COPY_BIBTEX_KEY_AND_LINK),
    // TODO COPY_CITATION_HTML(Localization.menuTitle("Copy citation") + " (HTML)", KeyBinding.COPY_PREVIEW),
    COPY_CITATION_MORE(Localization.menuTitle("Copy citation") + "..."),
    COPY_CITATION_TEXT("Text"),
    COPY_CITATION_RTF("RTF"),
    COPY_CITATION_ASCII_DOC("AsciiDoc"),
    COPY_CITATION_XSLFO("XSL-FO"),
    COPY_CITATION_HTML(Localization.lang("Copy preview"), KeyBinding.COPY_PREVIEW),
    EXPORT_TO_CLIPBOARD(Localization.lang("Export to clipboard"), IconTheme.JabRefIcons.EXPORT_TO_CLIPBOARD),
    COPY(Localization.lang("Copy"), IconTheme.JabRefIcons.COPY, KeyBinding.COPY),
    PASTE(Localization.lang("Paste"), IconTheme.JabRefIcons.PASTE, KeyBinding.PASTE),
    CUT(Localization.lang("Cut"), IconTheme.JabRefIcons.CUT, KeyBinding.CUT),
    DELETE(Localization.lang("Delete"), IconTheme.JabRefIcons.DELETE_ENTRY, KeyBinding.DELETE_ENTRY),
    SEND_AS_EMAIL(Localization.lang("Send as email"), IconTheme.JabRefIcons.EMAIL),
    OPEN_FOLDER(Localization.lang("Open folder"), KeyBinding.OPEN_FOLDER),
    OPEN_EXTERNAL_FILE(Localization.lang("Open file"), KeyBinding.OPEN_FILE),
    OPEN_URL(Localization.lang("Open URL or DOI"), IconTheme.JabRefIcons.WWW, KeyBinding.OPEN_URL_OR_DOI),
    MERGE_WITH_FETCHED_ENTRY(Localization.lang("Get BibTeX data from %0", "DOI/ISBN/...")),
    ADD_FILE_LINK(Localization.lang("Attach file"), IconTheme.JabRefIcons.ATTACH_FILE),
    MERGE_ENTRIES(Localization.lang("Merge entries") + "...", IconTheme.JabRefIcons.MERGE_ENTRIES),
    ADD_TO_GROUP(Localization.lang("Add to group")),
    REMOVE_FROM_GROUP(Localization.lang("Remove from group")),
    MOVE_TO_GROUP(Localization.lang("Move to group"));

    private final String text;
    private final Optional<IconTheme.JabRefIcons> icon;
    private final Optional<KeyBinding> keyBinding;

    ActionsFX(String text) {
        this.text = text;
        this.icon = Optional.empty();
        this.keyBinding = Optional.empty();
    }

    ActionsFX(String text, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    ActionsFX(String text, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    ActionsFX(String text, KeyBinding keyBinding) {
        this.text = text;
        this.keyBinding = Optional.of(keyBinding);
        this.icon = Optional.empty();
    }

    public Optional<IconTheme.JabRefIcons> getIcon() {
        return icon;
    }

    public Optional<KeyBinding> getKeyBinding() {
        return keyBinding;
    }

    public String getText() {
        return text;
    }
}
