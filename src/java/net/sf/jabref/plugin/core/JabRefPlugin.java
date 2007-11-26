package net.sf.jabref.plugin.core;

import net.sf.jabref.plugin.core.generated._JabRefPlugin;

import org.java.plugin.PluginLifecycleException;
import org.java.plugin.PluginManager;

/**
 * Plug-in class for plug-in net.sf.jabref.core.
 * 
 * Feel free to modify this file, since only the class _JabRefPlugin 
 * (in the subpackage net.sf.jabref.plugin.core.generated)
 * will be overwritten, when you re-run the code generator.
 */
public class JabRefPlugin extends _JabRefPlugin {

    public void doStart(){
        // TODO: Will be called when plug-in is started.
    }

    public void doStop(){
        // TODO: Will be called when plug-in is stopped.
    }

    /**
	 * Retrieve the Plug-in instance from the given manager.
	 * 
	 * @param manager
	 *            The manager from which to retrieve the plug-in instance
	 * 
	 * @return The requested plug-in or null if not found.
	 */
	public static JabRefPlugin getInstance(PluginManager manager) {
		try {
            return (JabRefPlugin) manager
					.getPlugin(JabRefPlugin.getId());
		} catch (PluginLifecycleException e) {
			return null;
		} catch (IllegalArgumentException e) {
		    return null;
		}
	}
}