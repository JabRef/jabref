package org.jabref.gui.util;

import javafx.beans.property.StringProperty;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import org.jabref.gui.icon.IconTheme;

/**
 * A base class for non-modal windows of JabRef.
 * <p>
 * You can create a new instance of this class and set the title in the constructor. After that you can call
 * {@link org.jabref.gui.DialogService#showCustomWindow(BaseWindow)} in order to show the window. All the JabRef styles
 * will be applied.
 * <p>
 * See {@link org.jabref.gui.ai.components.aichat.AiChatWindow} for example.
 */
public class BaseWindow extends Stage {
    public BaseWindow(StringProperty title, Window owner) {
        initOwner(owner);
        this.initModality(Modality.NONE);
        this.getIcons().add(IconTheme.getJabRefImage());
        this.titleProperty().bind(title);
    }

    public BaseWindow(StringProperty title) {
        this(title, null);
    }
}
