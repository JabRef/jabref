package org.jabref.gui.externalfiletype;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;

public enum StandardExternalFileType implements ExternalFileType {

    PDF("PDF", "pdf", "application/pdf", "evince", "pdfSmall", IconTheme.JabRefIcons.PDF_FILE),
    PostScript("PostScript", "ps", "application/postscript", "evince", "psSmall", IconTheme.JabRefIcons.FILE),
    Word("Word", "doc", "application/msword", "oowriter", "openoffice", IconTheme.JabRefIcons.FILE_WORD),
    Word_NEW("Word 2007+", "docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "oowriter", "openoffice", IconTheme.JabRefIcons.FILE_WORD),
    OpenDocument_TEXT(Localization.lang("OpenDocument text"), "odt", "application/vnd.oasis.opendocument.text", "oowriter", "openoffice", IconTheme.JabRefIcons.FILE_OPENOFFICE),
    Excel("Excel", "xls", "application/excel", "oocalc", "openoffice", IconTheme.JabRefIcons.FILE_EXCEL),
    Excel_NEW("Excel 2007+", "xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "oocalc", "openoffice", IconTheme.JabRefIcons.FILE_EXCEL),
    OpenDocumentSpreadsheet(Localization.lang("OpenDocument spreadsheet"), "ods", "application/vnd.oasis.opendocument.spreadsheet", "oocalc", "openoffice", IconTheme.JabRefIcons.FILE_OPENOFFICE),
    PowerPoint("PowerPoint", "ppt", "application/vnd.ms-powerpoint", "ooimpress", "openoffice", IconTheme.JabRefIcons.FILE_POWERPOINT),
    PowerPoint_NEW("PowerPoint 2007+", "pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "ooimpress", "openoffice", IconTheme.JabRefIcons.FILE_POWERPOINT),
    OpenDocumentPresentation(Localization.lang("OpenDocument presentation"), "odp", "application/vnd.oasis.opendocument.presentation", "ooimpress", "openoffice", IconTheme.JabRefIcons.FILE_OPENOFFICE),
    RTF("Rich Text Format", "rtf", "application/rtf", "oowriter", "openoffice", IconTheme.JabRefIcons.FILE_TEXT),
    PNG(Localization.lang("%0 image", "PNG"), "png", "image/png", "gimp", "picture", IconTheme.JabRefIcons.PICTURE),
    GIF(Localization.lang("%0 image", "GIF"), "gif", "image/gif", "gimp", "picture", IconTheme.JabRefIcons.PICTURE),
    JPG(Localization.lang("%0 image", "JPG"), "jpg", "image/jpeg", "gimp", "picture", IconTheme.JabRefIcons.PICTURE),
    Djvu("Djvu", "djvu", "image/vnd.djvu", "evince", "psSmall", IconTheme.JabRefIcons.FILE),
    TXT("Text", "txt", "text/plain", "emacs", "emacs", IconTheme.JabRefIcons.FILE_TEXT),
    TEX("LaTeX", "tex", "application/x-latex", "emacs", "emacs", IconTheme.JabRefIcons.FILE_TEXT),
    CHM("CHM", "chm", "application/mshelp", "gnochm", "www", IconTheme.JabRefIcons.WWW),
    TIFF(Localization.lang("%0 image", "TIFF"), "tiff", "image/tiff", "gimp", "picture", IconTheme.JabRefIcons.PICTURE),
    URL("URL", "html", "text/html", "firefox", "www", IconTheme.JabRefIcons.WWW),
    MHT("MHT", "mht", "multipart/related", "firefox", "www", IconTheme.JabRefIcons.WWW),
    ePUB("ePUB", "epub", "application/epub+zip", "firefox", "www", IconTheme.JabRefIcons.WWW);

    private final String name;
    private final String extension;
    private final String mimeType;
    private final String openWith;
    private final String iconName;
    private final JabRefIcon icon;

    StandardExternalFileType(String name, String extension, String mimeType,
                             String openWith, String iconName, JabRefIcon icon) {
        this.name = name;
        this.extension = extension;
        this.mimeType = mimeType;
        this.openWith = openWith;
        this.iconName = iconName;
        this.icon = icon;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public String getMimeType() {
        return mimeType;
    }

    @Override
    public String getOpenWithApplication() {
        // On all OSes there is a generic application available to handle file opening, so use this one
        return "";
    }

    @Override
    public JabRefIcon getIcon() {
        return icon;
    }
}
