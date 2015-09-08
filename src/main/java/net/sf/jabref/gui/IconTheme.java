package net.sf.jabref.gui;

import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class IconTheme {

    private static final Log LOGGER = LogFactory.getLog(IconTheme.class);

    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(IconTheme.class.getResource("/images/crystal_16/Icons.properties"), "/images/crystal_16/");
    private static final URL DEFAULT_ICON_URL = IconTheme.class.getResource("/images/crystal_16/red.png");


    /**
     * Looks up the URL for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The URL to the actual image to use.
     */
    public static URL getIconUrl(String name) {
        if (KEY_TO_ICON.containsKey(name)) {
            String path = KEY_TO_ICON.get(name);
            URL url = IconTheme.class.getResource(path);
            if (url == null) {
                LOGGER.warn(Localization.lang("Could not find image file") + " '" + path + '\'');
                return DEFAULT_ICON_URL;
            } else {
                return url;
            }
        } else {
            return DEFAULT_ICON_URL;
        }
    }

    /**
     * Constructs an ImageIcon for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The ImageIcon for the function.
     */
    public static ImageIcon getImage(String name) {
        return new ImageIcon(getIconUrl(name));
    }

    /**
     * Get a Map of all application icons mapped from their keys.
     *
     * @return A Map containing all icons used in the application.
     */
    public static Map<String, String> getAllIcons() {
        return Collections.unmodifiableMap(KEY_TO_ICON);
    }

    /**
     * Read a typical java property file into a Map. Currently doesn't support escaping
     * of the '=' character - it simply looks for the first '=' to determine where the key ends.
     * Both the key and the value is trimmed for whitespace at the ends.
     *
     * @param file   The URL to read information from.
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory
     *               where icon files are to be found.
     * @return A Map containing all key-value pairs found.
     */
    private static Map<String, String> readIconThemeFile(URL file, String prefix) {
        Objects.requireNonNull(file, "passed URL to file must be not null");

        Map<String, String> result = new HashMap<>();

        try (InputStream in = file.openStream()) {
            StringBuilder buffer = new StringBuilder();
            int c;
            while ((c = in.read()) != -1) {
                buffer.append((char) c);
            }
            String[] lines = buffer.toString().split("\n");
            for (String line1 : lines) {
                String line = line1.trim();
                int index = line.indexOf("=");
                if (index >= 0) {
                    String key = line.substring(0, index).trim();
                    String value = prefix + line.substring(index + 1).trim();
                    result.put(key, value);
                }
            }
        } catch (IOException e) {
            LOGGER.warn(Localization.lang("Unable to read default icon theme."), e);
        }
        return result;
    }
}
