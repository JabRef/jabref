package org.jabref.gui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javafx.scene.Node;
import javafx.scene.control.SingleSelectionModel;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jabref.Globals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class JabRefFrameTest {

    private JabRefFrame jabRefFrame;
    private TabPane tabbedPane;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        tabbedPane = new TabPane();
        jabRefFrame = new JabRefFrame(new Stage());
        Field field = JabRefFrame.class.getField("tabbedPane");
        field.setAccessible(true);
        field.set(jabRefFrame, tabbedPane);
        field.setAccessible(false);
    }

    @Test
    void closeCurrentDatabase() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Tab tab1 = new Tab("lib1.bib", new StackPane());
        Tab tab2 = new Tab("lib2.bib", new StackPane());
        Tab tab3 = new Tab("lib3.bib", new StackPane());
        Tab tab4 = new Tab("lib4.bib", new StackPane());
        tabbedPane.getTabs().addAll(tab1, tab2, tab3, tab4);
        SingleSelectionModel<Tab> selectionModel = tabbedPane.getSelectionModel();
        selectionModel.select(tab1);
        Class<?> innerClazz = Class.forName("JabRefFrame$CloseDatabaseAction");
        Constructor<?> constructor = innerClazz.getDeclaredConstructor(JabRefFrame.class);
        constructor.setAccessible(true);
        Object o = constructor.newInstance(jabRefFrame);
        Method m = innerClazz.getDeclaredMethod("excute");
        m.invoke(o);
        assertFalse(tabbedPane.getTabs().contains(tab1));
        assertEquals(3, tabbedPane.getTabs().size());
    }

}
