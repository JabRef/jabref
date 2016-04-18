/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package net.sf.jabref.gui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class IconTheme {

    public static Font FONT;
    public static Font FONT_16;

    /* Colors */

    // JabRef's default colors
    public static final Color DEFAULT_COLOR = new Color(79, 95, 143); // The purple color of the logo
    public static final Color DEFAULT_DISABLED_COLOR = new Color(200, 200, 200);

    // Christmas edition
    //public static final Color DEFAULT_COLOR = new Color(0x155115);
    //public static final Color DEFAULT_DISABLED_COLOR = new Color(0x990000);


    public static final int DEFAULT_SIZE = 24;
    public static final int SMALL_SIZE = 16;

    private static final Map<String, String> KEY_TO_ICON = readIconThemeFile(
            IconTheme.class.getResource("/images/Icons.properties"), "/images/external/");
    private static final String DEFAULT_ICON_PATH = "/images/external/red.png";


    private static final Log LOGGER = LogFactory.getLog(IconTheme.class);

    static {
        try (InputStream stream = FontBasedIcon.class.getResourceAsStream("/fonts/materialdesignicons-webfont.ttf")) {
            FONT = Font.createFont(Font.TRUETYPE_FONT, stream);
            FONT_16 = FONT.deriveFont(Font.PLAIN, 16f);
        } catch (FontFormatException | IOException e) {
            LOGGER.warn("Error loading font", e);
        }
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
        DOI("\uf59f") /*css: web*/,
        DUPLICATE("\uf191") /*css: content-duplicate */,
        EDIT("\uf3eb") /*css: pencil */,
        NEW("\uf224") /* css: file-outline */,
        SAVE("\uf193") /*css: content-save*/,
        SAVE_ALL("\uf194") /*css: content-save-all*/,
        CLOSE("\uf156") /* css: close */,
        PASTE("\uf192") /*css: content-paste*/,
        CUT("\uf190") /*css: content-cut*/,
        COPY("\uf18f") /*css: content-copy */,
        REDO("\uf44e") /*css: redo*/,
        UNDO("\uf54c") /*css: undo*/,
        MARK_ENTRIES("\uf0c0") /*css: bookmark */,
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

        OPEN_IN_NEW_WINDOW("\uf3cc"), /*css: open-in-new */
        CASE_SENSITIVE("\uf02c"), /* css: mdi-alphabetical */
        REG_EX("\uf451"), /*css: mdi-regex */
        CONSOLE("\uf18d"), /*css: console */
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

        public FontBasedIcon createWithNewColor(Color newColor) {
            return new FontBasedIcon(this.iconCode, newColor, this.size);
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
     * @param prefix A String to prefix to all values read. Can represent e.g. the directory
     *               where icon files are to be found.
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
}
