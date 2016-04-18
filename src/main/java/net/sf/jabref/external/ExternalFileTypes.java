package net.sf.jabref.external;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.FileField;

public final class ExternalFileTypes {

    // This String is used in the encoded list in prefs of external file type
    // modifications, in order to indicate a removed default file type:
    private static final String FILE_TYPE_REMOVED_FLAG = "REMOVED";

    // Map containing all registered external file types:
    private final Set<ExternalFileType> externalFileTypes = new TreeSet<>();

    private final ExternalFileType HTML_FALLBACK_TYPE = new ExternalFileType("URL", "html", "text/html", "", "www",
            IconTheme.JabRefIcon.WWW.getSmallIcon());

    // The only instance of this class:
    private static ExternalFileTypes singleton;


    public static ExternalFileTypes getInstance() {
        if (ExternalFileTypes.singleton == null) {
            ExternalFileTypes.singleton = new ExternalFileTypes();
        }
        return ExternalFileTypes.singleton;
    }

    private ExternalFileTypes() {
        updateExternalFileTypes();
    }

    public static List<ExternalFileType> getDefaultExternalFileTypes() {
        List<ExternalFileType> list = new ArrayList<>();
        list.add(new ExternalFileType("PDF", "pdf", "application/pdf", "evince", "pdfSmall",
                IconTheme.JabRefIcon.PDF_FILE.getSmallIcon()));
        list.add(new ExternalFileType("PostScript", "ps", "application/postscript", "evince", "psSmall",
                IconTheme.JabRefIcon.FILE.getSmallIcon()));
        list.add(new ExternalFileType("Word", "doc", "application/msword", "oowriter", "openoffice",
                IconTheme.JabRefIcon.FILE_WORD.getSmallIcon()));
        list.add(new ExternalFileType("Word 2007+", "docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "oowriter", "openoffice",
                IconTheme.JabRefIcon.FILE_WORD.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("OpenDocument text"), "odt",
                "application/vnd.oasis.opendocument.text", "oowriter", "openoffice", IconTheme.getImage("openoffice")));
        list.add(new ExternalFileType("Excel", "xls", "application/excel", "oocalc", "openoffice",
                IconTheme.JabRefIcon.FILE_EXCEL.getSmallIcon()));
        list.add(new ExternalFileType("Excel 2007+", "xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "oocalc", "openoffice",
                IconTheme.JabRefIcon.FILE_EXCEL.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("OpenDocument spreadsheet"), "ods",
                "application/vnd.oasis.opendocument.spreadsheet", "oocalc", "openoffice",
                IconTheme.getImage("openoffice")));
        list.add(new ExternalFileType("PowerPoint", "ppt", "application/vnd.ms-powerpoint", "ooimpress", "openoffice",
                IconTheme.JabRefIcon.FILE_POWERPOINT.getSmallIcon()));
        list.add(new ExternalFileType("PowerPoint 2007+", "pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation", "ooimpress", "openoffice",
                IconTheme.JabRefIcon.FILE_POWERPOINT.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("OpenDocument presentation"), "odp",
                "application/vnd.oasis.opendocument.presentation", "ooimpress", "openoffice",
                IconTheme.getImage("openoffice")));
        list.add(new ExternalFileType("Rich Text Format", "rtf", "application/rtf", "oowriter", "openoffice",
                IconTheme.JabRefIcon.FILE_TEXT.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "PNG"), "png", "image/png", "gimp", "picture",
                IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "GIF"), "gif", "image/gif", "gimp", "picture",
                IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "JPG"), "jpg", "image/jpeg", "gimp", "picture",
                IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType("Djvu", "djvu", "image/vnd.djvu", "evince", "psSmall",
                IconTheme.JabRefIcon.FILE.getSmallIcon()));
        list.add(new ExternalFileType("Text", "txt", "text/plain", "emacs", "emacs",
                IconTheme.JabRefIcon.FILE_TEXT.getSmallIcon()));
        list.add(new ExternalFileType("LaTeX", "tex", "application/x-latex", "emacs", "emacs",
                IconTheme.JabRefIcon.FILE_TEXT.getSmallIcon()));
        list.add(new ExternalFileType("CHM", "chm", "application/mshelp", "gnochm", "www",
                IconTheme.JabRefIcon.WWW.getSmallIcon()));
        list.add(new ExternalFileType(Localization.lang("%0 image", "TIFF"), "tiff", "image/tiff", "gimp", "picture",
                IconTheme.JabRefIcon.PICTURE.getSmallIcon()));
        list.add(new ExternalFileType("URL", "html", "text/html", "firefox", "www",
                IconTheme.JabRefIcon.WWW.getSmallIcon()));
        list.add(new ExternalFileType("MHT", "mht", "multipart/related", "firefox", "www",
                IconTheme.JabRefIcon.WWW.getSmallIcon()));
        list.add(new ExternalFileType("ePUB", "epub", "application/epub+zip", "firefox", "www",
                IconTheme.JabRefIcon.WWW.getSmallIcon()));

        // On all OSes there is a generic application available to handle file opening,
        // so we don't need the default application settings anymore:
        for (ExternalFileType type : list) {
            type.setOpenWith("");
        }

        return list;
    }

    public Collection<ExternalFileType> getExternalFileTypeSelection() {
        return externalFileTypes;
    }

    /**
     * Look up the external file type registered with this name, if any.
     *
     * @param name The file type name.
     * @return The ExternalFileType registered, or null if none.
     */
    public Optional<ExternalFileType> getExternalFileTypeByName(String name) {
        for (ExternalFileType type : externalFileTypes) {
            if (type.getName().equals(name)) {
                return Optional.of(type);
            }
        }
        // Return an instance that signifies an unknown file type:
        return Optional.of(new UnknownExternalFileType(name));
    }

    /**
     * Look up the external file type registered for this extension, if any.
     *
     * @param extension The file extension.
     * @return The ExternalFileType registered, or null if none.
     */
    public Optional<ExternalFileType> getExternalFileTypeByExt(String extension) {
        for (ExternalFileType type : externalFileTypes) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns true if there is an external file type registered for this extension.
     *
     * @param extension The file extension.
     * @return true if an ExternalFileType with the extension exists, false otherwise
     */
    public boolean isExternalFileTypeByExt(String extension) {
        for (ExternalFileType type : externalFileTypes) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look up the external file type name registered for this extension, if any.
     *
     * @param extension The file extension.
     * @return The name of the ExternalFileType registered, or null if none.
     */
    public String getExternalFileTypeNameByExt(String extension) {
        for (ExternalFileType type : externalFileTypes) {
            if (type.getExtension().equalsIgnoreCase(extension)) {
                return type.getName();
            }
        }
        return "";
    }

    /**
     * Look up the external file type registered for this filename, if any.
     *
     * @param filename The name of the file whose type to look up.
     * @return The ExternalFileType registered, or null if none.
     */
    public Optional<ExternalFileType> getExternalFileTypeForName(String filename) {
        int longestFound = -1;
        ExternalFileType foundType = null;
        for (ExternalFileType type : externalFileTypes) {
            if (!type.getExtension().isEmpty() && filename.toLowerCase().endsWith(type.getExtension().toLowerCase())
                    && (type.getExtension().length() > longestFound)) {
                longestFound = type.getExtension().length();
                foundType = type;
            }
        }
        return Optional.ofNullable(foundType);
    }

    /**
     * Look up the external file type registered for this MIME type, if any.
     *
     * @param mimeType The MIME type.
     * @return The ExternalFileType registered, or null if none. For the mime type "text/html", a valid file type is
     *         guaranteed to be returned.
     */
    public Optional<ExternalFileType> getExternalFileTypeByMimeType(String mimeType) {
        for (ExternalFileType type : externalFileTypes) {
            if (type.getMimeType().equalsIgnoreCase(mimeType)) {
                return Optional.of(type);
            }
        }
        if ("text/html".equalsIgnoreCase(mimeType)) {
            return Optional.of(HTML_FALLBACK_TYPE);
        } else {
            return Optional.empty();
        }
    }

    /**
     * Reset the List of external file types after user customization.
     *
     * @param types The new List of external file types. This is the complete list, not just new entries.
     */
    public void setExternalFileTypes(List<ExternalFileType> types) {

        // First find a list of the default types:
        List<ExternalFileType> defTypes = getDefaultExternalFileTypes();
        // Make a list of types that are unchanged:
        List<ExternalFileType> unchanged = new ArrayList<>();

        externalFileTypes.clear();
        for (ExternalFileType type : types) {
            externalFileTypes.add(type);

            // See if we can find a type with matching name in the default type list:
            ExternalFileType found = null;
            for (ExternalFileType defType : defTypes) {
                if (defType.getName().equals(type.getName())) {
                    found = defType;
                    break;
                }
            }
            if (found != null) {
                // Found it! Check if it is an exact match, or if it has been customized:
                if (found.equals(type)) {
                    unchanged.add(type);
                } else {
                    // It was modified. Remove its entry from the defaults list, since
                    // the type hasn't been removed:
                    defTypes.remove(found);
                }
            }
        }

        // Go through unchanged types. Remove them from the ones that should be stored,
        // and from the list of defaults, since we don't need to mention these in prefs:
        for (ExternalFileType type : unchanged) {
            defTypes.remove(type);
            types.remove(type);
        }

        // Now set up the array to write to prefs, containing all new types, all modified
        // types, and a flag denoting each default type that has been removed:
        String[][] array = new String[types.size() + defTypes.size()][];
        int i = 0;
        for (ExternalFileType type : types) {
            array[i] = type.getStringArrayRepresentation();
            i++;
        }
        for (ExternalFileType type : defTypes) {
            array[i] = new String[] {type.getName(), FILE_TYPE_REMOVED_FLAG};
            i++;
        }
        Globals.prefs.put("externalFileTypes", FileField.encodeStringArray(array));
    }

    /**
     * Set up the list of external file types, either from default values, or from values recorded in Preferences.
     */
    private void updateExternalFileTypes() {
        // First get a list of the default file types as a starting point:
        List<ExternalFileType> types = getDefaultExternalFileTypes();
        // If no changes have been stored, simply use the defaults:
        if (Globals.prefs.get(JabRefPreferences.EXTERNAL_FILE_TYPES, null) == null) {
            externalFileTypes.clear();
            externalFileTypes.addAll(types);
            return;
        }
        // Read the prefs information for file types:
        String[][] vals = StringUtil
                .decodeStringDoubleArray(Globals.prefs.get(JabRefPreferences.EXTERNAL_FILE_TYPES, ""));
        for (String[] val : vals) {
            if ((val.length == 2) && val[1].equals(FILE_TYPE_REMOVED_FLAG)) {
                // This entry indicates that a default entry type should be removed:
                ExternalFileType toRemove = null;
                for (ExternalFileType type : types) {
                    if (type.getName().equals(val[0])) {
                        toRemove = type;
                        break;
                    }
                }
                // If we found it, remove it from the type list:
                if (toRemove != null) {
                    types.remove(toRemove);
                }
            } else {
                // A new or modified entry type. Construct it from the string array:
                ExternalFileType type = ExternalFileType.buildFromArgs(val);
                // Check if there is a default type with the same name. If so, this is a
                // modification of that type, so remove the default one:
                ExternalFileType toRemove = null;
                for (ExternalFileType defType : types) {
                    if (type.getName().equals(defType.getName())) {
                        toRemove = defType;
                        break;
                    }
                }
                // If we found it, remove it from the type list:
                if (toRemove != null) {
                    types.remove(toRemove);
                }

                // Then add the new one:
                types.add(type);
            }
        }

        // Finally, build the list of types based on the modified defaults list:
        for (ExternalFileType type : types) {
            externalFileTypes.add(type);
        }
    }

}
