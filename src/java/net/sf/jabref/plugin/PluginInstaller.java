package net.sf.jabref.plugin;

import net.sf.jabref.plugin.*;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.Globals;

import javax.swing.*;
import java.net.URL;
import java.io.File;
import java.io.IOException;
import java.io.FilenameFilter;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Collections;
import org.java.plugin.registry.PluginDescriptor;

/**
 *
 */
public class PluginInstaller {

    public static final int
        SUCCESS = 0,
        UNABLE_TO_CREATE_DIR = 1,
        UNABLE_TO_COPY_FILE = 2;

    public static final int
        NO_VERSIONS_INSTALLED = 0,
        NEWER_VERSION_INSTALLED = 1,
        SAME_VERSION_INSTALLED = 2,
        OLDER_VERSION_INSTALLED = 3,
        UNCONVENTIONAL_FILENAME = 4,
        UNKNOWN_VERSION = 5;

    public static final int
        NOT_LOADED = 0,
        LOADED = 1;
    
    public static void installPlugin(JabRefFrame frame, URL source) {
        String fileName = (new File(source.getFile())).getName();
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                JOptionPane.showMessageDialog(frame, Globals.lang("Unable to create plugin directory")
                    +" ("+PluginCore.userPluginDir.getPath()+").", Globals.lang("Plugin installer"),
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        int status = checkInstalledVersion(fileName);
        int result;
        switch (status) {
            case NO_VERSIONS_INSTALLED:
                result = copyPlugin(frame, source, fileName);
                if (result == SUCCESS)
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installed successfully. You must restart JabRef to load the new plugin."),
                            Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                else {
                    String reason;
                    if (result == UNABLE_TO_COPY_FILE)
                        reason = Globals.lang("Unable to copy file");
                    else
                        reason = Globals.lang("Unable to create user plugin directory")
                            +" ("+PluginCore.userPluginDir.getPath()+").";
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.")+" "+reason,
                            Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                }
                break;
            case SAME_VERSION_INSTALLED:
                JOptionPane.showMessageDialog(frame, Globals.lang("The same version of this plugin is already installed."),
                        Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                break;
            case NEWER_VERSION_INSTALLED:
                JOptionPane.showMessageDialog(frame, Globals.lang("A newer version of this plugin is already installed."),
                        Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                break;
            case OLDER_VERSION_INSTALLED:
                result = copyPlugin(frame, source, fileName);
                if (result == SUCCESS) {
                    int answer = JOptionPane.showConfirmDialog(frame,
                            Globals.lang("One or more older versions of this plugin is installed. Delete old versions?"),
                            Globals.lang("Plugin installer"), JOptionPane.YES_NO_OPTION);
                    if (answer == JOptionPane.YES_OPTION) {
                        boolean success = deleteOlderVersions(fileName);
                        if (success) {
                            JOptionPane.showMessageDialog(frame,
                                    Globals.lang("Old versions deleted successfully."),
                                    Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(frame,
                                    Globals.lang("Deletion of old versions failed."),
                                    Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                else {
                    String reason;
                    if (result == UNABLE_TO_COPY_FILE)
                        reason = Globals.lang("Unable to copy file");
                    else
                        reason = Globals.lang("Unable to create user plugin directory")
                            +" ("+PluginCore.userPluginDir.getPath()+").";
                    JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.")+" "+reason,
                            Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                }
                break;
            //case UNKNOWN_VERSION:
            //    JOptionPane.showMessageDialog(frame, Globals.lang("Could not determine version of "));
            //    break;
            case UNKNOWN_VERSION:
                JLabel lab = new JLabel("<html>"+Globals.lang("Unable to determine plugin name and "
                        +"version from filename."
                        +" File name convention is '[plugin name]-[version].jar'.")
                        +"<br>"+Globals.lang("Install anyway?")+"</html>");
                int answer = JOptionPane.showConfirmDialog(frame, lab,
                        Globals.lang("Plugin installer"), JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.YES_OPTION) {
                    result = copyPlugin(frame, source, fileName);
                    if (result == SUCCESS)
                        JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installed successfully. You must restart JabRef to load the new plugin."),
                                Globals.lang("Plugin installer"), JOptionPane.INFORMATION_MESSAGE);
                    else {
                        String reason;
                        if (result == UNABLE_TO_COPY_FILE)
                            reason = Globals.lang("Unable to copy file");
                        else
                            reason = Globals.lang("Unable to create user plugin directory")
                                +" ("+PluginCore.userPluginDir.getPath()+").";
                        JOptionPane.showMessageDialog(frame, Globals.lang("Plugin installation failed.")+" "+reason,
                                Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
                    }

                }
                break;
        }
    }

    /**
     * Check the status of the named plugin - whether an older, the same or a
     * newer version is already installed.
     * @param filename The filename of the plugin.
     * @return an integer indicating the status
     */
    public static int checkInstalledVersion(String filename) {
        String[] nav = getNameAndVersion(filename);
        if (nav == null)
            return UNKNOWN_VERSION;
        
        VersionNumber vn = new VersionNumber(nav[1]);
        List<VersionNumber> versions = getInstalledVersions(nav[0]);
        
        boolean hasSame = versions.size() > 0 && (vn.compareTo(versions.get(0)) == 0);
        boolean hasNewer = versions.size() > 0 && (vn.compareTo(versions.get(0)) > 0);

        if (versions.size() == 0) {
            return NO_VERSIONS_INSTALLED;
        }
        if (hasNewer)
            return NEWER_VERSION_INSTALLED;
        if (hasSame)
            return SAME_VERSION_INSTALLED;

        return OLDER_VERSION_INSTALLED;
    }

    /**
     * Delete the given plugin.
     * @param plugin Name and version information for the plugin to delete.
     * @return true if deletion is successful, false otherwise.
     */
    public static boolean deletePlugin(NameAndVersion plugin) {
        String file = buildFileName(plugin.name, 
                plugin.version.equals(VersionNumber.ZERO) ? null : plugin.version.toString());
        return (new File(file)).delete();
    }
    
    public static boolean deleteOlderVersions(String filename) {
        String[] nav = getNameAndVersion(filename);
        if (nav == null)
            return false;
        boolean success = true;
        VersionNumber num = new VersionNumber(nav[1]);
        List<VersionNumber> versions = getInstalledVersions(nav[0]);
        for (Iterator<VersionNumber> iterator = versions.iterator(); iterator.hasNext();) {
            VersionNumber versionNumber = iterator.next();
            if (num.compareTo(versionNumber) < 0) {
                String vnString = versionNumber.equals(VersionNumber.ZERO) ? null : versionNumber.toString();
                String file = buildFileName(nav[0], vnString);
                success = (new File(file)).delete() && success;
            }
        }
        return success;
    }

    /**
     * Copy a plugin to the user plugin directory. Does not check whether the plugin
     * already exists.
     * @param source The local or remote location to copy the plugin from.
     * @return true if the install was successful
     */
    public static int copyPlugin(JFrame frame, URL source, String destFileName) {
        if (destFileName == null)
            destFileName = source.getFile();
        if (!PluginCore.userPluginDir.exists()) {
            boolean created = PluginCore.userPluginDir.mkdirs();
            if (!created) {
                return UNABLE_TO_CREATE_DIR;
            }
        }
        File destFile = new File(PluginCore.userPluginDir, destFileName);
        URLDownload ud = new URLDownload(frame, source, destFile);

        try {
            ud.download();
            return SUCCESS;
        } catch (IOException e) {
            e.printStackTrace();
            return UNABLE_TO_COPY_FILE;
        }
    }



    /**
     * Based on a plugin name, find all versions that are already present
     * in the user plugin directory.
     * @param pluginName The name of the plugin.
     * @return A list of versions already present.
     */
    public static List<VersionNumber> getInstalledVersions(final String pluginName) {

        String[] files = PluginCore.userPluginDir.list(new FilenameFilter() {
            public boolean accept(File file, String s) {
                return s.startsWith(pluginName) && s.endsWith(".jar");
            }
        });
        List<VersionNumber> versions = new ArrayList<VersionNumber>();
        for (int i = 0; i < files.length; i++) {
            String file = files[i];
            String[] nav = getNameAndVersion(file);
            if (nav != null) {
                VersionNumber vn = new VersionNumber(nav[1]);
                versions.add(vn);
            }
                
        }
        Collections.sort(versions);
        return versions;
    }


    static Pattern pluginFilePattern = Pattern.compile("(.*)-([\\d\\.]+).jar");
    static Pattern pluginFilePatternNoVersion = Pattern.compile("(.*).jar");

    /**
     * Try to split up a plugin file name in order to find the plugin name and
     * the version number. The file name is expected to be on the format
     * [plugin name]-[version number].jar
     *
     * @param filename The plugin file name.
     * @return A string array containing the plugin name in the first element and
     *   the version number in the second, or null if the filename couldn't be
     *   interpreted.
     *
     */
    public static String[] getNameAndVersion(String filename) {
        Matcher m = pluginFilePattern.matcher(filename);
        if (m.matches()) {
            return new String[] {m.group(1), m.group(2)};
        }
        m = pluginFilePatternNoVersion.matcher(filename);
        if (m.matches()) {
            return new String[] {m.group(1), "0"}; // unknown version is set to 0
        }
        else
            return null;
    }

    /**
     * Make a File pointing to a file with the correct name in the user
     * plugin directory.
     * @param name The plugin name.
     * @param version The plugin version.
     * @return the correct File.
     */
    public static String buildFileName(String name, String version) {
        if (version != null)
            return PluginCore.userPluginDir+"/"+name+"-"+version+".jar";
        else
            return PluginCore.userPluginDir+"/"+name+".jar";
    }
    
    /**
     * Build a list of installed plugins.
     * @return a list of plugin names and version numbers.
     */
    public static EventList<NameAndVersion> findInstalledPlugins() {
	EventList<NameAndVersion> plugins = new BasicEventList<NameAndVersion>();
	if (!PluginCore.userPluginDir.exists())
		return plugins;
        String[] files = PluginCore.userPluginDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        
        List<String> urls = new ArrayList<String>();
        Collection<PluginDescriptor> descriptors =
                PluginCore.getManager().getRegistry().getPluginDescriptors();
        for (PluginDescriptor desc : descriptors) {
            urls.add(desc.getLocation().getFile());
        }
        
        for (int i=0; i<files.length; i++) {
            String[] nav = getNameAndVersion(files[i]);
            if (nav != null) {
                VersionNumber vn = nav[1] != null ? new VersionNumber(nav[1]) : null;
                NameAndVersion nameAndVersion = new NameAndVersion(nav[0], vn);
                for (String loc : urls) {
                    if (loc.indexOf(nav[0]) >= 0)
                        nameAndVersion.setStatus(LOADED);
                }
                plugins.add(nameAndVersion);
            }
        }
        return plugins;
    }
    
  
    public static class NameAndVersion implements Comparable {
        String name;
        VersionNumber version;
        int status = 0;
        
        public NameAndVersion(String name, VersionNumber version) {
            this.name = name;
            this.version = version;
        }
        
        public int compareTo(Object o) {
            NameAndVersion oth = (NameAndVersion)o;
            if (!name.equals(oth.name))
                return name.compareTo(oth.name);
            else {
                if (version == null)
                    return 1;
                else if (oth.version == null)
                    return -1;
                else
                    return version.compareTo(oth.version);
            }
        }
        
        public int getStatus() {
            return status;
        }
        
        public void setStatus(int status) {
            this.status = status;
        }
    }
            
    static class VersionNumber implements Comparable {
        public static final VersionNumber ZERO = new VersionNumber("0");
        List<Integer> digits;
        public VersionNumber(String number) {
            digits = new ArrayList<Integer>();
            String[] elms = number.split("\\.");
            for (int i = 0; i < elms.length; i++) {
                try {
                    int num = Integer.parseInt(elms[i]);
                    digits.add(num);
                } catch (NumberFormatException ex) {
                    // Do nothing
                }

            }
        }

        public int compareTo(Object o) {
            VersionNumber oth = (VersionNumber)o;
           
            for (int i=0; i<Math.min(digits.size(), oth.digits.size()); i++) {
                if (digits.get(i) != oth.digits.get(i))
                    return oth.digits.get(i)-digits.get(i);
            }
            // All digits equal so far, and only one of the numbers has more digits.
            // The one with digits remaining is the newest one:
            return oth.digits.size()-digits.size();
        }
        
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (Iterator<Integer> integerIterator = digits.iterator(); integerIterator.hasNext();) {
                sb.append(integerIterator.next());
                if (integerIterator.hasNext())
                    sb.append(".");
            }
            return sb.toString();
        }
        
        public boolean equals(Object o) {
            return compareTo(o) == 0;
        }
    }
}
