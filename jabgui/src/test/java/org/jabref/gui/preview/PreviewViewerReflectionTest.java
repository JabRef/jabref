package org.jabref.gui.preview;

import java.lang.reflect.Method;

import javafx.beans.property.ReadOnlyDoubleProperty;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Lightweight tests that ensure the PreviewViewer exposes content size properties
 * used by UI components for adaptive-sizing (reflections avoid needing a JavaFX runtime here).
 */
public class PreviewViewerReflectionTest {

    @Test
    public void contentPropertiesExist() throws Exception {
        Class<?> clazz = PreviewViewer.class;

        Method heightMethod = clazz.getMethod("contentHeightProperty");
        assertEquals(ReadOnlyDoubleProperty.class, heightMethod.getReturnType());

        Method widthMethod = clazz.getMethod("contentWidthProperty");
        assertEquals(ReadOnlyDoubleProperty.class, widthMethod.getReturnType());
    }
}
