package net.sf.jabref.gui.util;

import java.awt.Component;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FocusRequester implements Runnable {

    private static final Log LOGGER = LogFactory.getLog(FocusRequester.class);

    private final Component comp;

    public FocusRequester(Component comp) {
        if (comp == null) {
            Thread.dumpStack();
        }

        this.comp = comp;

        run();
    }

    @Override
    public void run() {
        LOGGER.debug("requesting focus for " + comp);
        comp.requestFocus();
    }
}
