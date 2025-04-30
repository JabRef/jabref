package org.jabref.model.openoffice.uno;

import com.sun.star.text.XTextDocument;

/**
 * Disable/enable screen refresh.
 */
public class UnoScreenRefresh {

    private UnoScreenRefresh() {
    }

    /**
     * Disable screen refresh.
     * <p>
     * Must be paired with unlockControllers()
     * <p>
     * https://www.openoffice.org/api/docs/common/ref/com/sun/star/frame/XModel.html
     * <p>
     * While there is at least one lock remaining, some notifications for display updates are not broadcasted.
     */
    public static void lockControllers(XTextDocument doc) {
        doc.lockControllers();
    }

    public static void unlockControllers(XTextDocument doc) {
        doc.unlockControllers();
    }

    public static boolean hasControllersLocked(XTextDocument doc) {
        return doc.hasControllersLocked();
    }
}
