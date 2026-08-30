package org.jabref.gui.fieldeditors.contextmenu;

import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.MenuItem;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.Clipboard;

import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.os.OS;

import com.sun.javafx.scene.control.Properties;

/// A context-menu command acting on one [TextInputControl], through the control's own API.
///
/// [StandardActions#UNDO] and [StandardActions#REDO] therefore drive *that control's* private
/// undo stack, which is right only where the control's text is not a value of the library —
/// today that is the search field alone. In a field editor the same two items would fight the
/// library's journal: `Ctrl+Z` there is routed to the global stack by the filter in
/// [org.jabref.gui.fieldeditors.FieldEditorFX#establishBinding] (issue #11420), so a menu Undo
/// would revert the control's text, the text listener would see a fresh edit, and the undo the
/// user asked for would arrive on the stack as a new forward change.
///
/// That is why [#getDefaultContextMenuItems] leaves both out. Wiring undo into a field editor's
/// menu means routing it to [org.jabref.gui.undo.UndoAction], not to the control.
public class EditorContextAction extends SimpleCommand {

    private static final boolean SHOW_HANDLES = Properties.IS_TOUCH_SUPPORTED && !OS.OS_X;

    private final StandardActions command;
    private final TextInputControl textInputControl;

    public EditorContextAction(StandardActions command, TextInputControl textInputControl) {
        this.command = command;
        this.textInputControl = textInputControl;

        BooleanProperty editableBinding = textInputControl.editableProperty();
        BooleanBinding hasTextBinding = Bindings.createBooleanBinding(() -> textInputControl.getLength() > 0, textInputControl.textProperty());
        BooleanBinding hasStringInClipboardBinding = (BooleanBinding) BindingsHelper.constantOf(Clipboard.getSystemClipboard().hasString());
        BooleanBinding hasSelectionBinding = Bindings.createBooleanBinding(() -> textInputControl.getSelection().getLength() > 0, textInputControl.selectionProperty());
        BooleanBinding allSelectedBinding = Bindings.createBooleanBinding(() -> textInputControl.getSelection().getLength() == textInputControl.getLength());
        BooleanBinding maskTextBinding = (BooleanBinding) BindingsHelper.constantOf(textInputControl instanceof PasswordField); // (maskText("A") != "A");
        BooleanBinding undoableBinding = Bindings.createBooleanBinding(textInputControl::isUndoable, textInputControl.undoableProperty());
        BooleanBinding redoableBinding = Bindings.createBooleanBinding(textInputControl::isRedoable, textInputControl.redoableProperty());

        this.executable.bind(
                switch (command) {
                    case COPY ->
                            editableBinding.and(maskTextBinding.not()).and(hasSelectionBinding);
                    case CUT ->
                            maskTextBinding.not().and(hasSelectionBinding);
                    case PASTE ->
                            editableBinding.and(hasStringInClipboardBinding);
                    case DELETE ->
                            editableBinding.and(hasSelectionBinding);
                    case UNDO ->
                            undoableBinding;
                    case REDO ->
                            redoableBinding;
                    case SELECT_ALL -> {
                        if (SHOW_HANDLES) {
                            yield hasTextBinding.and(allSelectedBinding.not());
                        } else {
                            yield BindingsHelper.constantOf(true);
                        }
                    }
                    default ->
                            BindingsHelper.constantOf(true);
                });
    }

    @Override
    public void execute() {
        switch (command) {
            case COPY ->
                    textInputControl.copy();
            case CUT ->
                    textInputControl.cut();
            case PASTE ->
                    textInputControl.paste();
            case DELETE ->
                    textInputControl.deleteText(textInputControl.getSelection());
            case SELECT_ALL ->
                    textInputControl.selectAll();
            case UNDO ->
                    textInputControl.undo();
            case REDO ->
                    textInputControl.redo();
        }
        textInputControl.requestFocus();
    }

    /// The items every text field in the entry editor gets, deliberately without Undo and Redo:
    /// in a field editor those belong to the library's journal, not to the control. See the class
    /// javadoc.
    public static List<MenuItem> getDefaultContextMenuItems(TextInputControl textInputControl) {
        ActionFactory factory = new ActionFactory();

        MenuItem selectAllMenuItem = factory.createMenuItem(StandardActions.SELECT_ALL,
                new EditorContextAction(StandardActions.SELECT_ALL, textInputControl));
        if (SHOW_HANDLES) {
            selectAllMenuItem.getProperties().put("refreshMenu", Boolean.TRUE);
        }

        return List.of(
                factory.createMenuItem(StandardActions.CUT, new EditorContextAction(StandardActions.CUT, textInputControl)),
                factory.createMenuItem(StandardActions.COPY, new EditorContextAction(StandardActions.COPY, textInputControl)),
                factory.createMenuItem(StandardActions.PASTE, new EditorContextAction(StandardActions.PASTE, textInputControl)),
                factory.createMenuItem(StandardActions.DELETE, new EditorContextAction(StandardActions.DELETE, textInputControl)),
                selectAllMenuItem);
    }
}
