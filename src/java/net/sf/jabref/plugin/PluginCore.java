package net.sf.jabref.plugin;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;

import net.sf.jabref.plugin.util.Util;

import org.java.plugin.ObjectFactory;
import org.java.plugin.PluginManager;
import org.java.plugin.PluginManager.PluginLocation;
import org.java.plugin.boot.DefaultPluginsCollector;
import org.java.plugin.standard.StandardPluginLocation;
import org.java.plugin.util.ExtendedProperties;

/**
 * Helper class for the plug-in system. Helps to retrieve the singleton instance
 * of the PluginManager.
 * 
 */
public class PluginCore {

	static PluginManager singleton;

	static PluginLocation getLocationInsideJar(String context, String manifest) {
		URL jar = PluginCore.class
				.getResource(Util.joinPath(context, manifest));
		if (jar != null && jar.getProtocol().toLowerCase().startsWith("jar")) {
			try {
				return new StandardPluginLocation(new URL(jar.toExternalForm()
						.replaceFirst("!(.*?)$", Util.joinPath("!", context))),
						jar);
			} catch (MalformedURLException e) {
				return null;
			}
		}
		return null;
	}

	public static PluginManager getManager() {
		if (singleton == null) {
			ObjectFactory objectFactory = ObjectFactory.newInstance();

			singleton = objectFactory.createManager();
			try {
				DefaultPluginsCollector collector = new DefaultPluginsCollector();
				ExtendedProperties ep = new ExtendedProperties();
				ep.setProperty("org.java.plugin.boot.pluginsRepositories",
						"./src/plugins,./plugins");
				collector.configure(ep);

				Collection<PluginLocation> plugins = collector
						.collectPluginLocations();

				/**
				 * I know the following is really, really ugly, but I have found
				 * no way to automatically discover multiple plugin.xmls in JARs
				 */
				PluginLocation location;

				location = getLocationInsideJar("/plugins/net.sf.jabref.core/",
						"plugin.xml");
				if (location != null)
					plugins.add(location);

				location = getLocationInsideJar(
						"/plugins/net.sf.jabref.export.misq/", "plugin.xml");
				if (location != null)
					plugins.add(location);

				if (plugins.size() <= 0) {
					net.sf.jabref.Globals
							.logger("No plugins found. At least net.sf.jabref.core should be there.");
				} else {
					net.sf.jabref.Globals.logger(plugins.size()
							+ " plugin(s) found.");
				}

				singleton.publishPlugins(plugins
						.toArray(new PluginLocation[] {}));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return singleton;
	}
}
