package org.jabref.gui.fieldeditors.contextmenu;

import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import org.jabref.gui.actions.StandardActions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class EditorContextActionTest {

    private static final int TIMEOUT_SECONDS = 10;

    private TextArea textArea;

    @Start
    public void onStart(Stage stage) {
        textArea = new TextArea("some text");
    }

    /// The text control enforces its editable state for typing only ([TextArea#cut()] and friends
    /// change a read-only control just fine), so the context menu must not offer any text-changing
    /// action for a read-only editor - e.g. another user's comment in the Comments tab.
    @Test
    void readOnlyInputOffersNoTextChangingAction(FxRobot robot) {
        robot.interact(() -> {
            // A read-only editor still gets text from its binding, which fills the undo history
            textArea.appendText(" written by its binding");
            textArea.setEditable(false);
            textArea.selectAll();
        });
        assertTrue(textArea.isUndoable(), "precondition: undo would have something to revert");

        assertFalse(isExecutable(StandardActions.CUT));
        assertFalse(isExecutable(StandardActions.DELETE));
        assertFalse(isExecutable(StandardActions.PASTE));
        assertFalse(isExecutable(StandardActions.UNDO));
        assertFalse(isExecutable(StandardActions.REDO));
    }

    /// Copying does not change the text, so it stays available for a read-only editor.
    @Test
    void readOnlyInputCanStillBeCopied(FxRobot robot) {
        robot.interact(() -> {
            textArea.setEditable(false);
            textArea.selectAll();
        });

        assertTrue(isExecutable(StandardActions.COPY));
    }

    @Test
    void editableInputCanBeCutCopiedAndDeleted(FxRobot robot) {
        robot.interact(() -> textArea.selectAll());

        assertTrue(isExecutable(StandardActions.CUT));
        assertTrue(isExecutable(StandardActions.COPY));
        assertTrue(isExecutable(StandardActions.DELETE));
    }

    /// Without a selection there is nothing to cut, copy, or delete.
    @Test
    void nothingIsOfferedWithoutSelection(FxRobot robot) {
        robot.interact(() -> textArea.deselect());

        assertFalse(isExecutable(StandardActions.CUT));
        assertFalse(isExecutable(StandardActions.COPY));
        assertFalse(isExecutable(StandardActions.DELETE));
    }

    /// The action reads the system clipboard while being created, so it must be built on the FX thread.
    private boolean isExecutable(StandardActions action) {
        return WaitForAsyncUtils.waitForAsyncFx(TIMEOUT_SECONDS * 1000,
                () -> new EditorContextAction(action, textArea).executableProperty().get());
    }
}
