package org.jabref.model.openoffice.ootext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.UnoCast;
import org.jabref.model.openoffice.uno.UnoCrossRef;
import org.jabref.model.openoffice.util.OOPair;
import org.jabref.model.strings.StringUtil;

import com.sun.star.awt.FontSlant;
import com.sun.star.awt.FontStrikeout;
import com.sun.star.awt.FontUnderline;
import com.sun.star.awt.FontWeight;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.beans.XMultiPropertyStates;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.beans.XPropertyState;
import com.sun.star.lang.Locale;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.CaseMap;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interpret OOText into an OpenOffice or LibreOffice writer document.
 */
@AllowedToUseAwt("Requires AWT for changing document properties")
public class OOTextIntoOO {

    private static final Logger LOGGER = LoggerFactory.getLogger(OOTextIntoOO.class);

    /**
     *  "ParaStyleName" is an OpenOffice Property name.
     */
    private static final String PARA_STYLE_NAME = "ParaStyleName";

    /*
     * Character property names used in multiple locations below.
     */
    private static final String CHAR_ESCAPEMENT_HEIGHT = "CharEscapementHeight";
    private static final String CHAR_ESCAPEMENT = "CharEscapement";
    private static final String CHAR_STYLE_NAME = "CharStyleName";
    private static final String CHAR_UNDERLINE = "CharUnderline";
    private static final String CHAR_STRIKEOUT = "CharStrikeout";

    /*
     *  SUPERSCRIPT_VALUE and SUPERSCRIPT_HEIGHT are percents of the normal character height
     */
    private static final short CHAR_ESCAPEMENT_VALUE_DEFAULT = (short) 0;
    private static final short SUPERSCRIPT_VALUE = (short) 33;
    private static final short SUBSCRIPT_VALUE = (short) -10;
    private static final byte CHAR_ESCAPEMENT_HEIGHT_DEFAULT = (byte) 100;
    private static final byte SUPERSCRIPT_HEIGHT = (byte) 58;
    private static final byte SUBSCRIPT_HEIGHT = (byte) 58;

    private static final String TAG_NAME_REGEXP =
        "(?:b|i|em|tt|smallcaps|sup|sub|u|s|p|span|oo:referenceToPageNumberOfReferenceMark)";

    private static final String ATTRIBUTE_NAME_REGEXP =
        "(?:oo:ParaStyleName|oo:CharStyleName|lang|style|target)";

    private static final String ATTRIBUTE_VALUE_REGEXP = "\"([^\"]*)\"";

    private static final Pattern HTML_TAG =
        Pattern.compile("<(/" + TAG_NAME_REGEXP + ")>"
                        + "|"
                        + "<(" + TAG_NAME_REGEXP + ")"
                        + "((?:\\s+(" + ATTRIBUTE_NAME_REGEXP + ")=" + ATTRIBUTE_VALUE_REGEXP + ")*)"
                        + ">");

    private static final Pattern ATTRIBUTE_PATTERN =
        Pattern.compile("\\s+(" + ATTRIBUTE_NAME_REGEXP + ")=" + ATTRIBUTE_VALUE_REGEXP);

    private OOTextIntoOO() {
        // Hide the public constructor
    }

    /**
     * Insert a text with formatting indicated by HTML-like tags, into
     * a text at the position given by a cursor.
     *
     * Limitation: understands no entities. It does not receive any either, unless
     * the user provides it.
     *
     * To limit the damage {@code TAG_NAME_REGEXP} and {@code ATTRIBUTE_NAME_REGEXP}
     * explicitly lists the names we care about.
     *
     * Notable changes w.r.t insertOOFormattedTextAtCurrentLocation:
     *
     * - new tags:
     *
     *   - {@code <span lang="zxx">}
     *     - earlier was applied from code
     *
     *   - {@code <span oo:CharStyleName="CharStylename">}
     *     - earlier was applied from code, for "CitationCharacterFormat"
     *
     *   - {@code <p>} start new paragraph
     *     - earlier was applied from code
     *
     *   - {@code <p oo:ParaStyleName="ParStyleName">} : start new paragraph and apply ParStyleName
     *     - earlier was applied from code
     *
     *   - {@code <tt>}
     *     - earlier: known, but ignored
     *     - now: equivalent to {@code <span oo:CharStyleName="Example">}
     *   - {@code <oo:referenceToPageNumberOfReferenceMark>} (self-closing)
     *
     * - closing tags try to properly restore state (in particular, the "not directly set" state)
     *   instead of dictating an "off" state. This makes a difference when the value inherited from
     *   another level (for example the paragraph) is not the "off" state.
     *
     *   An example: a style with
     *   {@code ReferenceParagraphFormat="JR_bibentry"}
     *   Assume JR_bibentry in LibreOffice is a paragraph style that prescribes "bold" font.
     *   LAYOUT only prescribes bold around year.
     *   Which parts of the bibliography entries should come out as bold?
     *
     * - The user can format citation marks (it is enough to format their start) and the
     *   properties not (everywhere) dictated by the style are preserved (where they are not).
     *
     * @param position   The cursor giving the insert location. Not modified.
     * @param ootext     The marked-up text to insert.
     */
    public static void write(XTextDocument doc, XTextCursor position, OOText ootext)
        throws
        WrappedTargetException,
        CreationException {

        Objects.requireNonNull(doc);
        Objects.requireNonNull(ootext);
        Objects.requireNonNull(position);

        String lText = OOText.toString(ootext);

        LOGGER.debug(lText);

        XText text = position.getText();
        XTextCursor cursor = text.createTextCursorByRange(position);
        cursor.collapseToEnd();

        MyPropertyStack formatStack = new MyPropertyStack(cursor);
        Stack<String> expectEnd = new Stack<>();

        // We need to extract formatting. Use a simple regexp search iteration:
        int piv = 0;
        Matcher tagMatcher = HTML_TAG.matcher(lText);
        while (tagMatcher.find()) {

            String currentSubstring = lText.substring(piv, tagMatcher.start());
            if (!currentSubstring.isEmpty()) {
                cursor.setString(currentSubstring);
            }
            formatStack.apply(cursor);
            cursor.collapseToEnd();

            String endTagName = tagMatcher.group(1);
            String startTagName = tagMatcher.group(2);
            String attributeListPart = tagMatcher.group(3);
            boolean isStartTag = StringUtil.isNullOrEmpty(endTagName);
            String tagName = isStartTag ? startTagName : endTagName;
            Objects.requireNonNull(tagName);

            // Attibutes parsed into (name,value) pairs.
            List<OOPair<String, String>> attributes = parseAttributes(attributeListPart);

            // Handle tags:
            switch (tagName) {
                case "b":
                    formatStack.pushLayer(setCharWeight(FontWeight.BOLD));
                    expectEnd.push("/" + tagName);
                    break;
                case "i":
                case "em":
                    formatStack.pushLayer(setCharPosture(FontSlant.ITALIC));
                    expectEnd.push("/" + tagName);
                    break;
                case "smallcaps":
                    formatStack.pushLayer(setCharCaseMap(CaseMap.SMALLCAPS));
                    expectEnd.push("/" + tagName);
                    break;
                case "sup":
                    formatStack.pushLayer(setSuperScript(formatStack));
                    expectEnd.push("/" + tagName);
                    break;
                case "sub":
                    formatStack.pushLayer(setSubScript(formatStack));
                    expectEnd.push("/" + tagName);
                    break;
                case "u":
                    formatStack.pushLayer(setCharUnderline(FontUnderline.SINGLE));
                    expectEnd.push("/" + tagName);
                    break;
                case "s":
                    formatStack.pushLayer(setCharStrikeout(FontStrikeout.SINGLE));
                    expectEnd.push("/" + tagName);
                    break;
                case "/p":
                    // nop
                    break;
                case "p":
                    insertParagraphBreak(text, cursor);
                    cursor.collapseToEnd();
                    for (OOPair<String, String> pair : attributes) {
                        String key = pair.a;
                        String value = pair.b;
                        switch (key) {
                            case "oo:ParaStyleName":
                                // <p oo:ParaStyleName="Standard">
                                if (StringUtil.isNullOrEmpty(value)) {
                                    LOGGER.debug(String.format("oo:ParaStyleName inherited"));
                                } else {
                                    if (setParagraphStyle(cursor, value)) {
                                        // Presumably tested already:
                                        LOGGER.debug(String.format("oo:ParaStyleName=\"%s\" failed", value));
                                    }
                                }
                                break;
                            default:
                                LOGGER.warn(String.format("Unexpected attribute '%s' for <%s>", key, tagName));
                                break;
                        }
                    }
                    break;
                case "oo:referenceToPageNumberOfReferenceMark":
                    for (OOPair<String, String> pair : attributes) {
                        String key = pair.a;
                        String value = pair.b;
                        switch (key) {
                            case "target":
                                UnoCrossRef.insertReferenceToPageNumberOfReferenceMark(doc, value, cursor);
                                break;
                            default:
                                LOGGER.warn(String.format("Unexpected attribute '%s' for <%s>", key, tagName));
                                break;
                        }
                    }
                    break;
                case "tt":
                    // Note: "Example" names a character style in LibreOffice.
                    formatStack.pushLayer(setCharStyleName("Example"));
                    expectEnd.push("/" + tagName);
                    break;
                case "span":
                    List<OOPair<String, Object>> settings = new ArrayList<>();
                    for (OOPair<String, String> pair : attributes) {
                        String key = pair.a;
                        String value = pair.b;
                        switch (key) {
                            case "oo:CharStyleName":
                                // <span oo:CharStyleName="Standard">
                                settings.addAll(setCharStyleName(value));
                                break;
                            case "lang":
                                // <span lang="zxx">
                                // <span lang="en-US">
                                settings.addAll(setCharLocale(value));
                                break;
                            case "style":
                                // HTML-style small-caps
                                if ("font-variant: small-caps".equals(value)) {
                                    settings.addAll(setCharCaseMap(CaseMap.SMALLCAPS));
                                    break;
                                }
                                LOGGER.warn(String.format("Unexpected value %s for attribute '%s' for <%s>",
                                                          value, key, tagName));
                                break;
                            default:
                                LOGGER.warn(String.format("Unexpected attribute '%s' for <%s>", key, tagName));
                                break;
                        }
                    }
                    formatStack.pushLayer(settings);
                    expectEnd.push("/" + tagName);
                    break;
                case "/b":
                case "/i":
                case "/em":
                case "/tt":
                case "/smallcaps":
                case "/sup":
                case "/sub":
                case "/u":
                case "/s":
                case "/span":
                    formatStack.popLayer();
                    String expected = expectEnd.pop();
                    if (!tagName.equals(expected)) {
                        LOGGER.warn(String.format("expected '<%s>', found '<%s>' after '%s'",
                                                  expected,
                                                  tagName,
                                                  currentSubstring));
                    }
                    break;
                default:
                    LOGGER.warn(String.format("ignoring unknown tag '<%s>'", tagName));
                    break;
            }

            piv = tagMatcher.end();
        }

        if (piv < lText.length()) {
            cursor.setString(lText.substring(piv));
        }
        formatStack.apply(cursor);
        cursor.collapseToEnd();

        if (!expectEnd.empty()) {
            String rest = "";
            for (String s : expectEnd) {
                rest = String.format("<%s>", s) + rest;
            }
            LOGGER.warn(String.format("OOTextIntoOO.write:"
                                      + " expectEnd stack is not empty at the end: %s%n",
                                      rest));
        }
    }

    /**
     * Purpose: in some cases we do not want to inherit direct
     *          formatting from the context.
     *
     *          In particular, when filling the bibliography title and body.
     */
    public static void removeDirectFormatting(XTextCursor cursor) {

        XMultiPropertyStates mpss = UnoCast.cast(XMultiPropertyStates.class, cursor).get();

        XPropertySet propertySet = UnoCast.cast(XPropertySet.class, cursor).get();
        XPropertyState xPropertyState = UnoCast.cast(XPropertyState.class, cursor).get();

        try {
            // Special handling
            propertySet.setPropertyValue(CHAR_STYLE_NAME, "Standard");
            xPropertyState.setPropertyToDefault("CharCaseMap");
        } catch (UnknownPropertyException |
                 PropertyVetoException |
                 WrappedTargetException ex) {
            LOGGER.warn("exception caught", ex);
        }

        mpss.setAllPropertiesToDefault();

        /*
         * Now that we have called setAllPropertiesToDefault, check which properties are not set to
         * default and try to correct what we can and seem necessary.
         *
         * Note: tested with LibreOffice : 6.4.6.2
         */

        // Only report those we do not yet know about
        final Set<String> knownToFail = Set.of("ListAutoFormat",
                                               "ListId",
                                               "NumberingIsNumber",
                                               "NumberingLevel",
                                               "NumberingRules",
                                               "NumberingStartValue",
                                               "ParaChapterNumberingLevel",
                                               "ParaIsNumberingRestart",
                                               "ParaStyleName");

        // query again, just in case it matters
        propertySet = UnoCast.cast(XPropertySet.class, cursor).get();
        XPropertySetInfo propertySetInfo = propertySet.getPropertySetInfo();

        // check the result
        for (Property p : propertySetInfo.getProperties()) {
            if ((p.Attributes & PropertyAttribute.READONLY) != 0) {
                continue;
            }
            try {
                if (isPropertyDefault(cursor, p.Name)) {
                    continue;
                }
            } catch (UnknownPropertyException ex) {
                throw new IllegalStateException("Unexpected UnknownPropertyException", ex);
            }
            if (knownToFail.contains(p.Name)) {
                continue;
            }
            LOGGER.warn(String.format("OOTextIntoOO.removeDirectFormatting failed on '%s'", p.Name));
        }
    }

    static class MyPropertyStack {

        /*
         * We only try to control these. Should include all character properties we set, and maybe
         * their interdependencies.
         *
         * For a list of properties see:
         * https://www.openoffice.org/api/docs/common/ref/com/sun/star/style/CharacterProperties.html
         *
         * For interdependencies between properties:
         * https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Formatting
         * (at the end, under "Interdependencies between Properties")
         *
         */
        static final Set<String> CONTROLLED_PROPERTIES = Set.of(

            /* Used for SuperScript, SubScript.
             *
             * These three are interdependent: changing one may change others.
             */
            "CharEscapement", "CharEscapementHeight", "CharAutoEscapement",

            /* used for Bold */
            "CharWeight",

            /* Used for Italic */
            "CharPosture",

            /* Used for strikeout. These two are interdependent. */
            "CharStrikeout", "CharCrossedOut",

            /* Used for underline. These three are interdependent, but apparently
             * we can leave out the last two.
             */
            "CharUnderline", // "CharUnderlineColor", "CharUnderlineHasColor",

            /* Used for lang="zxx", to silence spellchecker. */
            "CharLocale",

            /* Used for CitationCharacterFormat.  */
            "CharStyleName",

            /* Used for <smallcaps> and <span style="font-variant: small-caps"> */
            "CharCaseMap");

        /**
         * The number of properties actually controlled.
         */
        final int goodSize;

        /**
         * From property name to index in goodNames.
         */
        final Map<String, Integer> goodNameToIndex;

        /**
         * From index to property name.
         */
        final String[] goodNames;

        /**
         * Maintain a stack of layers, each containing a description of the desired state of
         * properties. Each description is an ArrayList of property values, Optional.empty()
         * encoding "not directly set".
         */
        final Stack<ArrayList<Optional<Object>>> layers;

        MyPropertyStack(XTextCursor cursor) {

            XPropertySet propertySet = UnoCast.cast(XPropertySet.class, cursor).get();
            XPropertySetInfo propertySetInfo = propertySet.getPropertySetInfo();

            /*
             * On creation, initialize the property name -- index mapping.
             */
            this.goodNameToIndex = new HashMap<>();
            int nextIndex = 0;
            for (Property p : propertySetInfo.getProperties()) {
                if ((p.Attributes & PropertyAttribute.READONLY) != 0) {
                    continue;
                }
                if (!CONTROLLED_PROPERTIES.contains(p.Name)) {
                    continue;
                }
                this.goodNameToIndex.put(p.Name, nextIndex);
                nextIndex++;
            }

            this.goodSize = nextIndex;

            this.goodNames = new String[goodSize];
            for (Map.Entry<String, Integer> entry : goodNameToIndex.entrySet()) {
                goodNames[ entry.getValue() ] = entry.getKey();
            }

            // XMultiPropertySet.setPropertyValues() requires alphabetically sorted property names.
            // We adjust here:
            Arrays.sort(goodNames);
            for (int i = 0; i < goodSize; i++) {
                this.goodNameToIndex.put(goodNames[i], i);
            }

            /*
             * Get the initial state of the properties and add the first layer.
             */
            XMultiPropertyStates mpss = UnoCast.cast(XMultiPropertyStates.class, cursor).get();
            PropertyState[] propertyStates;
            try {
                propertyStates = mpss.getPropertyStates(goodNames);
            } catch (UnknownPropertyException ex) {
                throw new IllegalStateException("Caught unexpected UnknownPropertyException", ex);
            }

            XMultiPropertySet mps = UnoCast.cast(XMultiPropertySet.class, cursor).get();
            Object[] initialValues = mps.getPropertyValues(goodNames);

            ArrayList<Optional<Object>> initialValuesOpt = new ArrayList<>(goodSize);

            for (int i = 0; i < goodSize; i++) {
                if (propertyStates[i] == PropertyState.DIRECT_VALUE) {
                    initialValuesOpt.add(Optional.of(initialValues[i]));
                } else {
                    initialValuesOpt.add(Optional.empty());
                }
            }

            this.layers = new Stack<>();
            this.layers.push(initialValuesOpt);
        }

        /**
         * Given a list of property name, property value pairs, construct and push a new layer
         * describing the intended state after these have been applied.
         *
         * Opening tags usually call this.
         */
        void pushLayer(List<OOPair<String, Object>> settings) {
            ArrayList<Optional<Object>> oldLayer = layers.peek();
            ArrayList<Optional<Object>> newLayer = new ArrayList<>(oldLayer);
            for (OOPair<String, Object> pair : settings) {
                String name = pair.a;
                Integer index = goodNameToIndex.get(name);
                if (index == null) {
                    LOGGER.warn(String.format("pushLayer: '%s' is not in goodNameToIndex", name));
                    continue;
                }
                Object newValue = pair.b;
                newLayer.set(index, Optional.ofNullable(newValue));
            }
            layers.push(newLayer);
        }

        /**
         * Closing tags just pop a layer.
         */
        void popLayer() {
            if (layers.size() <= 1) {
                LOGGER.warn("popLayer: underflow");
                return;
            }
            layers.pop();
        }

        /**
         * Apply the current desired formatting state to a cursor.
         *
         * The idea is to minimize the number of calls to OpenOffice.
         */
        void apply(XTextCursor cursor) {
            XMultiPropertySet mps = UnoCast.cast(XMultiPropertySet.class, cursor).get();
            XMultiPropertyStates mpss = UnoCast.cast(XMultiPropertyStates.class, cursor).get();
            ArrayList<Optional<Object>> topLayer = layers.peek();
            try {
                // select values to be set
                ArrayList<String> names = new ArrayList<>(goodSize);
                ArrayList<Object> values = new ArrayList<>(goodSize);
                // and those to be cleared
                ArrayList<String> delNames = new ArrayList<>(goodSize);
                for (int i = 0; i < goodSize; i++) {
                    if (topLayer.get(i).isPresent()) {
                        names.add(goodNames[i]);
                        values.add(topLayer.get(i).get());
                    } else {
                        delNames.add(goodNames[i]);
                    }
                }
                // namesArray must be alphabetically sorted.
                String[] namesArray = names.toArray(new String[0]);
                String[] delNamesArray = delNames.toArray(new String[0]);
                mpss.setPropertiesToDefault(delNamesArray);
                mps.setPropertyValues(namesArray, values.toArray());
            } catch (UnknownPropertyException ex) {
                LOGGER.warn("UnknownPropertyException in MyPropertyStack.apply", ex);
            } catch (PropertyVetoException ex) {
                LOGGER.warn("PropertyVetoException in MyPropertyStack.apply");
            } catch (WrappedTargetException ex) {
                LOGGER.warn("WrappedTargetException in MyPropertyStack.apply");
            }
        }

        // Relative CharEscapement needs to know current values.
        Optional<Object> getPropertyValue(String name) {
            if (goodNameToIndex.containsKey(name)) {
                int index = goodNameToIndex.get(name);
                ArrayList<Optional<Object>> topLayer = layers.peek();
                return topLayer.get(index);
            }
            return Optional.empty();
        }
    }

    /**
     * Parse HTML-like attributes to a list of (name,value) pairs.
     */
    private static List<OOPair<String, String>> parseAttributes(String attributes) {
        List<OOPair<String, String>> res = new ArrayList<>();
        if (attributes == null) {
            return res;
        }
        Matcher attributeMatcher = ATTRIBUTE_PATTERN.matcher(attributes);
        while (attributeMatcher.find()) {
            String key = attributeMatcher.group(1);
            String value = attributeMatcher.group(2);
            res.add(new OOPair<String, String>(key, value));
        }
        return res;
    }

    /*
     * We rely on property values being either DIRECT_VALUE or DEFAULT_VALUE (not
     * AMBIGUOUS_VALUE). If the cursor covers a homogeneous region, or is collapsed, then this is
     * true.
     */
    private static boolean isPropertyDefault(XTextCursor cursor, String propertyName)
        throws
        UnknownPropertyException {
        XPropertyState xPropertyState = UnoCast.cast(XPropertyState.class, cursor).get();
        PropertyState state = xPropertyState.getPropertyState(propertyName);
        if (state == PropertyState.AMBIGUOUS_VALUE) {
            throw new java.lang.IllegalArgumentException("PropertyState.AMBIGUOUS_VALUE"
                                                         + " (expected properties for a homogeneous cursor)");
        }
        return state == PropertyState.DEFAULT_VALUE;
    }

    /*
     * Various property change requests. Their results are passed to MyPropertyStack.pushLayer()
     */

    private static List<OOPair<String, Object>> setCharWeight(float value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        settings.add(new OOPair<>("CharWeight", (Float) value));
        return settings;
    }

    private static List<OOPair<String, Object>> setCharPosture(FontSlant value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        settings.add(new OOPair<>("CharPosture", (Object) value));
        return settings;
    }

    private static List<OOPair<String, Object>> setCharCaseMap(short value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        settings.add(new OOPair<>("CharCaseMap", (Short) value));
        return settings;
    }

    // com.sun.star.awt.FontUnderline
    private static List<OOPair<String, Object>> setCharUnderline(short value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        settings.add(new OOPair<>(CHAR_UNDERLINE, (Short) value));
        return settings;
    }

    // com.sun.star.awt.FontStrikeout
    private static List<OOPair<String, Object>> setCharStrikeout(short value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        settings.add(new OOPair<>(CHAR_STRIKEOUT, (Short) value));
        return settings;
    }

    // CharStyleName
    private static List<OOPair<String, Object>> setCharStyleName(String value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        if (StringUtil.isNullOrEmpty(value)) {
            LOGGER.warn("setCharStyleName: received null or empty value");
        } else {
            settings.add(new OOPair<>(CHAR_STYLE_NAME, value));
        }
        return settings;
    }

    // Locale
    private static List<OOPair<String, Object>> setCharLocale(Locale value) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        settings.add(new OOPair<>("CharLocale", (Object) value));
        return settings;
    }

    /**
     * Locale from string encoding: language, language-country or language-country-variant
     */
    private static List<OOPair<String, Object>> setCharLocale(String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            throw new java.lang.IllegalArgumentException("setCharLocale \"\" or null");
        }
        String[] parts = value.split("-");
        String language = (parts.length > 0) ? parts[0] : "";
        String country = (parts.length > 1) ? parts[1] : "";
        String variant = (parts.length > 2) ? parts[2] : "";
        return setCharLocale(new Locale(language, country, variant));
    }

    /*
     * SuperScript and SubScript.
     *
     * @param relative If true, calculate the new values relative to the current values. This allows
     *                 subscript-in-superscript.
     */
    private static List<OOPair<String, Object>> setCharEscapement(Optional<Short> value,
                                                                  Optional<Byte> height,
                                                                  boolean relative,
                                                                  MyPropertyStack formatStack) {
        List<OOPair<String, Object>> settings = new ArrayList<>();
        Optional<Short> oldValue = (formatStack
                                    .getPropertyValue(CHAR_ESCAPEMENT)
                                    .map(e -> (short) e));

        Optional<Byte> oldHeight = (formatStack
                                    .getPropertyValue(CHAR_ESCAPEMENT_HEIGHT)
                                    .map(e -> (byte) e));

        if (relative && (value.isPresent() || height.isPresent())) {
            double oldHeightFloat = oldHeight.orElse(CHAR_ESCAPEMENT_HEIGHT_DEFAULT) * 0.01;
            double oldValueFloat = oldValue.orElse(CHAR_ESCAPEMENT_VALUE_DEFAULT);
            double heightFloat = height.orElse(CHAR_ESCAPEMENT_HEIGHT_DEFAULT);
            double valueFloat = value.orElse(CHAR_ESCAPEMENT_VALUE_DEFAULT);
            byte newHeight = (byte) Math.round(heightFloat * oldHeightFloat);
            short newValue = (short) Math.round(valueFloat * oldHeightFloat + oldValueFloat);
            if (value.isPresent()) {
                settings.add(new OOPair<>(CHAR_ESCAPEMENT, (Short) newValue));
            }
            if (height.isPresent()) {
                settings.add(new OOPair<>(CHAR_ESCAPEMENT_HEIGHT, (Byte) newHeight));
            }
        } else {
            if (value.isPresent()) {
                settings.add(new OOPair<>(CHAR_ESCAPEMENT, (Short) value.get()));
            }
            if (height.isPresent()) {
                settings.add(new OOPair<>(CHAR_ESCAPEMENT_HEIGHT, (Byte) height.get()));
            }
        }
        return settings;
    }

    private static List<OOPair<String, Object>> setSubScript(MyPropertyStack formatStack) {
        return setCharEscapement(Optional.of(SUBSCRIPT_VALUE),
                                 Optional.of(SUBSCRIPT_HEIGHT),
                                 true,
                                 formatStack);
    }

    private static List<OOPair<String, Object>> setSuperScript(MyPropertyStack formatStack) {
        return setCharEscapement(Optional.of(SUPERSCRIPT_VALUE),
                                 Optional.of(SUPERSCRIPT_HEIGHT),
                                 true,
                                 formatStack);
    }

    /*
     * @return true on failure
     */
    public static boolean setParagraphStyle(XTextCursor cursor, String paragraphStyle) {
        final boolean FAIL = true;
        final boolean PASS = false;

        XParagraphCursor paragraphCursor = UnoCast.cast(XParagraphCursor.class, cursor).get();
        XPropertySet propertySet = UnoCast.cast(XPropertySet.class, paragraphCursor).get();
        try {
            propertySet.setPropertyValue(PARA_STYLE_NAME, paragraphStyle);
            return PASS;
        } catch (UnknownPropertyException
                 | PropertyVetoException
                 | com.sun.star.lang.IllegalArgumentException
                 | WrappedTargetException ex) {
            return FAIL;
        }
    }

    private static void insertParagraphBreak(XText text, XTextCursor cursor) {
        try {
            text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, true);
        } catch (com.sun.star.lang.IllegalArgumentException ex) {
            // Assuming it means wrong code for ControlCharacter.
            // https://api.libreoffice.org/docs/idl/ref/  does not tell.
            // If my assumption is correct, we never get here.
            throw new java.lang.IllegalArgumentException("Caught unexpected com.sun.star.lang.IllegalArgumentException", ex);
        }
    }

}
