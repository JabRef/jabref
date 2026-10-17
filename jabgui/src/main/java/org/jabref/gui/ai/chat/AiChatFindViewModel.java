package org.jabref.gui.ai.chat;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.AbstractViewModel;

import org.jspecify.annotations.NullMarked;

/// State of the find bar of [AiChatView]: the query, whether the bar is shown, and which occurrence is the current one.
/// The view reports the number of occurrences it highlighted via [#setTotal(int)].
@NullMarked
public class AiChatFindViewModel extends AbstractViewModel {
    private final BooleanProperty visible = new SimpleBooleanProperty(false);
    private final StringProperty query = new SimpleStringProperty("");
    private final IntegerProperty current = new SimpleIntegerProperty(0);
    private final IntegerProperty total = new SimpleIntegerProperty(0);
    private final StringBinding resultText = Bindings.createStringBinding(
            () -> getActiveQuery().isEmpty() ? "" : (total.get() == 0 ? 0 : current.get() + 1) + "/" + total.get(),
            visible, query, current, total);

    public AiChatFindViewModel() {
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
        if (total.get() > 0) {
            current.set((current.get() + 1) % total.get());
        }
    }

    public void previous() {
        if (total.get() > 0) {
            current.set((current.get() - 1 + total.get()) % total.get());
        }
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

    public int getCurrent() {
        return current.get();
    }

    public StringBinding resultTextProperty() {
        return resultText;
    }
}
