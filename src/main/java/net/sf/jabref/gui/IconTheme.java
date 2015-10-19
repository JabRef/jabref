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
        CLIPBOARD("\uf218"),
        FOLDER("\uf07b"),
        REMOVE("\uf406"),
        REMOVE_NOBOX("\uF405") /*css: minus */,
        FILE("\uf2cf"),
        PDF_FILE("\uf2dc"),
        TAGS("\uf485"),
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
        INC_SEARCH("\uF3DE") /*css: magnify */,
        PREFERENCES("\uF4F0") /*css: settings */,
        HELP("\uF37D") /*css: help-circle*/,
        HELP_CONTENTS("\uF1A0") /*css: book-open */,
        UP("\uF214") /*css: chevron-up */,
        DOWN("\uF211") /*css: chevron-down */,
        LEFT("\uF141") /* css; arrow-left-bold */,
        RIGHT("\uF146") /*css: arrow-right-bold */,
        UNKNOWN("\uF37C") /*css: help */,
        SOURCE("\uF232") /*css: code-braces*/,
        MAKE_KEY("\uF3AC") /*css: key-variant */,
        CLEANUP_ENTRIES("\uF1C2") /*css: broom */,
        PRIORITY("\uF2F0") /*css: flag */,
        PRIORITY_HIGH("\uF2F0", Color.RED) /*css: flag */,
        PRIORITY_MEDIUM("\uF2F0", Color.ORANGE) /*css: flag */,
        PRIORITY_LOW("\uF2F0", Color.GREEN) /*css: flag */,
        PRINTED("\uF4A5") /*css: printer */,
        TOGGLE_PRINTED("\uF4A5") /*css: printer */,
        RANKING("\uf521") /*css: numeric*/,
        //RANK1("\uF430") /*css: numeric-1-box */,
        RANK1("\uf521\uf524\uF524\uF524\uF524"),
        RANK2("\uf521\uf521\uF524\uF524\uF524"),
        RANK3("\uf521\uf521\uF521\uF524\uF524"),
        RANK4("\uf521\uf521\uF521\uF521\uF524"),
        RANK5("\uf521\uf521\uF521\uF521\uF521"),
        WWW("\uF5D1") /*css: web*/,
        GROUP_INCLUDING("\uF2E8") /*css: filter; should be rotated*/,
        GROUP_REFINING("\uF2E8") /*css: filter*/,
        EMAIL("\uF2AD") /*css: email*/,
        DOWNLOAD("\uF299") /*css: download */,
        EXPORT_TO_CLIPBOARD("\uF21C") /*css: clipboard-arrow-left */,
        ATTACH_FILE("\uF469") /*css: paperclip*/,
        AUTO_FILE_LINK("\uF2D6") /*css: file-find */,
        TOGGLE_QUALITY_ASSURED("\uF5E7"),
        QUALITY_ASSURED("\uF5E7"),
        QUALITY("\uF5E7"),
        OPEN("\uF300") /*css: folder */,
        ADD_ROW("\uf4ed") /* css: server-plus*/,
        REMOVE_ROW("\uF4E9") /*css: server-minus */,
        PICTURE("\uf2d7") /*css: file-image */,
        READ_STATUS_READ("\uF2C4", Color.GREEN), /*css: eye */
        READ_STATUS_SKIMMED("\uF2C4", Color.ORANGE), /*css: eye */
        READ_STATUS("\uF2C4"),/*css: eye */
        RELEVANT("\uF522"), /*css: star-circle */
        TOGGLE_RELEVANCE("\uF522"),/*css: star-circle */
        SET_RELEVANT("\uF522"), /*css: star-circle */
        MERGE_ENTRIES("\uf24f"), /* css: compare */
        CONNECT_OPEN_OFFICE("\uf454") /*css: open-in-app */,
        PLAIN_TEXT_IMPORT_TODO("\uf202") /* css: checkbox-blank-circle-outline*/,
        PLAIN_TEXT_IMPORT_DONE("\uf206") /* checkbox-marked-circle-outline */,
        DONATE("\uf34d"), /* css: gift */

        // simple color boxes:
        BOX_RED("\uF200", Color.RED),
        BOX_ORANGE("\uF200", Color.ORANGE),
        BOX_GREEN("\uF200", Color.GREEN),

        // STILL MISSING:
        COMPLETE("\uF4E6", Color.RED),
        DRAG_AND_DROP_ARROW("\uF4E6", Color.RED),
        EDIT_PREAMBLE("\uF4E6", Color.RED),
        EXPORT_TO_KEYWORDS("\uF4E6", Color.RED),
        //SAVE_AS("\uF4E6", Color.RED),
        WRONG("\uF4E6", Color.RED),
        GROUPS_HIGHLIGHT_ALL("\uF4E6", Color.RED),
        GROUPS_HIGHLIGHT_ANY("\uF4E6", Color.RED),
        GENERAL("\uF4E6", Color.RED),
        GREEN("\uF4E6", Color.RED),
        IMPORT_FROM_KEYWORDS("\uF4E6", Color.RED),
        INTEGRITY_CHECK("\uF4E6", Color.RED),
        INTEGRITY_FAIL("\uF4E6", Color.RED),
        INTEGRITY_INFO("\uF4E6", Color.RED),
        INTEGRITY_WARN("\uF4E6", Color.RED),
        LOAD_SESSION("\uF4E6", Color.RED),

        OPTIONAL("\uF4E6", Color.RED),
        ORANGE("\uF4E6", Color.RED),
        AUTO_GROUP("\uF4E6", Color.RED),
        PLUGIN("\uF4E6", Color.RED),
        RED("\uF4E6", Color.RED),
        REQUIRED("\uF4E6", Color.RED),
        SECONDARY_SORTED_REVERSE("\uF4E6", Color.RED),
        TOGGLE_GROUPS("\uF4E6", Color.RED),
        TOGGLE_ENTRY_PREVIEW("\uF4E6", Color.RED),
        OPEN_FOLDER("\uF4E6", Color.RED),
        GROUP_REGULAR("\uF4E6", Color.RED),
        WRITE_XMP("\uF4E6", Color.RED);

        private final String code;
        private final Color color;

        JabRefIcon(String code) {
            this(code, new Color(113,134,145));
        }

        JabRefIcon(String code, Color color) {
            this.code = code;
            this.color = color;
        }

        public FontBasedIcon getIcon() {
            return new FontBasedIcon(this.code, this.color);
        }

        public FontBasedIcon getSmallIcon() {
            return new FontBasedIcon(this.code, this.color, 16);
        }

        public ImageIcon getImageIcon() {
            return new ImageIcon() {

                private FontBasedIcon icon = getIcon();

                @Override
                public synchronized void paintIcon(Component c, Graphics g, int x, int y) {
                    icon.paintIcon(c, g, x, y);
                }

                @Override
                public int getIconWidth() {
                    return icon.getIconWidth();
                }

                @Override
                public int getIconHeight() {
                    return icon.getIconHeight();
                }

                @Override
                public Image getImage() {
                    int w = icon.getIconWidth();
                    int h = icon.getIconHeight();
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice gd = ge.getDefaultScreenDevice();
                    GraphicsConfiguration gc = gd.getDefaultConfiguration();
                    BufferedImage image = gc.createCompatibleImage(w, h);
                    Graphics2D g = image.createGraphics();
                    icon.paintIcon(null, g, 0, 0);
                    g.dispose();
                    return image;
                }
            };
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
            this.size = 24;
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
        //return JabRefIcon.values()[new Random().nextInt(JabRefIcon.values().length)].getImageIcon();
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
