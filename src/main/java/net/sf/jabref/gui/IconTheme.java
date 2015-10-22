package net.sf.jabref.gui;

import net.sf.jabref.logic.l10n.Localization;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

public class IconTheme {

    public static Font FONT;
    public static Font FONT_16;

    public static final Color DEFAULT_COLOR = new Color(98, 119, 130);
    public static final Color DEFAULT_DISABLED_COLOR = new Color(200,200,200);
    public static final int DEFAULT_SIZE = 24;
    public static final int SMALL_SIZE = 16;

    static {
        try {
            FONT = Font.createFont(Font.TRUETYPE_FONT, FontBasedIcon.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf"));
            FONT_16 = FONT.deriveFont(Font.PLAIN, 16f);
        } catch (FontFormatException | IOException e) {
            // PROBLEM!
            e.printStackTrace();
        }
    }

    public enum JabRefIcon {

        ADD("\uf494") /*css: plus-box*/,
        ADD_NOBOX("\uF493") /*css: plus */,
        ADD_ENTRY("\uF571") /*css: tooltip-outline-plus */,
        EDIT_ENTRY("\uf56e") /*css: tooltip-edit */,
        EDIT_STRINGS("\uf572") /*css: tooltip-text */,
        FOLDER("\uf07b"),
        REMOVE("\uf406"),
        REMOVE_NOBOX("\uF405") /*css: minus */,
        FILE("\uf2cf"),
        PDF_FILE("\uf2dc"),
        DOI("\uF5D1") /*css: web*/,
        DUPLICATE("\uF255") /*css: content-duplicate */,
        EDIT("\uF1AB") /*css: pencil */,
        NEW("\uF2DB") /* css: file-outline */,
        SAVE("\uF257") /*css: content-save*/,
        SAVE_ALL("\uF258") /*css: content-save-all*/,
        CLOSE("\uF5DE") /* css: close */,
        PASTE("\uF256") /*css: content-paste*/,
        CUT("\uF254") /*css: content-cut*/,
        COPY("\uF253") /*css: content-copy */,
        REDO("\uF4B9") /*css: redo*/,
        UNDO("\uF58F") /*css: undo*/,
        MARK_ENTRIES("\uF1A2") /*css: bookmark */,
        UNMARK_ENTRIES("\uF1A5") /*css: bookmark-outline */,
        REFRESH("\uF4BB") /*css: refresh */,
        DELETE("\uF280") /*css: delete */,
        SEARCH("\uF3DE") /*css: magnify */,
        PREFERENCES("\uF4F0") /*css: settings */,
        HELP("\uF37D") /*css: help-circle*/,
        HELP_CONTENTS("\uF1A0") /*css: book-open */,
        UP("\uF214") /*css: chevron-up */,
        DOWN("\uF211") /*css: chevron-down */,
        LEFT("\uF141") /* css; arrow-left-bold */,
        RIGHT("\uF146") /*css: arrow-right-bold */,
        SOURCE("\uF232") /*css: code-braces*/,
        MAKE_KEY("\uF3AC") /*css: key-variant */,
        CLEANUP_ENTRIES("\uF1C2") /*css: broom */,
        PRIORITY("\uF2F0") /*css: flag */,
        PRIORITY_HIGH("\uF2F0", Color.RED) /*css: flag */,
        PRIORITY_MEDIUM("\uF2F0", Color.ORANGE) /*css: flag */,
        PRIORITY_LOW("\uF2F0", new Color(111, 204, 117)) /*css: flag */,
        PRINTED("\uF4A5") /*css: printer */,
        RANKING("\uf521") /*css: star */,
        RANK1("\uf521\uf524\uF524\uF524\uF524"),
        RANK2("\uf521\uf521\uF524\uF524\uF524"),
        RANK3("\uf521\uf521\uF521\uF524\uF524"),
        RANK4("\uf521\uf521\uF521\uF521\uF524"),
        RANK5("\uf521\uf521\uF521\uF521\uF521"),
        WWW("\uF5D1") /*css: web*/,
        GROUP_INCLUDING("\uF2E9") /*css: filter-outline*/,
        GROUP_REFINING("\uF2E8") /*css: filter*/,
        AUTO_GROUP("\uF154"), /*css: auto-fix*/
        EMAIL("\uF2AD") /*css: email*/,
        EXPORT_TO_CLIPBOARD("\uF21C") /*css: clipboard-arrow-left */,
        ATTACH_FILE("\uF469") /*css: paperclip*/,
        AUTO_FILE_LINK("\uF2D6") /*css: file-find */,
        QUALITY_ASSURED("\uF5E7"),
        QUALITY("\uF5E7"),
        OPEN("\uF300") /*css: folder */,
        ADD_ROW("\uf4ed") /* css: server-plus*/,
        REMOVE_ROW("\uF4E9") /*css: server-minus */,
        PICTURE("\uf2d7") /*css: file-image */,
        READ_STATUS_READ("\uF2C4", new Color(111, 204, 117)), /*css: eye */
        READ_STATUS_SKIMMED("\uF2C4", Color.ORANGE), /*css: eye */
        READ_STATUS("\uF2C4"),/*css: eye */
        RELEVANCE("\uF522"),/*css: star-circle */
        MERGE_ENTRIES("\uf24f"), /* css: compare */
        CONNECT_OPEN_OFFICE("\uf454") /*css: open-in-app */,
        PLAIN_TEXT_IMPORT_TODO("\uf202") /* css: checkbox-blank-circle-outline*/,
        PLAIN_TEXT_IMPORT_DONE("\uf206") /* checkbox-marked-circle-outline */,
        DONATE("\uf34d"), /* css: gift */
        MOVE_TAB_ARROW("\uf14d"), /*css:  arrow-up-bold */
        OPTIONAL("\uf3b6"), /*css: label-outline */
        REQUIRED("\uf3b5"), /*css: label */
        INTEGRITY_FAIL("\uF226", Color.RED), /*css: close-circle */
        INTEGRITY_INFO("\uF39E"), /*css: information */
        INTEGRITY_WARN("\uF120"), /*css alert-circle */
        INTEGRITY_SUCCESS("\uf206") /*css: checkbox-marked-circle-outline */,
        GITHUB("\uF350"), /*css: github-circle*/
        TOGGLE_ENTRY_PREVIEW("\uf3c7"), /*css: library-books */
        TOGGLE_GROUPS("\uf4e8"), /*css: server */
        WRITE_XMP("\uf39c"), /* css: import */

        // STILL MISSING:
        GROUP_REGULAR("\uF4E6", Color.RED);

        private final String code;
        private final Color color;

        JabRefIcon(String code) {
            this(code, IconTheme.DEFAULT_COLOR);
        }

        JabRefIcon(String code, Color color) {
            this.code = code;
            this.color = color;
        }

        public FontBasedIcon getIcon() {
            return new FontBasedIcon(this.code, this.color);
        }

        public FontBasedIcon getSmallIcon() {
            return new FontBasedIcon(this.code, this.color, IconTheme.SMALL_SIZE);
        }

        public String getCode() {
            return this.code;
        }

        public JLabel getLabel() {
            JLabel label = new JLabel(this.code);
            label.setForeground(this.color);
            label.setFont(FONT_16);
            return label;
        }
    }

    public static class FontBasedIcon implements Icon {

        private final String iconCode;
        private final Color iconColor;
        private final int size;

        public FontBasedIcon(String code, Color iconColor) {
            this.iconCode = code;
            this.iconColor = iconColor;
            this.size = IconTheme.DEFAULT_SIZE;
        }

        public FontBasedIcon(String code, Color iconColor, int size) {
            this.iconCode = code;
            this.iconColor = iconColor;
            this.size = size;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();

            RenderingHints rh = new RenderingHints(
                    RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setRenderingHints(rh);

            g2.setFont(FONT.deriveFont(Font.PLAIN, size));
            g2.setColor(iconColor);
            FontMetrics fm = g2.getFontMetrics();

            g2.translate(x, y + fm.getAscent());
            g2.drawString(iconCode, 0, 0);

            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }

        public FontBasedIcon createDisabledIcon() {
            return new FontBasedIcon(this.iconCode, IconTheme.DEFAULT_DISABLED_COLOR, this.size);
        }

        public FontBasedIcon createSmallIcon() {
            return new FontBasedIcon(this.iconCode, this.iconColor, IconTheme.SMALL_SIZE);
        }
    }

    private static final Log LOGGER = LogFactory.getLog(IconTheme.class);

    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";

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
     * Looks up the URL for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The URL to the actual image to use.
     */
    public static URL getIconUrl(String name) {
        String key = Objects.requireNonNull(name, "icon name");
        if (!KEY_TO_ICON.containsKey(key)) {
            LOGGER.warn("could not find icon url by name " + name + ", so falling back on default icon " + DEFAULT_ICON_PATH);
        }
        String path = KEY_TO_ICON.getOrDefault(key, DEFAULT_ICON_PATH);
        return Objects.requireNonNull(IconTheme.class.getResource(path), "Path must not be null for key " + key);
    }

    /**
     * Read a typical java property url into a Map. Currently doesn't support escaping
     * of the '=' character - it simply looks for the first '=' to determine where the key ends.
     * Both the key and the value is trimmed for whitespace at the ends.
     *
     * @param url    The URL to read information from.
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory
     *               where icon files are to be found.
     * @return A Map containing all key-value pairs found.
     */
    // FIXME: prefix can be removed?!
    private static Map<String, String> readIconThemeFile(URL url, String prefix) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(prefix, "prefix");

        Map<String, String> result = new HashMap<>();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.contains("=")) {
                    continue;
                }

                int index = line.indexOf("=");
                String key = line.substring(0, index).trim();
                String value = prefix + line.substring(index + 1).trim();
                result.put(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn(Localization.lang("Unable to read default icon theme."), e);
        }
        return result;
    }
}
