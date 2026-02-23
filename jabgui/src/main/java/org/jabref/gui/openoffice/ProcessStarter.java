package org.jabref.gui.openoffice;

import java.io.IOException;

/**
 * Functional interface responsible for starting an external process.
 */
@FunctionalInterface
interface ProcessStarter {
    Process exec(String[] cmd) throws IOException;
}
