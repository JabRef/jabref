Plugins for JabRef that are supposed to be distributed with the system should go here.

At the moment we need an ugly hack in 

net.sf.jabref.plugin.PluginCore.java

to add all plugins manually. Since they are loaded from the resulting JabRef jar.

To work around this issue, one can add all extension directly to the core plugin instead. 