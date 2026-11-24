package org.jabref.gui.walkthrough.declarative;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIconView;
import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class NodeResolverTest {

    private final AtomicReference<Stage> stage = new AtomicReference<>();

    @AfterEach
    void closeStage() {
        JavaFxExtension.invokeAndWait(() -> stage.get().close());
    }

    /// The DOI row's "look up" button carries the same glyph as the file editor's "Get fulltext"
    /// button, so a scene-wide search must not be used for the walkthrough.
    @Test
    void buttonWithGraphicInIgnoresSameGlyphOutsideTheContainer() {
        Button outside = new Button("", new JabRefIconView(IconTheme.JabRefIcons.FETCH_FULLTEXT));
        Button inside = new Button("", new JabRefIconView(IconTheme.JabRefIcons.FETCH_FULLTEXT));
        HBox container = new HBox(inside);
        Scene scene = show(new VBox(outside, container));

        assertEquals(Optional.of(inside), NodeResolver.buttonWithGraphicIn(HBox.class, IconTheme.JabRefIcons.FETCH_FULLTEXT).resolve(scene));
        assertEquals(Optional.of(outside), NodeResolver.buttonWithGraphic(IconTheme.JabRefIcons.FETCH_FULLTEXT).resolve(scene));
    }

    @Test
    void firstOfFallsBackWhenPreferredNodeIsAbsent() {
        Button fallback = new Button();
        fallback.setId("fallback");
        Scene scene = show(new VBox(fallback));

        NodeResolver resolver = NodeResolver.firstOf(NodeResolver.fxId("preferred"), NodeResolver.fxId("fallback"));

        assertEquals(Optional.of(fallback), resolver.resolve(scene));
    }

    @Test
    void firstOfPrefersTheFirstResolverThatFinds() {
        Button preferred = new Button();
        preferred.setId("preferred");
        Button fallback = new Button();
        fallback.setId("fallback");
        Scene scene = show(new VBox(fallback, preferred));

        NodeResolver resolver = NodeResolver.firstOf(NodeResolver.fxId("preferred"), NodeResolver.fxId("fallback"));

        assertEquals(Optional.of(preferred), resolver.resolve(scene));
    }

    private Scene show(Node root) {
        Scene scene = new Scene(new VBox(root));
        JavaFxExtension.invokeAndWait(() -> {
            Stage newStage = new Stage();
            newStage.setScene(scene);
            newStage.show();
            stage.set(newStage);
        });
        return scene;
    }
}
