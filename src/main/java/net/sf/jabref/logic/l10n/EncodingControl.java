package net.sf.jabref.logic.l10n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.ResourceBundle.Control;
import java.util.Locale;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * {@link Control} class allowing properties bundles to be in different encodings.
 *
 * @see <a href="http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle">utf-8
 *      and property files</a>
 */
public class EncodingControl extends Control {

    private final Charset encoding;


    public EncodingControl(Charset encoding) {
        this.encoding = encoding;
    }

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale,
                                    String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException,
            IOException {
        // The below is a copy of the default implementation.
        String bundleName = toBundleName(baseName, locale);
        String resourceName = toResourceName(bundleName, "properties");
        ResourceBundle bundle = null;
        if (reload) {
            URL url = loader.getResource(resourceName);
            if (url != null) {
                URLConnection connection = url.openConnection();
                if (connection != null) {
                    connection.setUseCaches(false);
                    try (InputStream stream = connection.getInputStream()) {
                        bundle = new PropertyResourceBundle(new InputStreamReader(stream, encoding));
                    }
                }
            }
        } else {
            try (InputStream stream = loader.getResourceAsStream(resourceName)) {
                bundle = new PropertyResourceBundle(new InputStreamReader(stream, encoding));
            }
        }
        return bundle;
    }
}


