package org.jabref.gui.externalfiletype;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.bibtex.FileFieldWriter;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.FilePreferences;

// Do not make this class final, as it otherwise can't be mocked for tests
public class ExternalFileTypes {

    // This String is used in the encoded list in prefs of external file type
    // modifications, in order to indicate a removed default file type:
    private static final String FILE_TYPE_REMOVED_FLAG = "REMOVED";
    private static final ExternalFileType HTML_FALLBACK_TYPE = StandardExternalFileType.URL;

    private ExternalFileTypes() {
    }

    public static List<ExternalFileType> getDefaultExternalFileTypes() {
        return Arrays.asList(StandardExternalFileType.values());
    }

    /**
     * Look up the external file type registered with this name, if any.
     *
     * @param name The file type name.
     * @return The ExternalFileType registered, or null if none.
     */
    public static Optional<ExternalFileType> getExternalFileTypeByName(String name, FilePreferences filePreferences) {
        Optional<ExternalFileType> externalFileType = filePreferences.getExternalFileTypes().stream().filter(type -> type.getName().equals(name)).findFirst();
        if (externalFileType.isPresent()) {
            return externalFileType;
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
    public static Optional<ExternalFileType> getExternalFileTypeByExt(String extension, FilePreferences filePreferences) {
        String extensionCleaned = extension.replace(".", "").replace("*", "");
        return filePreferences.getExternalFileTypes().stream().filter(type -> type.getExtension().equalsIgnoreCase(extensionCleaned)).findFirst();
    }

    /**
     * Returns true if there is an external file type registered for this extension.
     *
     * @param extension The file extension.
     * @return true if an ExternalFileType with the extension exists, false otherwise
     */
    public static boolean isExternalFileTypeByExt(String extension, FilePreferences filePreferences) {
        return filePreferences.getExternalFileTypes().stream().anyMatch(type -> type.getExtension().equalsIgnoreCase(extension));
    }

    /**
     * Look up the external file type registered for this filename, if any.
     *
     * @param filename The name of the file whose type to look up.
     * @return The ExternalFileType registered, or null if none.
     */
    public static Optional<ExternalFileType> getExternalFileTypeForName(String filename, FilePreferences filePreferences) {
        int longestFound = -1;
        ExternalFileType foundType = null;
        for (ExternalFileType type : filePreferences.getExternalFileTypes()) {
            if (!type.getExtension().isEmpty() && filename.toLowerCase(Locale.ROOT).endsWith(type.getExtension().toLowerCase(Locale.ROOT))
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
    public static Optional<ExternalFileType> getExternalFileTypeByMimeType(String mimeType, FilePreferences filePreferences) {
        // Ignores parameters according to link: (https://developer.mozilla.org/en-US/docs/Web/HTTP/Basics_of_HTTP/MIME_types)
        if (mimeType.indexOf(';') != -1) {
            mimeType = mimeType.substring(0, mimeType.indexOf(';')).trim();
        }
        for (ExternalFileType type : filePreferences.getExternalFileTypes()) {
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

    public static Optional<ExternalFileType> getExternalFileTypeByFile(Path file, FilePreferences filePreferences) {
        final String filePath = file.toString();
        final Optional<String> extension = FileUtil.getFileExtension(filePath);
        return extension.flatMap(ext -> getExternalFileTypeByExt(ext, filePreferences));
    }

    public static Optional<ExternalFileType> getExternalFileTypeByLinkedFile(LinkedFile linkedFile, boolean deduceUnknownType, FilePreferences filePreferences) {
        Optional<ExternalFileType> type = getExternalFileTypeByName(linkedFile.getFileType(), filePreferences);
        boolean isUnknownType = type.isEmpty() || (type.get() instanceof UnknownExternalFileType);

        if (isUnknownType && deduceUnknownType) {
            // No file type was recognized. Try to find a usable file type based on mime type:
            Optional<ExternalFileType> mimeType = getExternalFileTypeByMimeType(linkedFile.getFileType(), filePreferences);
            if (mimeType.isPresent()) {
                return mimeType;
            }

            // No type could be found from mime type. Try based on the extension:
            return FileUtil.getFileExtension(linkedFile.getLink())
                             .flatMap(extension -> getExternalFileTypeByExt(extension, filePreferences));
        } else {
            return type;
        }
    }

    /**
     * @return A StringList of customized and removed file types compared to the default list of external file types for storing
     */
    public static String toStringList(Collection<ExternalFileType> fileTypes) {
        // First find a list of the default types:
        List<ExternalFileType> defTypes = new ArrayList<>(getDefaultExternalFileTypes());
        // Make a list of types that are unchanged:
        List<ExternalFileType> unchanged = new ArrayList<>();
        // Create a result list
        List<ExternalFileType> results = new ArrayList<>();

        for (ExternalFileType type : fileTypes) {
            results.add(type);
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
            results.remove(type);
        }

        // Now set up the array to write to prefs, containing all new types, all modified
        // types, and a flag denoting each default type that has been removed:
        String[][] array = new String[results.size() + defTypes.size()][];
        int i = 0;
        for (ExternalFileType type : results) {
            array[i] = type.toStringArray();
            i++;
        }
        for (ExternalFileType type : defTypes) {
            array[i] = new String[] {type.getName(), FILE_TYPE_REMOVED_FLAG};
            i++;
        }
        return FileFieldWriter.encodeStringArray(array);
    }

    /**
     * Set up the list of external file types, either from default values, or from values recorded in PreferencesService.
     */
    public static Set<ExternalFileType> fromString(String storedFileTypes) {
        // First get a list of the default file types as a starting point:
        Set<ExternalFileType> types = new HashSet<>(getDefaultExternalFileTypes());

        // If no changes have been stored, simply use the defaults:
        if (StringUtil.isBlank(storedFileTypes)) {
            return types;
        }

        // Read the prefs information for file types:
        String[][] vals = StringUtil.decodeStringDoubleArray(storedFileTypes);
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
                ExternalFileType type = CustomExternalFileType.buildFromArgs(val);
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

        return types;
    }
}
