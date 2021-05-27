package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.style.XStyle;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.text.XTextDocument;

/**
 * Styles in the document.
 */
public class UnoStyle {

    public static final String CHARACTER_STYLES = "CharacterStyles";
    public static final String PARAGRAPH_STYLES = "ParagraphStyles";

    private UnoStyle() { }

    private static Optional<XStyle> getStyleFromFamily(XTextDocument doc,
                                                       String familyName,
                                                       String styleName)
        throws
        WrappedTargetException {

        XStyleFamiliesSupplier fss = UnoCast.unoQI(XStyleFamiliesSupplier.class, doc);
        XNameAccess fs = UnoCast.unoQI(XNameAccess.class, fss.getStyleFamilies());
        XNameContainer xFamily;
        try {
            xFamily = UnoCast.unoQI(XNameContainer.class, fs.getByName(familyName));
        } catch (NoSuchElementException ex) {
            String msg = String.format("Style family name '%s' is not recognized", familyName);
            throw new RuntimeException(msg, ex);
        }

        try {
            Object s = xFamily.getByName(styleName);
            XStyle xs = (XStyle) UnoCast.unoQI(XStyle.class, s);
            return Optional.ofNullable(xs);
        } catch (NoSuchElementException ex) {
            return Optional.empty();
        }
    }

    public static Optional<XStyle> getParagraphStyle(XTextDocument doc, String styleName)
        throws
        WrappedTargetException {
        return getStyleFromFamily(doc, PARAGRAPH_STYLES, styleName);
    }

    public static Optional<XStyle> getCharacterStyle(XTextDocument doc, String styleName)
        throws
        WrappedTargetException {
        return getStyleFromFamily(doc, CHARACTER_STYLES, styleName);
    }

    public static Optional<String> getInternalNameOfStyle(XTextDocument doc, String familyName,
                                                          String name)
        throws
        WrappedTargetException {
        return (getStyleFromFamily(doc, familyName, name)
                .map(e -> e.getName()));
    }

    public static Optional<String> getInternalNameOfParagraphStyle(XTextDocument doc, String name)
        throws
        WrappedTargetException {
        return getInternalNameOfStyle(doc, PARAGRAPH_STYLES, name);
    }

    public static Optional<String> getInternalNameOfCharacterStyle(XTextDocument doc, String name)
        throws
        WrappedTargetException {
        return getInternalNameOfStyle(doc, CHARACTER_STYLES, name);
    }
}
