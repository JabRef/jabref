package org.jabref.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.text.Text;

import org.jabref.preferences.JabRefPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class IconTheme {

    /**
     * JabRef's default color
     */
    public static final Color DEFAULT_COLOR = JabRefPreferences.getInstance().getColor(JabRefPreferences.ICON_ENABLED_COLOR);
    public static final Color DEFAULT_DISABLED_COLOR = JabRefPreferences.getInstance().getColor(JabRefPreferences.ICON_DISABLED_COLOR);
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";
    private static final Log LOGGER = LogFactory.getLog(IconTheme.class);
    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(
            IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");
    public static Font FONT;

    // Christmas edition
    //public static final Color DEFAULT_COLOR = new Color(0x155115);
    //public static final Color DEFAULT_DISABLED_COLOR = new Color(0x990000);
    private static Font FONT_16;
    private static javafx.scene.text.Font FX_FONT;

    static {
        try (InputStream stream = FontBasedIcon.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf")) {
            FONT = Font.createFont(Font.TRUETYPE_FONT, stream);
            FONT_16 = FONT.deriveFont(Font.PLAIN, 16f);
            try (InputStream stream2 = FontBasedIcon.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf")) {
                FX_FONT = javafx.scene.text.Font.loadFont(stream2, JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_LARGE));
            }
        } catch (FontFormatException | IOException e) {
            LOGGER.warn("Error loading font", e);
        }
    }

    public static javafx.scene.paint.Color getDefaultColor() {
        return javafx.scene.paint.Color.rgb(DEFAULT_COLOR.getRed(), DEFAULT_COLOR.getGreen(), DEFAULT_COLOR.getBlue(), DEFAULT_COLOR.getAlpha() / 255.0);
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

    public static Image getJabRefImageFX() {
        return getImageFX("jabrefIcon48");
    }

    /**
     * Constructs an {@link Image} for the image representing the given function, in the resource
     * file listing images.
     *
     * @param name The name of the icon, such as "open", "save", "saveAs" etc.
     * @return The {@link Image} for the function.
     */
    public static Image getImageFX(String name) {
        return new Image(getIconUrl(name).toString());
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
            LOGGER.warn("Could not find icon url by name " + name + ", so falling back on default icon "
                    + DEFAULT_ICON_PATH);
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
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory where icon files are to be
     *               found.
     * @return A Map containing all key-value pairs found.
     */
    // FIXME: prefix can be removed?!
    private static Map<String, String> readIconThemeFile(URL url, String prefix) {
        Objects.requireNonNull(url, "url");
        Objects.requireNonNull(prefix, "prefix");

        Map<String, String> result = new HashMap<>();

        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(url.openStream(), StandardCharsets.ISO_8859_1))) {
            String line;
            while ((line = in.readLine()) != null) {
                if (!line.contains("=")) {
                    continue;
                }

                int index = line.indexOf('=');
                String key = line.substring(0, index).trim();
                String value = prefix + line.substring(index + 1).trim();
                result.put(key, value);
            }
        } catch (IOException e) {
            LOGGER.warn("Unable to read default icon theme.", e);
        }
        return result;
    }


    public enum JabRefIcon {

        ADD("\uf416") /*css: mdi-plus-box*/,
        ADD_NOBOX("\uf415") /*css: plus */,
        ADD_ENTRY("\uf527") /*css: tooltip-outline-plus */,
        EDIT_ENTRY("\uf524") /*css: tooltip-edit */,
        EDIT_STRINGS("\uf528") /*css: tooltip-text */,
        FOLDER("\uf24b"), /*css: folder */
        REMOVE("\uf375"), /*css: minus-box */
        REMOVE_NOBOX("\uf374") /*css: minus */,
        FILE("\uf214"), /* css: file*/
        PDF_FILE("\uf225"), /* css: file-pdf*/
        DOI("\uF072") /*css: barcode-scan*/,
        DUPLICATE("\uf191") /*css: content-duplicate */,
        EDIT("\uf3eb") /*css: pencil */,
        NEW("\uf224") /* css: file-outline */,
        SAVE("\uf193") /*css: content-save*/,
        SAVE_ALL("\uf194") /*css: content-save-all*/,
        CLOSE("\uf156") /* css: close */,
        PASTE("\uf192") /*css: content-paste*/,
        CUT("\uf190") /*css: content-cut*/,
        COPY("\uf18f") /*css: content-copy */,
        COMMENT("\uF188") /*css: comment*/,
        REDO("\uf44e") /*css: redo*/,
        UNDO("\uf54c") /*css: undo*/,
        MARK_ENTRIES("\uf0c0") /*css: bookmark */,
        MARKER("\uF524") /*css: marker */,
        UNMARK_ENTRIES("\uf0c3") /*css: bookmark-outline */,
        REFRESH("\uf450") /*css: refresh */,
        DELETE_ENTRY("\uf1c0") /*css: delete */,
        SEARCH("\uf349") /*css: magnify */,
        PREFERENCES("\uf493") /*css: settings */,
        HELP("\uf2d7") /*css: help-circle*/,
        UP("\uf143") /*css: chevron-up */,
        DOWN("\uf140") /*css: chevron-down */,
        LEFT("\uf04e") /* css; arrow-left-bold */,
        RIGHT("\uf055") /*css: arrow-right-bold */,
        SOURCE("\uf169") /*css: code-braces*/,
        MAKE_KEY("\uf30b") /*css: key-variant */,
        CLEANUP_ENTRIES("\uf0e2") /*css: broom */,
        PRIORITY("\uf23b") /*css: flag */,
        PRIORITY_HIGH("\uF23B", Color.RED) /*css: flag */,
        PRIORITY_MEDIUM("\uF23B", Color.ORANGE) /*css: flag */,
        PRIORITY_LOW("\uF23B", new Color(111, 204, 117)) /*css: flag */,
        PRINTED("\uf42a") /*css: printer */,
        RANKING("\uf4ce") /*css: star + star-outline: f4d2*/,
        RANK1("\uF4CE\uF4D2\uF4D2\uF4D2\uf4d2"),
        RANK2("\uF4CE\uF4CE\uF4D2\uF4D2\uF4D2"),
        RANK3("\uF4CE\uF4CE\uF4CE\uF4D2\uF4D2"),
        RANK4("\uF4CE\uF4CE\uF4CE\uF4CE\uF4D2"),
        RANK5("\uF4CE\uF4CE\uF4CE\uF4CE\uF4CE"),
        WWW("\uf59f") /*css: web*/,
        GROUP_INCLUDING("\uf233") /*css: filter-outline*/,
        GROUP_REFINING("\uf232") /*css: filter*/,
        AUTO_GROUP("\uf068"), /*css: auto-fix*/
        EMAIL("\uf1ee") /*css: email*/,
        EXPORT_TO_CLIPBOARD("\uf14b") /*css: clipboard-arrow-left */,
        ATTACH_FILE("\uf3e2") /*css: paperclip*/,
        AUTO_FILE_LINK("\uf21e") /*css: file-find */,
        QUALITY_ASSURED("\uf124"), /*css: certificate */
        QUALITY("\uF124"),/*css: certificate */
        OPEN("\uf24b") /*css: folder */,
        ADD_ROW("\uf490") /* css: server-plus*/,
        REMOVE_ROW("\uf48c") /*css: server-minus */,
        PICTURE("\uf21f") /*css: file-image */,
        READ_STATUS_READ("\uf208", new Color(111, 204, 117)), /*css: eye */
        READ_STATUS_SKIMMED("\uF208", Color.ORANGE), /*css: eye */
        READ_STATUS("\uF208"),/*css: eye */
        RELEVANCE("\uf4cf"),/*css: star-circle */
        MERGE_ENTRIES("\uf18a"), /* css: compare */
        CONNECT_OPEN_OFFICE("\uf3cb") /*css: open-in-app */,
        PLAIN_TEXT_IMPORT_TODO("\uf130") /* css: checkbox-blank-circle-outline*/,
        PLAIN_TEXT_IMPORT_DONE("\uf134") /* checkbox-marked-circle-outline */,
        DONATE("\uf2a1"), /* css: gift */
        MOVE_TAB_ARROW("\uf05e"), /*css:  arrow-up-bold */
        OPTIONAL("\uf316"), /*css: label-outline */
        REQUIRED("\uf315"), /*css: label */
        INTEGRITY_FAIL("\uf159", Color.RED), /*css: close-circle */
        INTEGRITY_INFO("\uf2fc"), /*css: information */
        INTEGRITY_WARN("\uf028"), /*css alert-circle */
        INTEGRITY_SUCCESS("\uF134") /*css: checkbox-marked-circle-outline */,
        GITHUB("\uf2a4"), /*css: github-circle*/
        TOGGLE_ENTRY_PREVIEW("\uf332"), /*css: library-books */
        TOGGLE_GROUPS("\uf572"), /*css: view-list */
        WRITE_XMP("\uf2fa"), /* css: import */
        FILE_WORD("\uf22c"), /*css: file-word */
        FILE_EXCEL("\uf21b"), /*css: file-excel */
        FILE_POWERPOINT("\uf227"), /*css: file-powerpoint */
        FILE_TEXT("\uf219"), /*css: file-document */
        FILE_MULTIPLE("\uf222"), /*css: file-multiple */
        KEY_BINDINGS("\uf30c"), /*css: keyboard */
        FIND_DUPLICATES("\uf16b"), /*css: code-equal */
        PULL("\uf4c2"), /*source-pull*/
        OPEN_IN_NEW_WINDOW("\uf3cc"), /*css: open-in-new */
        CASE_SENSITIVE("\uf02c"), /* css: mdi-alphabetical */
        REG_EX("\uf451"), /*css: mdi-regex */
        CONSOLE("\uf18d"), /*css: console */
        FORUM("\uf28c"), /* css: forum */
        FACEBOOK("\uf20c"), /* css: facebook */
        BLOG("\uf46b"), /* css: rss */
        GLOBAL_SEARCH("\uF1E7"), /* css: earth */
        DATE_PICKER("\uF0ED;"), /* css: calendar */
        DEFAULT_GROUP_ICON("\uF316"), /* css: label-outline */
        ALL_ENTRIES_GROUP_ICON("\uF1B8"), /* css: database */
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
            return new FontBasedIcon(this.code, this.color, JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL));
        }

        public Node getGraphicNode() {
            Text graphic = new Text(this.code);
            graphic.getStyleClass().add("icon");
            return graphic;
        }

        public String getCode() {
            return this.code;
        }
    }

    public static class FontBasedIcon implements Icon {

        private final String iconCode;
        private final Color iconColor;
        private final int size;

        public FontBasedIcon(String code, Color iconColor) {
            this.iconCode = code;
            this.iconColor = iconColor;
            this.size = JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_LARGE);
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
            return new FontBasedIcon(this.iconCode, this.iconColor, JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL));
        }

        public FontBasedIcon createWithNewColor(Color newColor) {
            return new FontBasedIcon(this.iconCode, newColor, this.size);
        }
    }

    public static List<java.awt.Image> getLogoSet() {
        List<java.awt.Image> jabrefLogos = new ArrayList<>();
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon16")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon20")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon32")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon40")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon48")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon64")).getImage());
        jabrefLogos.add(new ImageIcon(getIconUrl("jabrefIcon128")).getImage());

        return jabrefLogos;
    }
}
