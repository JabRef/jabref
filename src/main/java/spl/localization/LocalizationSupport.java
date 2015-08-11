package spl.localization;

import net.sf.jabref.logic.l10n.Localization;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 08.09.2010
 * Time: 09:56:31
 * To change this template use File | Settings | File Templates.
 */
public class LocalizationSupport {

    public static String message(String key) {
        return Localization.lang(key);
    }

}
