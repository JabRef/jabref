package org.jabref.gui.icon;

import java.util.List;
import java.util.Set;

import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.scene.paint.Paint;

import org.jspecify.annotations.NullMarked;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.javafx.FontIcon;

/// A [FontIcon] whose color is chosen in code rather than by the theme -- a group's color, or the gray of a
/// disabled icon. The font-backed counterpart to [JabRefSvgIcon].
///
/// Plain [FontIcon#setIconColor(Paint)] is not enough for that: JavaFX lets a value from an author stylesheet
/// win over one set programmatically, so the `.glyph-icon` and `.ikonli-font-icon` rules in jabref-base.css
/// would paint the icon in the theme's text color again on the next CSS pass. The usual way out -- reporting
/// the property as not settable -- is closed too, because Ikonli's `-fx-icon-color` metadata answers
/// [CssMetaData#isSettable] with a constant `true`.
///
/// What is left is [#getCssMetaData()]: CSS only ever touches the properties a node lists there, so this node
/// lists neither color property. Everything else -- sizes, bounds type -- stays under the theme's control.
@NullMarked
class JabRefFontIcon extends FontIcon {

    /// The properties that color a [FontIcon]. `-fx-icon-color` feeds `-fx-fill` through Ikonli's own listener,
    /// so both have to go for the color to survive.
    private static final Set<String> COLOR_PROPERTIES = Set.of("-fx-fill", "-fx-icon-color");

    private static final List<CssMetaData<? extends Styleable, ?>> CSS_META_DATA =
            FontIcon.getClassCssMetaData()
                    .stream()
                    .filter(metaData -> !COLOR_PROPERTIES.contains(metaData.getProperty()))
                    .toList();

    JabRefFontIcon(Ikon ikon, int size, Paint color) {
        setIconCode(ikon);
        setIconSize(size);
        setIconColor(color);
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return CSS_META_DATA;
    }
}
