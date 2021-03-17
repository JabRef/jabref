package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.Property;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.Node;
import javafx.scene.control.Skin;
import javafx.scene.control.TitledPane;
import javafx.scene.control.skin.TitledPaneSkin;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;

import static javafx.css.StyleConverter.getEnumConverter;

/**
 *
 * CustomTitledPaneSkin with option to move arrow to the right
 * https://stackoverflow.com/a/55085777/3450689s
 */
public class CustomTitledPaneSkin extends TitledPaneSkin {

    public enum ArrowSide {
        LEFT, RIGHT
    }

    /* ********************************************************
     *                                                        *
     * Properties                                             *
     *                                                        *
     **********************************************************/

    private final StyleableObjectProperty<ArrowSide> arrowSide = new SimpleStyleableObjectProperty<>(StyleableProperties.ARROW_SIDE, this, "arrowSide", ArrowSide.LEFT) {

        @Override
        protected void invalidated() {
            adjustTitleLayout();
        }
    };

    public final void setArrowSide(ArrowSide arrowSide) {
        this.arrowSide.set(arrowSide);
    }

    public final ArrowSide getArrowSide() {
        return arrowSide.get();
    }

    public final ObjectProperty<ArrowSide> arrowSideProperty() {
        return arrowSide;
    }

    /* ********************************************************
     *                                                        *
     * Instance Fields                                        *
     *                                                        *
     **********************************************************/

    private final Region title;
    private final Region arrowButton;
    private final Region arrow;
    private final Text text;

    private DoubleBinding arrowTranslateBinding;
    private DoubleBinding textGraphicTranslateBinding;
    private Node graphic;

    /* ********************************************************
     *                                                        *
     * Constructors                                           *
     *                                                        *
     **********************************************************/

    public CustomTitledPaneSkin(TitledPane control) {
        super(control);
        title = (Region) Objects.requireNonNull(control.lookup(".title"));
        arrowButton = (Region) Objects.requireNonNull(title.lookup(".arrow-button"));
        arrow = (Region) Objects.requireNonNull(arrowButton.lookup(".arrow"));
        text = (Text) Objects.requireNonNull(title.lookup(".text"));

        // based on https://stackoverflow.com/a/55156460/3450689
        Rotate rotate = new Rotate();
        rotate.pivotXProperty().bind(arrow.widthProperty().divide(2.0));
        rotate.pivotYProperty().bind(arrow.heightProperty().divide(2.0));
        rotate.angleProperty().bind(
                Bindings.when(control.expandedProperty())
                        .then(-180.0)
                        .otherwise(90.0));

        arrow.getTransforms().add(rotate);

        registerChangeListener(control.graphicProperty(), ov -> adjustTitleLayout());
    }

    /* ********************************************************
     *                                                        *
     * Skin Stuff                                             *
     *                                                        *
     **********************************************************/

    private void adjustTitleLayout() {
        clearBindings();
        if (getArrowSide() != ArrowSide.RIGHT) {
            // if arrow is on the left we don't need to translate anything
            return;
        }

        arrowTranslateBinding = Bindings.createDoubleBinding(() -> {
            double rightInset = title.getPadding().getRight();
            return title.getWidth() - arrowButton.getLayoutX() - arrowButton.getWidth() - rightInset;
        }, title.paddingProperty(), title.widthProperty(), arrowButton.widthProperty(), arrowButton.layoutXProperty());
        arrowButton.translateXProperty().bind(arrowTranslateBinding);

        textGraphicTranslateBinding = Bindings.createDoubleBinding(
                () -> switch (getSkinnable().getAlignment()) {
                    case TOP_CENTER, CENTER, BOTTOM_CENTER, BASELINE_CENTER -> 0.0;
                    default -> -(arrowButton.getWidth());
                }, getSkinnable().alignmentProperty(), arrowButton.widthProperty());
        text.translateXProperty().bind(textGraphicTranslateBinding);

        graphic = getSkinnable().getGraphic();
        if (graphic != null) {
            graphic.translateXProperty().bind(textGraphicTranslateBinding);
        }
    }

    private void clearBindings() {
        if (arrowTranslateBinding != null) {
            arrowButton.translateXProperty().unbind();
            arrowButton.setTranslateX(0);
            arrowTranslateBinding.dispose();
            arrowTranslateBinding = null;
        }
        if (textGraphicTranslateBinding != null) {
            text.translateXProperty().unbind();
            text.setTranslateX(0);
            if (graphic != null) {
                graphic.translateXProperty().unbind();
                graphic.setTranslateX(0);
                graphic = null;
            }
            textGraphicTranslateBinding.dispose();
            textGraphicTranslateBinding = null;
        }
    }

    @Override
    public void dispose() {
        clearBindings();
        unregisterChangeListeners(getSkinnable().graphicProperty());
        super.dispose();
    }

    /* ********************************************************
     *                                                        *
     * Stylesheet Handling                                    *
     *                                                        *
     **********************************************************/

    public static List<CssMetaData<?, ?>> getClassCssMetaData() {
        return StyleableProperties.CSS_META_DATA;
    }

    @Override
    public List<CssMetaData<?, ?>> getCssMetaData() {
        return getClassCssMetaData();
    }

    private static class StyleableProperties {

        private static final CssMetaData<TitledPane, ArrowSide> ARROW_SIDE = new CssMetaData<>("-fx-arrow-side", getEnumConverter(ArrowSide.class), ArrowSide.LEFT) {

            @Override
            public boolean isSettable(TitledPane styleable) {
                Property<?> prop = (Property<?>) getStyleableProperty(styleable);
                return (prop != null) && !prop.isBound();
            }

            @Override
            public StyleableProperty<ArrowSide> getStyleableProperty(TitledPane styleable) {
                Skin<?> skin = styleable.getSkin();
                if (skin instanceof CustomTitledPaneSkin) {
                    return ((CustomTitledPaneSkin) skin).arrowSide;
                }
                return null;
            }

        };

        private static final List<CssMetaData<?, ?>> CSS_META_DATA;

        static {
            List<CssMetaData<?, ?>> list = new ArrayList<>(TitledPane.getClassCssMetaData().size() + 1);
            list.addAll(TitledPaneSkin.getClassCssMetaData());
            list.add(ARROW_SIDE);
            CSS_META_DATA = Collections.unmodifiableList(list);
        }

    }
}
