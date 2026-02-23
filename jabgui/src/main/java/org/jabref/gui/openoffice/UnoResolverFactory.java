package org.jabref.gui.openoffice;

import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.uno.XComponentContext;

/**
 * Functional interface for creating {@link XUnoUrlResolver} instances.
 */
@FunctionalInterface
interface UnoResolverFactory {
    XUnoUrlResolver create(XComponentContext ctx) throws com.sun.star.uno.Exception;
}
