package org.jabref.gui.ai.chat;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.util.BindingsHelper;

import org.jspecify.annotations.NullMarked;

/// State of [AiChatFindBar]: the query, whether the bar is shown, and which occurrence is the current one.
/// The view reports the number of occurrences it highlighted via [#setTotal(int)].
@NullMarked
public class AiChatFindViewModel extends AbstractViewModel {
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final StringProperty query = new SimpleStringProperty("");
    private final IntegerProperty current = new SimpleIntegerProperty(0);
    private final IntegerProperty total = new SimpleIntegerProperty(0);
    private final ObjectProperty<EventHandler<ActionEvent>> onClose = new SimpleObjectProperty<>();
    private final ObjectProperty<EventHandler<ActionEvent>> onNavigate = new SimpleObjectProperty<>();

    public AiChatFindViewModel() {
        // Reset counter when the query is changed.
        query.addListener((_, _, _) -> current.set(0));
    }

    /// The query to highlight; empty while the find bar is hidden.
    public String getActiveQuery() {
        return visible.get() ? query.get() : "";
    }

    /// Records the number of highlighted occurrences.
    ///
    /// @return `false` if the current occurrence no longer exists and was reset to the first one, so the highlighting has to be redone
    public boolean setTotal(int value) {
        total.set(value);
        if (value > 0 && current.get() >= value) {
            current.set(0);
            return false;
        }
        return true;
    }

    public void next() {
        if (total.get() == 0) {
            return;
        }
        current.set((current.get() + 1) % total.get());
        BindingsHelper.handle(onNavigate);
    }

    public void previous() {
        if (total.get() == 0) {
            return;
        }
        current.set((current.get() - 1 + total.get()) % total.get());
        BindingsHelper.handle(onNavigate);
    }

    public void close() {
        BindingsHelper.handle(onClose);
    }

    public BooleanProperty visibleProperty() {
        return visible;
    }

    public StringProperty queryProperty() {
        return query;
    }

    public IntegerProperty currentProperty() {
        return current;
    }

    public IntegerProperty totalProperty() {
        return total;
    }

    public int getCurrent() {
        return current.get();
    }

    public ObjectProperty<EventHandler<ActionEvent>> onNavigateProperty() {
        return onNavigate;
    }

    public ObjectProperty<EventHandler<ActionEvent>> onCloseProperty() {
        return onClose;
    }
}
